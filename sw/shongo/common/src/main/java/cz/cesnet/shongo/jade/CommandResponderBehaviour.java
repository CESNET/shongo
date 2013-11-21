package cz.cesnet.shongo.jade;

import cz.cesnet.shongo.JadeReportSet;
import cz.cesnet.shongo.api.jade.CommandDisabledException;
import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.api.jade.Command;
import jade.content.Concept;
import jade.content.ContentElement;
import jade.content.ContentManager;
import jade.content.lang.Codec;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.content.onto.basic.Done;
import jade.content.onto.basic.Result;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Behaviour that performs and responds to {@link Command} requests sent by {@link CommandRequesterBehaviour}.
 * <p/>
 * See {@link CommandRequesterBehaviour} class for the other party of the conversation.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class CommandResponderBehaviour extends ParallelResponderBehaviour
{
    private static Logger logger = LoggerFactory.getLogger(CommandResponderBehaviour.class);

    /**
     * {@link cz.cesnet.shongo.jade.Agent} which is used for handling and replying to received {@link Command}s.
     */
    protected cz.cesnet.shongo.jade.Agent agent;

    /**
     * Template defining which messages are received by the {@link CommandResponderBehaviour}.
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
    public CommandResponderBehaviour(jade.core.Agent agent)
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

    @Override
    protected void handleRequest(ACLMessage requestMessage)
    {
        logger.debug("Received message: {}", requestMessage);

        RequestHandler handler = new RequestHandler(requestMessage);

        Thread handlerThread = new Thread(handler);
        handlerThread.setName(agent.getLocalName() + "-handler");
        handlerThread.start();
        addSubBehaviour(handler);
    }

    /**
     * Request handler.
     */
    private class RequestHandler extends Behaviour implements Runnable
    {
        /**
         * Request which is being handled.
         */
        private ACLMessage request;

        /**
         * Response by which the request is handled.
         */
        private ACLMessage response;

        /**
         * Specifies whether response has been sent.
         */
        private boolean done = false;

        /**
         * Constructor.
         *
         * @param request sets the {@link #request}
         */
        public RequestHandler(ACLMessage request)
        {
            this.request = request;
        }

        @Override
        public void action()
        {
            if (response != null) {
                logger.debug("Sending reply: {}", response);

                agent.send(response);
                response = null;
                done = true;
            }
            else {
                block();
            }
        }

        @Override
        public boolean done()
        {
            return done;
        }

        @Override
        public void run()
        {
            ACLMessage reply = request.createReply();

            ContentManager cm = agent.getContentManager();
            try {
                Action action = (Action) cm.extractContent(request);
                Concept actionContent = action.getAction();
                if (!(actionContent instanceof Command)) {
                    logger.error(String.format("Unknown action '%s' requested by '%s'.",
                            actionContent.getClass(), request.getSender().getName()));
                    reply.setPerformative(ACLMessage.REFUSE);
                }
                Command command = (Command) actionContent;
                try {
                    Object result = agent.handleCommand(command, request.getSender());
                    // respond to the caller - either with the command return value or saying it was OK
                    ContentElement response = (result == null ? new Done(action) : new Result(action, result));
                    fillMessage(reply, ACLMessage.INFORM, response);
                }
                catch (UnknownCommandException exception) {
                    logger.error(String.format("Unknown command '%s' requested by '%s'.",
                            exception.getCommand(), request.getSender().getName()), exception);
                    reply.setPerformative(ACLMessage.REFUSE);
                }
                catch (CommandUnsupportedException exception) {
                    logger.error(String.format("Unsupported command requested by '%s'.",
                            request.getSender().getName()), exception);
                    ContentElement response = new Result(action, new JadeReportSet.CommandNotSupportedReport(
                            command.getName(), agent.getAID().getName()));
                    fillMessage(reply, ACLMessage.FAILURE, response);
                }
                catch (CommandDisabledException exception) {
                    logger.error(String.format("Command is disabled requested by '%s'.",
                            request.getSender().getName()), exception);
                    ContentElement response = new Result(action, new JadeReportSet.AgentNotFoundReport(
                            agent.getAID().getName()));
                    fillMessage(reply, ACLMessage.FAILURE, response);
                }
                catch (CommandException exception) {
                    logger.error(String.format("Command requested by '%s' has failed.",
                            request.getSender().getName()), exception);
                    ContentElement response = new Result(action, new JadeReportSet.CommandFailedReport(
                            command.getName(), agent.getAID().getName(), exception.getCode(), exception.getMessage()));
                    fillMessage(reply, ACLMessage.FAILURE, response);
                }
                catch (Throwable exception) {
                    logger.error(String.format("Command requested by '%s' has failed with unknown reason.",
                            request.getSender().getName()), exception);
                    String message = exception.getMessage();
                    if (exception.getCause() != null) {
                        message += " (" + exception.getCause().getMessage() + ")";
                    }
                    ContentElement response = new Result(action, new JadeReportSet.CommandUnknownErrorReport(
                            command.getName(), message));
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
                logger.error(
                        String.format("Received a request which the agent did not understand (wrong content type): %s",
                                request), exception);
                reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
            }
            catch (Exception exception) {
                logger.error(String.format("Received a request which the agent did not understand: %s", request),
                        exception);
                reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
            }

            response = reply;
            restart();
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
                agent.getContentManager().fillContent(message, content);
            }
            catch (Codec.CodecException e) {
                logger.error("Error composing a reply to message", e);
            }
            catch (OntologyException e) {
                logger.error("Error composing a reply to message", e);
            }
        }
    }
}
