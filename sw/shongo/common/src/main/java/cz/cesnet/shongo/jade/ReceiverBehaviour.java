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


    /**
     * Associates this behaviour with a Shongo agent.
     *
     * @param agent a Shongo agent (must be an instance of cz.cesnet.shongo.jade.Agent)
     */
    @Override
    public void setAgent(Agent agent)
    {
        if (!(agent instanceof cz.cesnet.shongo.jade.Agent)) {
            throw new IllegalArgumentException("This behaviour works only with instances of " + cz.cesnet.shongo.jade.Agent.class);
        }

        myShongoAgent = (cz.cesnet.shongo.jade.Agent) agent;
        super.setAgent(agent);
    }


    @Override
    public void action()
    {
        ACLMessage msg = myAgent.receive();
        if (msg == null) {
            block();
            return;
        }

        // messages from the directory facilitator are not for us
        if (msg.getSender().getLocalName().equals(FIPANames.DEFAULT_DF)) {
            myAgent.putBack(msg);
            return;
        }

        logger.info("{} received from {}: {}\n\n",
                new Object[]{myAgent.getAID().getName(), msg.getSender().getName(), msg.toString()});

        processMessage(msg);
    }


    /**
     * Process the received message.
     *
     * Actions taken depend on the message performative. There are several kinds of messages:
     * - REQUEST: a request to perform an action
     * - INFORM: a result of successfully processed action previously requested by this agent
     * - FAILURE: there was a failure while processing a request previously sent by this agent
     * - NOT_UNDERSTOOD: the sending party did not understand a message previously sent by this agent
     *
     * @param msg    message to process
     */
    private void processMessage(ACLMessage msg)
    {
        boolean understood;
        try {
            switch (msg.getPerformative()) {
                case ACLMessage.REQUEST:
                    understood = processRequestMessage(msg);
                    break;
                case ACLMessage.INFORM:
                    understood = processInformMessage(msg);
                    break;
                case ACLMessage.FAILURE:
                    understood = processFailureMessage(msg);
                    break;
                case ACLMessage.NOT_UNDERSTOOD:
                    understood = processNotUnderstoodMessage(msg);
                    break;
                default:
                    logger.error("Unrecognized performative of message: {}", msg);
                    understood = false;
            }
        }
        catch (Codec.CodecException e) {
            logger.info("Error in decoding a message", e);
            understood = false;
        }
        catch (OntologyException e) {
            logger.error("Error in decoding a message", e);
            understood = false;
        }

        if (!understood) {
            // common behaviour when the message content was not understood
            reply(msg, ACLMessage.NOT_UNDERSTOOD);
            myShongoAgent.endConversation(msg.getConversationId(), Command.State.FAILED);
        }
    }

    /**
     * Processes a request message, e.g. an agent action for performing a command.
     *
     * @param msg    request message
     * @return true if the request was understood, false if not
     * @throws Codec.CodecException when it was impossible to decode the message content
     * @throws OntologyException when it was impossible to understand the message content
     */
    private boolean processRequestMessage(ACLMessage msg) throws OntologyException, Codec.CodecException
    {
        ContentManager cm = myAgent.getContentManager();
        ContentElement contentElement = cm.extractContent(msg);

        if (contentElement instanceof Action) {
            Action act = (Action) contentElement;
            Concept action = act.getAction();

            if (action instanceof AgentAction) {
                try {
                    Object actionRetVal = myShongoAgent.handleAgentAction((AgentAction) action, msg.getSender());
                    // respond to the caller - either with the command return value or saying it was OK
                    ContentElement response = (actionRetVal == null ? new Done(act) : new Result(act, actionRetVal));
                    reply(msg, ACLMessage.INFORM, response);
                    myShongoAgent.endConversation(msg.getConversationId(), Command.State.SUCCESSFUL);
                    return true;
                }
                catch (CommandException e) {
                    logger.info("Failure executing a command", e);
                    String message = e.getMessage();
                    if (e.getCause() != null) {
                        message += " (" + e.getCause().getMessage() + ")";
                    }
                    ContentElement response = new Result(act, new CommandError(message));
                    reply(msg, ACLMessage.FAILURE, response);
                    myShongoAgent.endConversation(msg.getConversationId(), Command.State.FAILED);
                    return true;
                }
                catch (CommandUnsupportedException e) {
                    logger.info("Unsupported command", e);
                    ContentElement response = new Result(act, new CommandNotSupported(e.getMessage()));
                    reply(msg, ACLMessage.FAILURE, response);
                    myShongoAgent.endConversation(msg.getConversationId(), Command.State.FAILED);
                    return true;
                }
                catch (UnknownActionException e) {
                    logger.info("Unknown action requested by " + msg.getSender().getName(), e);
                }
            }
        }

        return false;
    }

    /**
     * Processes an inform message, e.g. result of a command.
     *
     * @param msg    informative message
     * @return true if the information was understood, false if not
     * @throws Codec.CodecException when it was impossible to decode the message content
     * @throws OntologyException when it was impossible to understand the message content
     */
    private boolean processInformMessage(ACLMessage msg) throws OntologyException, Codec.CodecException
    {
        ContentManager cm = myAgent.getContentManager();
        ContentElement contentElement = cm.extractContent(msg);

        if (contentElement instanceof Result) {
            // return value of a command
            Result result = (Result) contentElement;

            // log some meaningful message
            String logMsg = String.format("Received a result of type %s, value %s.", result.getValue().getClass(),
                    result.getValue());
            if (result.getAction() instanceof Action) {
                Action action = (Action) result.getAction();
                logMsg += " It is the result of action: " + action.getAction();
            }
            logger.info(logMsg);

            Command cmd = myShongoAgent.getConversationCommand(msg.getConversationId());
            if (cmd != null) {
                cmd.setResult(result.getValue());
            }

            myShongoAgent.endConversation(msg.getConversationId(), Command.State.SUCCESSFUL);
            return true;
        }
        else if (contentElement instanceof Done) {
            // notification that a command succeeded
            Done done = (Done) contentElement;

            // log some meaningful message
            String logMsg = "Received confirmation of successful execution of an action.";
            if (done.getAction() instanceof Action) {
                Action action = (Action) done.getAction();
                logMsg += " It is the result of action: " + action.getAction();
            }
            logger.info(logMsg);

            myShongoAgent.endConversation(msg.getConversationId(), Command.State.SUCCESSFUL);
            return true;
        }

        return false;
    }

    /**
     * Processes a failure message, e.g. a message holding an error of a command.
     *
     * @param msg    failure message
     * @return true if the failure message was understood, false if not
     */
    private boolean processFailureMessage(ACLMessage msg)
    {
        logger.error("Execution of the command failed: " + msg);
        // TODO: process the error; it might contain Result with a CommandError as the value (for an example of an error, try dialing a number for several times from an endpoint, initiating multiple calls at a time)
        myShongoAgent.endConversation(msg.getConversationId(), Command.State.FAILED, msg.getContent());
        return true;
    }

    /**
     * Processes a message saying that something was not understood.
     *
     * @param msg    the message saying that something was not understood.
     * @return true if the message was understood, false if not
     */
    private boolean processNotUnderstoodMessage(ACLMessage msg)
    {
        logger.error("The recipient did not understand the message: " + msg);
        myShongoAgent.endConversation(msg.getConversationId(), Command.State.FAILED, msg.getContent());
        return true;
    }


    /**
     * Sends a reply to a given message.
     *
     * @param msg             message to send a reply to
     * @param performative    performative of the reply
     */
    private void reply(ACLMessage msg, int performative)
    {
        reply(msg, performative, null);
    }


    /**
     * Sends a reply to a given message.
     *
     * Might not succeed if there is some content to send with the reply.
     *
     * @param msg             message to send a reply to
     * @param performative    performative of the reply
     * @param content         content of the reply
     */
    private void reply(ACLMessage msg, int performative, ContentElement content)
    {
        try {
            ACLMessage re = msg.createReply();
            re.setPerformative(performative);
            if (content != null) {
                myAgent.getContentManager().fillContent(re, content);
            }
            logger.info("{} sending reply: {}", myAgent.getName(), re);
            myAgent.send(re);
        }
        catch (Codec.CodecException e) {
            logger.error("Error composing a reply to message", e);
        }
        catch (OntologyException e) {
            logger.error("Error composing a reply to message", e);
        }
    }
}
