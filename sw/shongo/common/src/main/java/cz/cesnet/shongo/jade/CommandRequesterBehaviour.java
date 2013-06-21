package cz.cesnet.shongo.jade;

import cz.cesnet.shongo.JadeReport;
import cz.cesnet.shongo.JadeReportSet;
import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.Command;
import jade.content.ContentElement;
import jade.content.ContentManager;
import jade.content.lang.Codec;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Done;
import jade.content.onto.basic.Result;
import jade.core.AID;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.proto.SimpleAchieveREInitiator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Behaviour that sends an {@link ACLMessage} from given {@link cz.cesnet.shongo.jade.Agent} to target JADE agent
 * specified in the {@link SendLocalCommand#receiverAgentId} and the message content is {@link SendLocalCommand#command}.
 * <p/>
 * Implements the initiator part of the standard FIPA-Request protocol (see the Jade Programmer's Guide or the Ontology
 * example found in the Jade distribution in examples/src/examples/ontology).
 * <p/>
 * See {@link CommandResponderBehaviour} class for the other party of the conversation.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class CommandRequesterBehaviour extends SimpleAchieveREInitiator
{
    private static Logger logger = LoggerFactory.getLogger(CommandRequesterBehaviour.class);

    /**
     * {@link SendLocalCommand} that invoked this behaviour (and someone is possibly waiting for it).
     */
    private SendLocalCommand sendLocalCommand;

    /**
     * Constructor.
     *
     * @param agent            representing requester who should send the request message
     * @param sendLocalCommand which defines the target agent and the request content
     */
    public CommandRequesterBehaviour(Agent agent, SendLocalCommand sendLocalCommand) throws Exception
    {
        super(agent, createMessage(agent, sendLocalCommand.getReceiverAgentId(), sendLocalCommand.getCommand()));

        this.sendLocalCommand = sendLocalCommand;
    }

    /**
     * Create {@link ACLMessage} which will be sent.
     *
     * @param agentRequester  sender agent
     * @param agentReceiverId receiver agent id
     * @param command         {@link Command} which should be sent
     * @return the constructed message
     * @throws Exception when the message construction fails
     */
    private static ACLMessage createMessage(Agent agentRequester, AID agentReceiverId, Command command)
            throws Exception
    {
        ACLMessage message = new ACLMessage(ACLMessage.REQUEST);
        message.addReceiver(agentReceiverId);
        message.setSender(agentRequester.getAID());
        message.setLanguage(FIPANames.ContentLanguage.FIPA_SL);
        message.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
        message.setOntology(command.getOntology().getName());

        ContentElement content = new jade.content.onto.basic.Action(agentRequester.getAID(), command);
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
                // Result value of a command
                Result result = (Result) contentElement;
                logger.debug("Received a result of type {}, value {}.",
                        result.getValue().getClass(), result.getValue());
                sendLocalCommand.setResult(result.getValue());
                sendLocalCommand.setState(SendLocalCommand.State.SUCCESSFUL);
            }
            else if (contentElement instanceof Done) {
                // Notification that a command succeeded
                Done done = (Done) contentElement;
                logger.debug("Received confirmation of successful execution of an action.");
                sendLocalCommand.setState(SendLocalCommand.State.SUCCESSFUL);
            }
        }
        catch (Codec.CodecException exception) {
            logger.error("Decoding failed", exception);
            sendLocalCommand.setFailed(new JadeReportSet.CommandResultDecodingFailedReport(
                    sendLocalCommand.getCommand().toString(), myAgent.getAID().getName()));
        }
        catch (OntologyException exception) {
            logger.error("Decoding failed", exception);
            sendLocalCommand.setFailed(new JadeReportSet.CommandResultDecodingFailedReport(
                    sendLocalCommand.getCommand().toString(), myAgent.getAID().getName()));
        }
        catch (ClassCastException exception) {
            logger.error("Decoding failed", exception);
            sendLocalCommand.setFailed(new JadeReportSet.CommandResultDecodingFailedReport(
                    sendLocalCommand.getCommand().toString(), myAgent.getAID().getName()));
        }
    }

    @Override
    protected void handleNotUnderstood(ACLMessage msg)
    {
        logger.debug("Received message: {}", msg);

        logger.error("Execution of '{}' failed, because it was not understood.", sendLocalCommand.getCommand());
        sendLocalCommand.setFailed(new JadeReportSet.CommandNotUnderstoodReport(
                sendLocalCommand.getCommand().toString(), sendLocalCommand.getReceiverAgentId().getName()));
    }

    @Override
    protected void handleFailure(ACLMessage msg)
    {
        logger.debug("Received message: {}", msg);

        JadeReport commandFailure = getFailure(msg);
        logger.error("Execution of '{}' failed: {}", sendLocalCommand.getCommand(), commandFailure.getMessage());
        sendLocalCommand.setFailed(commandFailure);
    }

    @Override
    protected void handleRefuse(ACLMessage msg)
    {
        logger.debug("Received message: {}", msg);

        logger.error("Execution of '{}' failed, because it was refused.", sendLocalCommand.getCommand());
        sendLocalCommand.setFailed(new JadeReportSet.CommandRefusedReport(
                sendLocalCommand.getCommand().toString(), sendLocalCommand.getReceiverAgentId().getName()));
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
    private JadeReport getFailure(ACLMessage msg)
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
                return new JadeReportSet.AgentNotFoundReport(connectorAgentName);
            }
            return new JadeReportSet.UnknownErrorReport(content);
        }
        else {
            ContentManager cm = myAgent.getContentManager();
            try {
                ContentElement contentElement = cm.extractContent(msg);
                Result result = (Result) contentElement;
                Object commandFailure = result.getValue();
                if (commandFailure instanceof JadeReport) {
                    return (JadeReport) commandFailure;
                }
                else {
                    return new JadeReportSet.CommandResultDecodingFailedReport(
                            sendLocalCommand.getCommand().toString(), myAgent.getAID().getName());
                }
            }
            catch (Exception exception) {
                logger.error("Contents of the error message could not be decoded for message " + msg, exception);
                return new JadeReportSet.CommandResultDecodingFailedReport(
                        sendLocalCommand.getCommand().toString(), myAgent.getAID().getName());
            }
        }
    }
}
