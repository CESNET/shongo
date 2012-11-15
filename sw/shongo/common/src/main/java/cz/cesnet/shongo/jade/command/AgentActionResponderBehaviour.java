package cz.cesnet.shongo.jade.command;

import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;
import cz.cesnet.shongo.jade.UnknownAgentActionException;
import jade.content.AgentAction;
import jade.content.ContentElement;
import jade.content.ContentManager;
import jade.content.lang.Codec;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.content.onto.basic.Done;
import jade.content.onto.basic.Result;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.SimpleAchieveREResponder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Behaviour that responds to {@link AgentAction} requests sent by {@link AgentActionRequesterBehaviour} and performs
 * requested {@link AgentAction}.
 * <p/>
 * Implements the responder part of the standard FIPA-Request protocol (see the Jade Programmer's Guide or the Ontology
 * example found in the Jade distribution in examples/src/examples/ontology).
 * <p/>
 * See {@link AgentActionRequesterBehaviour} class for the other party of the conversation.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class AgentActionResponderBehaviour extends SimpleAchieveREResponder
{
    private Logger logger = LoggerFactory.getLogger(AgentActionResponderBehaviour.class);

    protected cz.cesnet.shongo.jade.Agent myShongoAgent;

    /**
     * Constructor.
     *
     * @param agent
     */
    public AgentActionResponderBehaviour(jade.core.Agent agent)
    {
        super(agent, MessageTemplate.MatchProtocol(FIPA_REQUEST));
    }

    /**
     * Associates this behaviour with a Shongo agent.
     *
     * @param agent a Shongo agent (must be an instance of cz.cesnet.shongo.jade.Agent)
     */
    @Override
    public void setAgent(jade.core.Agent agent)
    {
        if (!(agent instanceof cz.cesnet.shongo.jade.Agent)) {
            throw new IllegalArgumentException(
                    "This behaviour works only with instances of " + cz.cesnet.shongo.jade.Agent.class);
        }

        myShongoAgent = (cz.cesnet.shongo.jade.Agent) agent;
        super.setAgent(agent);
    }

    /**
     * Processes a request message, e.g. an agent action for performing a command.
     *
     * @param request request message
     * @return message to respond with
     * @throws NotUnderstoodException
     * @throws RefuseException
     */
    @Override
    protected ACLMessage prepareResponse(ACLMessage request) throws NotUnderstoodException, RefuseException
    {
        logger.debug("Received message: {}", request);

        ACLMessage reply = request.createReply();

        ContentManager cm = myAgent.getContentManager();
        try {
            Action act = (Action) cm.extractContent(request);
            AgentAction action = (AgentAction) act.getAction();
            try {
                Object actionRetVal = myShongoAgent.handleAgentAction(action, request.getSender());
                // respond to the caller - either with the command return value or saying it was OK
                ContentElement response = (actionRetVal == null ? new Done(act) : new Result(act, actionRetVal));
                fillMessage(reply, ACLMessage.INFORM, response);
            }
            catch (CommandException e) {
                logger.info("Failure executing a command requested by " + request.getSender().getName(), e);
                String message = e.getMessage();
                if (e.getCause() != null) {
                    message += " (" + e.getCause().getMessage() + ")";
                }
                ContentElement response = new Result(act, new CommandError(message));
                fillMessage(reply, ACLMessage.FAILURE, response);
            }
            catch (ClassCastException e) {
                logger.info("Failure executing a command requested by " + request.getSender().getName(), e);
                String message = e.getMessage();
                if (e.getCause() != null) {
                    message += " (" + e.getCause().getMessage() + ")";
                }
                ContentElement response = new Result(act, new CommandError(message));
                fillMessage(reply, ACLMessage.FAILURE, response);
            }
            catch (CommandUnsupportedException e) {
                logger.info("Unsupported command requested by " + request.getSender().getName(), e);
                ContentElement response = new Result(act, new CommandNotSupported(e.getMessage()));
                fillMessage(reply, ACLMessage.FAILURE, response);
            }
            catch (UnknownAgentActionException e) {
                logger.info("Unknown action requested by " + request.getSender().getName(), e);
                reply.setPerformative(ACLMessage.REFUSE);
            }
        }
        catch (Codec.CodecException e) {
            logger.info(String.format("Received a request which the agent did not understand (wrong codec):%s",
                    request), e);
            reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
        }
        catch (OntologyException e) {
            logger.info(String.format("Received a request which the agent did not understand (wrong ontology): %s",
                    request), e);
            reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
        }
        catch (ClassCastException e) {
            logger.info(String.format("Received a request which the agent did not understand (wrong content type): %s",
                    request), e);
            reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
        }

        logger.debug("Sending reply: {}", reply);

        return reply;
    }

    /**
     * Fill message content.
     *
     * @param message
     * @param performative
     * @param content
     */
    private void fillMessage(ACLMessage message, int performative, ContentElement content)
    {
        message.setPerformative(performative);

        try {
            myAgent.getContentManager().fillContent(message, content);
        }
        catch (Codec.CodecException e) {
            logger.error("Error composing a reply to message", e);
        }
        catch (OntologyException e) {
            logger.error("Error composing a reply to message", e);
        }
    }
}
