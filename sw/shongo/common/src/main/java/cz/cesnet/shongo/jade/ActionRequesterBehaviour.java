package cz.cesnet.shongo.jade;

import cz.cesnet.shongo.fault.jade.*;
import cz.cesnet.shongo.jade.command.ActionRequestCommand;
import cz.cesnet.shongo.jade.command.Command;
import cz.cesnet.shongo.jade.ontology.CommandError;
import cz.cesnet.shongo.jade.ontology.CommandNotSupported;
import jade.content.ContentElement;
import jade.content.ContentManager;
import jade.content.lang.Codec;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.content.onto.basic.Done;
import jade.content.onto.basic.Result;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.proto.SimpleAchieveREInitiator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Behaviour that requests a connector to perform an action.
 * <p/>
 * Automatically run on the controller by a command processing.
 * <p/>
 * Implements the initiator part of the standard FIPA-Request protocol (see the Jade Programmer's Guide or the Ontology
 * example found in the Jade distribution in examples/src/examples/ontology).
 * <p/>
 * See ActionRequestResponderBehaviour class for the other party of the conversation.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class ActionRequesterBehaviour extends SimpleAchieveREInitiator
{
    private Logger logger = LoggerFactory.getLogger(ActionRequesterBehaviour.class);

    /**
     * Command that invoked this behaviour (and someone is possibly waiting for it).
     */
    private ActionRequestCommand command;


    public ActionRequesterBehaviour(jade.core.Agent agent, ACLMessage requestMsg, ActionRequestCommand command)
    {
        super(agent, requestMsg);
        requestMsg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
        this.command = command;

        logger.debug("Sending message: {}", requestMsg);
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
            command.setFailed(new CommandResultDecodingException(e));
        }
        catch (OntologyException e) {
            command.setFailed(new CommandResultDecodingException(e));
        }
        catch (ClassCastException e) {
            command.setFailed(new CommandResultDecodingException(e));
        }
    }

    @Override
    protected void handleNotUnderstood(ACLMessage msg)
    {
        logger.debug("Received message: {}", msg);

        logger.error("Execution of the command failed: {}", msg);
        command.setFailed(new CommandNotUnderstoodException());
    }

    @Override
    protected void handleFailure(ACLMessage msg)
    {
        logger.debug("Received message: {}", msg);

        logger.error("Execution of the command failed: {}", msg);
        command.setFailed(getFailure(msg));
    }

    @Override
    protected void handleRefuse(ACLMessage msg)
    {
        logger.debug("Received message: {}", msg);

        logger.error("Execution of the command failed: {}", msg);
        command.setFailed(new CommandRefusedException());
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
    private CommandFailureException getFailure(ACLMessage msg)
    {
        String content = msg.getContent();
        if (msg.getSender().equals(myAgent.getAMS())) {
            Matcher matcher = PATTERN_INTERNAL_ERROR.matcher(content);
            if (matcher.find()) {
                content = matcher.group(1);
            }
            Matcher agentNotFoundMatcher = PATTERN_AGENT_NOT_FOUND.matcher(content);
            if (agentNotFoundMatcher.find()) {
                String connectorAgentName = agentNotFoundMatcher.group(1);
                return new CommandConnectorNotFoundException(connectorAgentName);
            }
        }
        else {
            ContentManager cm = myAgent.getContentManager();
            try {
                ContentElement contentElement = cm.extractContent(msg);
                Result result = (Result) contentElement;
                Object commandError = result.getValue();
                if (commandError instanceof CommandNotSupported) {
                    content = ((CommandNotSupported) commandError).getDescription();
                }
                if (commandError instanceof CommandError) {
                    content = ((CommandError) commandError).getDescription();
                }
            }
            catch (Exception exception) {
                logger.error("Contents of the error message could not be decoded for message " + msg, exception);
            }
        }
        return new CommandFailureException(content);
    }

}
