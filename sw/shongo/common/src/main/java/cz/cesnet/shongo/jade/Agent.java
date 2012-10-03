package cz.cesnet.shongo.jade;

import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;
import cz.cesnet.shongo.jade.command.Command;
import cz.cesnet.shongo.jade.ontology.Message;
import cz.cesnet.shongo.jade.ontology.ShongoOntology;
import jade.content.AgentAction;
import jade.content.lang.sl.SLCodec;
import jade.core.AID;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.wrapper.AgentController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents an agent in JADE middle-ware.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Agent extends jade.core.Agent
{
    private static Logger logger = LoggerFactory.getLogger(Agent.class);

    /**
     * Is agent started? A "started" agent is that the setup() method of which has ended.
     */
    private boolean started = false;

    /**
     * Agent description for DF.
     */
    private DFAgentDescription agentDescription;

    /**
     * Map of commands by conversation identifier.
     */
    private Map<String, Command> commandByConversationId = new HashMap<String, Command>();


    /**
     * Is agent started?
     *
     * @return true if agent is started,
     *         false otherwise
     */
    public boolean isStarted()
    {
        return started;
    }


    /**
     * Perform command on local agent
     *
     * @param command    command to be performed
     */
    public Command performCommand(Command command)
    {
        if (isStarted() == false) {
            logger.error("Cannot perform command when the agent is not started.");
            return command;
        }
        try {
            startConversation(command);
            this.putO2AObject(command, AgentController.SYNC); // FIXME: should not be used by application code (according to Jade docs)
        }
        catch (InterruptedException exception) {
            logger.error("Failed to put command object to agent queue.", exception);
        }
        return command;
    }

    /**
     * Perform command on local agent and wait for it to be processed
     *
     * @param command    command to be performed
     * @return command (potentially modified)
     */
    public Command performCommandAndWait(Command command)
    {
        performCommand(command);
        command.waitForProcessed();
        return command;
    }


    private void startConversation(Command command)
    {
        commandByConversationId.put(command.getIdentifier(), command);
    }


    /**
     * Gets data from a given conversation.
     *
     * @param conversationId    ID of the conversation to get data for
     * @return command for the conversation
     */
    public Command getConversationCommand(String conversationId)
    {
        return commandByConversationId.get(conversationId);
    }


    /**
     * Ends a previously started conversation.
     *
     * Disposes of the command which originated the conversation, which prevents the agent from remembering all the
     * commands it has ever sent.
     *
     * TODO: call this method automatically to forget very old conversations to save memory
     *
     * @param conversationId      ID of the conversation
     * @param state               state to set to the originating command
     * @param stateDescription    description to the state set to the originating command
     */
    void endConversation(String conversationId, Command.State state, String stateDescription)
    {
        if (conversationId == null) {
            return; // no conversation to end
        }

        Command command = commandByConversationId.get(conversationId);
        if (command != null) {
            command.setState(state, stateDescription);
            commandByConversationId.remove(conversationId); // free the command from conversation memory
        }
    }


    /**
     * Ends a previously started conversation.
     *
     * Disposes of the command which originated the conversation.
     *
     * @param conversationId      ID of the conversation
     * @param state               state to set to the originating command
     */
    public void endConversation(String conversationId, Command.State state)
    {
        endConversation(conversationId, state, null);
    }

    /**
     * Constructor.
     */
    public Agent()
    {
        super();

        // Agent will accept objects from container
        // NOTE: must be enabled here, setup() is too late - someone could pass an object before setup()
        setEnabledO2ACommunication(true, 0);
    }

    @Override
    protected void setup()
    {
        super.setup();

        started = true;

        // Register content language
        getContentManager().registerLanguage(new SLCodec());

        // Register ontology used by Shongo
        getContentManager().registerOntology(ShongoOntology.getInstance());

        // Each agent is able to process commands passed via O2A channel and receive JADE messages
        addBehaviour(new CommandBehaviour());
        addBehaviour(new ReceiverBehaviour());

        // Prepare agent description for DF
        agentDescription = new DFAgentDescription();
        agentDescription.setName(getAID());

        logger.debug("Agent [{}] is ready!", getAID().getName());
    }

    @Override
    protected void takeDown()
    {
        unregisterServices();

        started = false;

        super.takeDown();

        logger.debug("Agent [{}] exiting!", getAID().getName());
    }

    /**
     * (Re)register services for agent.
     */
    private void registerServices()
    {
        // If agent is already registered, unregister it from DF
        try {
            DFAgentDescription agentDescription = new DFAgentDescription();
            agentDescription.setName(getAID());

            DFAgentDescription[] result = DFService.search(this, agentDescription);
            if (result.length > 0) {
                DFService.deregister(this);
            }
        }
        catch (Exception exception) {
            logger.error("Failed to search on DF", exception);
        }

        // Register agent to DF
        try {
            DFService.register(this, agentDescription);
        }
        catch (Exception exception) {
            logger.error("Failed to register to DF", exception);
        }
    }

    /**
     * Unregister services for agent.
     */
    private void unregisterServices()
    {
        // If agent is not registered, do nothing
        try {
            DFAgentDescription agentDescription = new DFAgentDescription();
            agentDescription.setName(getAID());

            DFAgentDescription[] result = DFService.search(this, agentDescription);
            if (result.length == 0) {
                return;
            }
        }
        catch (Exception exception) {
            logger.error("Failed to search on DF", exception);
        }

        // Unregister agent from DF
        try {
            DFService.deregister(this);
        }
        catch (Exception exception) {
            logger.error("Failed to unregister from DF", exception);
        }
    }

    /**
     * Register a new service with the agent.
     *
     * @param serviceType
     * @param serviceName
     */
    public void registerService(String serviceType, String serviceName)
    {
        ServiceDescription serviceDescription = new ServiceDescription();
        serviceDescription.setType(serviceType);
        serviceDescription.setName(serviceName);

        agentDescription.addServices(serviceDescription);

        registerServices();
    }

    /**
     * Find agents by service.
     *
     * @param serviceType
     * @return array of agents
     */
    public AID[] findAgentsByService(String serviceType, long timeout)
    {
        ServiceDescription serviceDescription = new ServiceDescription();
        serviceDescription.setType(serviceType);

        DFAgentDescription agentDescription = new DFAgentDescription();
        agentDescription.addServices(serviceDescription);

        try {
            DFAgentDescription[] result = DFService
                    .searchUntilFound(this, getDefaultDF(), agentDescription, null, timeout);
            //DFAgentDescription[] result = DFService.search(this, agentDescription);
            if (result == null) {
                result = new DFAgentDescription[0];
            }
            AID[] agents = new AID[result.length];
            for (int i = 0; i < result.length; ++i) {
                agents[i] = result[i].getName();
            }
            return agents;
        }
        catch (Exception exception) {
            logger.error("Failed to find agents by a service", exception);
        }
        return new AID[0];
    }

    /**
     * Handles an agent action request.
     *
     * @param action    agent action to be performed
     * @param sender    sender of the action request
     * @return return value of the performed command (null if the command does not return anything)
     * @throws UnknownActionException
     */
    public Object handleAgentAction(AgentAction action, AID sender)
            throws UnknownActionException, CommandException, CommandUnsupportedException
    {
        if (action == null) {
            throw new NullPointerException("action");
        }

        if (action instanceof Message) {
            Message message = (Message) action;
            System.out.println("Message from " + sender.getName() + ": " + message.getMessage() + "\n\n");
            return null;
        }

        throw new UnknownActionException(action);
    }
}
