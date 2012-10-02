package cz.cesnet.shongo.jade;

import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;
import cz.cesnet.shongo.jade.command.Command;
import cz.cesnet.shongo.jade.ontology.CommandError;
import cz.cesnet.shongo.jade.ontology.CommandNotSupported;
import jade.content.AgentAction;
import jade.content.Concept;
import jade.content.ContentElement;
import jade.content.ContentManager;
import jade.content.lang.Codec;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.content.onto.basic.Done;
import jade.content.onto.basic.Result;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Behaviour listening for incoming messages.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class ReceiverBehaviour extends CyclicBehaviour
{
    private static Logger logger = LoggerFactory.getLogger(ReceiverBehaviour.class);

    protected cz.cesnet.shongo.jade.Agent myShongoAgent;

    @Override
    public void action()
    {
        ACLMessage msg = myAgent.receive();
        if (msg == null) {
            block();
            return;
        }

        if (msg.getSender().getLocalName().equals(FIPANames.DEFAULT_DF)) {
            myAgent.putBack(msg);
            return;
        }

        logger.info("{} received from {}: {}\n\n",
                new Object[]{myAgent.getAID().getName(), msg.getSender().getName(), msg.toString()});

        boolean result = onReceiveMessage(msg);

        String commandIdentifier = msg.getConversationId();
        Command command = myShongoAgent.getCommand(commandIdentifier);
        if ( command != null ) {
            if ( result ) {
                command.setState(Command.State.SUCCESSFUL);
            } else {
                command.setState(Command.State.FAILED, msg.getContent());
            }
        }
    }

    private boolean onReceiveMessage(ACLMessage msg)
    {
        // FIXME: refactor; think over what messages may be received and process them systematically

        // Command identifier is store in the conversation id

        if (msg.getPerformative() == ACLMessage.NOT_UNDERSTOOD) {
            logger.error("The recipient did not understand the message:\n" + msg);

            return false;
        }
        if (msg.getPerformative() == ACLMessage.FAILURE) {
            logger.error("Execution of the command failed:\n" + msg);
            // TODO: process the error; it might contain Result with a CommandError as the value (for an example of an error, try dialing a number for several times, initiating multiple calls at a time)
            return false;
        }

        // prepare a reply to the message received
        ACLMessage reply = msg.createReply();
        reply.setPerformative(ACLMessage.INFORM);

        ContentManager cm = myAgent.getContentManager();
        try {
            ContentElement contentElement = cm.extractContent(msg);
            // Action content
            if (contentElement instanceof Action) {
                Action act = (Action) contentElement;
                Concept action = act.getAction();
                if (action instanceof AgentAction) {
                    try {
                        Object actionRetVal = myShongoAgent.handleAgentAction((AgentAction) action, msg.getSender());
                        if (msg.getPerformative() == ACLMessage.INFORM) {
                            return true; // do not reply to inform messages
                        }
                        // respond to the caller - either with the command return value or saying it was OK
                        ContentElement response = (actionRetVal == null ? new Done(act) : new Result(act, actionRetVal));
                        cm.fillContent(reply, response);
                    }
                    catch (CommandException e) {
                        logger.info("Failure executing a command", e);
                        reply.setPerformative(ACLMessage.FAILURE);
                        String message = e.getMessage();
                        if (e.getCause() != null) {
                            message += " (" + e.getCause().getMessage() + ")";
                        }
                        cm.fillContent(reply, new Result(act, new CommandError(message)));
                    }
                    catch (CommandUnsupportedException e) {
                        logger.info("Unsupported command", e);
                        reply.setPerformative(ACLMessage.FAILURE);
                        String message = e.getMessage();
                        if (e.getCause() != null) {
                            message += " (" + e.getCause().getMessage() + ")";
                        }
                        cm.fillContent(reply, new Result(act, new CommandNotSupported(message)));
                    }
                }
                else {
                    // other actions than AgentAction are not recognized
                    reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
                }
            }
            // Result content
            else if (contentElement instanceof Result) {
                Result result = (Result) contentElement;

                // log some meaningful message
                String logMsg = String.format("Received a result of type %s, value %s.", result.getValue().getClass(), result.getValue());
                if (result.getAction() instanceof Action) {
                    Action action = (Action) result.getAction();
                    logMsg += " It is the result of action: " + action.getAction();
                }
                logger.info(logMsg);

                String commandIdentifier = msg.getConversationId();
                Command command = myShongoAgent.getCommand(commandIdentifier);
                if ( command != null ) {
                    command.setResult(result.getValue());
                }

                return true; // do not reply to results of actions
            }
            else if (contentElement instanceof Done) {
                Done done = (Done) contentElement;

                // log some meaningful message
                String logMsg = "Received confirmation of successful execution of an action.";
                if (done.getAction() instanceof Action) {
                    Action action = (Action) done.getAction();
                    logMsg += " It is the result of action: " + action.getAction();
                }
                logger.info(logMsg);

                return true; // do not reply to results of actions
            }
            else {
                reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
            }
        }
        catch (Codec.CodecException e) {
            logger.info("Error in decoding a message", e);
            reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
        }
        catch (OntologyException e) {
            logger.error("Error in decoding a message", e);
            reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
        }
        catch (UnknownActionException e) {
            logger.info("Unknown action requested by " + msg.getSender().getName(), e);
            reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
        }

        logger.info(String.format("%s sending reply: %s", myAgent.getName(), reply));
        myAgent.send(reply);
        return reply.getPerformative() != ACLMessage.NOT_UNDERSTOOD;
    }

    @Override
    public void setAgent(Agent a)
    {
        if (!(a instanceof cz.cesnet.shongo.jade.Agent)) {
            throw new IllegalArgumentException("This behaviour works only with instances of " + cz.cesnet.shongo.jade.Agent.class);
        }
        myShongoAgent = (cz.cesnet.shongo.jade.Agent) a;
        super.setAgent(a);
    }
}
