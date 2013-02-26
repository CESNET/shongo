package cz.cesnet.shongo.jade.command;

import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.jade.AgentAction;
import cz.cesnet.shongo.fault.jade.*;
import jade.content.ContentElement;
import jade.content.ContentManager;
import jade.content.lang.Codec;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.content.onto.basic.Done;
import jade.content.onto.basic.Result;
import jade.core.AID;
import jade.core.Agent;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.proto.SimpleAchieveREInitiator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Behaviour that requests an {@link cz.cesnet.shongo.jade.Agent} to perform an {@link cz.cesnet.shongo.api.jade.AgentAction}.
 * <p/>
 * Implements the initiator part of the standard FIPA-Request protocol (see the Jade Programmer's Guide or the Ontology
 * example found in the Jade distribution in examples/src/examples/ontology).
 * <p/>
 * See {@link AgentActionResponderBehaviour} class for the other party of the conversation.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class AgentActionRequesterBehaviour extends SimpleAchieveREInitiator
{
    private static Logger logger = LoggerFactory.getLogger(AgentActionRequesterBehaviour.class);

    /**
     * Command that invoked this behaviour (and someone is possibly waiting for it).
     */
    private AgentActionCommand command;

    /**
     * Constructor.
     *
     * @param agent   representing requester who should send the request message
     * @param command agent action command which should be sent as the request message
     */
    public AgentActionRequesterBehaviour(jade.core.Agent agent, AgentActionCommand command) throws Exception
    {
        super(agent, createMessage(agent, command.getAgentReceiverId(), command.getAgentAction()));

        this.command = command;
    }

    /**
     * Create {@link ACLMessage} which will be sent.
     *
     * @param agentRequester sender agent
     * @param agentReceiverId receiver agent id
     * @param agentAction agent action which should be sent
     * @return the constructed message
     * @throws Exception when the message construction fails
     */
    private static ACLMessage createMessage(Agent agentRequester, AID agentReceiverId,
            AgentAction agentAction) throws Exception
    {
        ACLMessage message = new ACLMessage(ACLMessage.REQUEST);
        message.addReceiver(agentReceiverId);
        message.setSender(agentRequester.getAID());
        message.setLanguage(FIPANames.ContentLanguage.FIPA_SL);
        message.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
        message.setOntology(agentAction.getOntology().getName());

        ContentElement content = new jade.content.onto.basic.Action(agentRequester.getAID(), agentAction);
        try {
            agentRequester.getContentManager().fillContent(message, content);
        }
        catch (Codec.CodecException e) {
            throw new CommandException("Error in composing the command message.", e);
        }
        catch (OntologyException e) {
            throw new CommandException("Error in composing the command message.", e);
        }

        logger.debug("Sending message: {}", message);

        return message;
    }

    @Override
    protected void handleInform(ACLMessage msg)
    {
        logger.debug("Received message: {}", msg);

        ContentManager cm = myAgent.getContentManager();
        try {
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

                command.setResult(result.getValue());
                command.setState(Command.State.SUCCESSFUL);
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

                command.setState(Command.State.SUCCESSFUL);
            }
        }
        catch (Codec.CodecException e) {
            command.setFailed(new CommandResultDecodingFailed(e));
        }
        catch (OntologyException e) {
            command.setFailed(new CommandResultDecodingFailed(e));
        }
        catch (ClassCastException e) {
            command.setFailed(new CommandResultDecodingFailed(e));
        }
    }

    @Override
    protected void handleNotUnderstood(ACLMessage msg)
    {
        logger.debug("Received message: {}", msg);

        logger.error("Execution of '{}' failed, because it was not understood.", command.getAgentAction());
        command.setFailed(new CommandNotUnderstood());
    }

    @Override
    protected void handleFailure(ACLMessage msg)
    {
        logger.debug("Received message: {}", msg);

        CommandFailure commandFailure = getFailure(msg);
        logger.error("Execution of '{}' failed: {}", command.getAgentAction(), commandFailure.getMessage());
        command.setFailed(commandFailure);
    }

    @Override
    protected void handleRefuse(ACLMessage msg)
    {
        logger.debug("Received message: {}", msg);

        logger.error("Execution of '{}' failed, because it was refused.", command.getAgentAction());
        command.setFailed(new CommandRefused());
    }

    /**
     * Pattern for matching JADE internal errors.
     */
    private static final Pattern PATTERN_INTERNAL_ERROR = Pattern.compile("internal-error \"(.*)\"");

    /**
     * Pattern for matching agent not found JADE errors.
     */
    private static final Pattern PATTERN_AGENT_NOT_FOUND =
            Pattern.compile("Agent not found: getContainerID\\(\\) failed to find agent (.*)@.*");

    /**
     * Tries to parse error message out of a message.
     *
     * @param msg an error, should contain a Result with value of type CommandError or CommandNotSupported
     * @return error message found in the message, or null if it was not there
     */
    private CommandFailure getFailure(ACLMessage msg)
    {
        if (msg.getSender().equals(myAgent.getAMS())) {
            String content = msg.getContent();
            Matcher matcher = PATTERN_INTERNAL_ERROR.matcher(content);
            if (matcher.find()) {
                content = matcher.group(1);
            }
            Matcher agentNotFoundMatcher = PATTERN_AGENT_NOT_FOUND.matcher(content);
            if (agentNotFoundMatcher.find()) {
                String connectorAgentName = agentNotFoundMatcher.group(1);
                return new CommandAgentNotFound(connectorAgentName);
            }
            return new CommandUnknownFailure(content);
        }
        else {
            ContentManager cm = myAgent.getContentManager();
            try {
                ContentElement contentElement = cm.extractContent(msg);
                Result result = (Result) contentElement;
                Object commandFailure = result.getValue();
                if (commandFailure instanceof CommandFailure) {
                    return (CommandFailure) commandFailure;
                }
                else {
                    return new CommandResultDecodingFailed();
                }
            }
            catch (Exception exception) {
                logger.error("Contents of the error message could not be decoded for message " + msg, exception);
                return new CommandResultDecodingFailed(exception);
            }
        }
    }
}
