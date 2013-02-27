package cz.cesnet.shongo.jade;

import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;
import cz.cesnet.shongo.fault.jade.CommandError;
import cz.cesnet.shongo.fault.jade.CommandNotSupported;
import cz.cesnet.shongo.fault.jade.CommandUnknownFailure;
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

    /**
     * {@link cz.cesnet.shongo.jade.Agent} which is used for handling and replying to received {@link AgentAction}s.
     */
    protected cz.cesnet.shongo.jade.Agent agent;

    /**
     * Template defining which messages are received by the {@link AgentActionResponderBehaviour}.
     * We want receive only {@link #FIPA_REQUEST}s and only {@link ACLMessage#REQUEST} performative.
     */
    private static MessageTemplate MESSAGE_TEMPLATE = MessageTemplate.and(
            MessageTemplate.MatchProtocol(FIPA_REQUEST),
            MessageTemplate.MatchPerformative(ACLMessage.REQUEST));

    /**
     * Constructor.
     *
     * @param agent
     */
    public AgentActionResponderBehaviour(jade.core.Agent agent)
    {
        super(agent, MESSAGE_TEMPLATE);
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
            throw new IllegalArgumentException("This behaviour works only with instances of "
                    + cz.cesnet.shongo.jade.Agent.class);
        }
        this.agent = (cz.cesnet.shongo.jade.Agent) agent;
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
                Object actionRetVal = agent.handleAgentAction(action, request.getSender());
                // respond to the caller - either with the command return value or saying it was OK
                ContentElement response = (actionRetVal == null ? new Done(act) : new Result(act, actionRetVal));
                fillMessage(reply, ACLMessage.INFORM, response);
            }
            catch (UnknownAgentActionException exception) {
                logger.error(String.format("Unknown action '%s' requested by '%s'.",
                        exception.getAgentAction(), request.getSender().getName()), exception);
                reply.setPerformative(ACLMessage.REFUSE);
            }
            catch (CommandUnsupportedException exception) {
                logger.error(String.format("Unsupported command requested by '%s'.",
                        request.getSender().getName()), exception);
                ContentElement response = new Result(act, new CommandNotSupported());
                fillMessage(reply, ACLMessage.FAILURE, response);
            }
            catch (CommandException exception) {
                logger.error(String.format("Command requested by '%s' has failed.",
                        request.getSender().getName()), exception);
                ContentElement response = new Result(act, new CommandError(exception.getMessage()));
                fillMessage(reply, ACLMessage.FAILURE, response);
            }
            catch (Exception exception) {
                logger.error(String.format("Command requested by '%s' has failed with unknown reason.",
                        request.getSender().getName()), exception);
                String message = exception.getMessage();
                if (exception.getCause() != null) {
                    message += " (" + exception.getCause().getMessage() + ")";
                }
                ContentElement response = new Result(act, new CommandUnknownFailure(message));
                fillMessage(reply, ACLMessage.FAILURE, response);
            }
        }
        catch (Codec.CodecException exception) {
            logger.error(String.format("Received a request which the agent did not understand (wrong codec): %s",
                    request), exception);
            reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
        }
        catch (OntologyException exception) {
            logger.error(String.format("Received a request which the agent did not understand (wrong ontology): %s",
                    request), exception);
            reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
        }
        catch (ClassCastException exception) {
            logger.error(String.format("Received a request which the agent did not understand (wrong content type): %s",
                    request), exception);
            reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
        }
        catch (Exception exception) {
            logger.error(String.format("Received a request which the agent did not understand: %s", request),
                    exception);
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