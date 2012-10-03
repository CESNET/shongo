package cz.cesnet.shongo.jade;

import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;
import cz.cesnet.shongo.jade.command.Command;
import cz.cesnet.shongo.jade.ontology.Message;
import cz.cesnet.shongo.jade.ontology.ShongoOntology;
import jade.content.AgentAction;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
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
     * Represents an empty successful response.
     */
    public static Object COMMAND_RESPONSE_DONE = new Object();

    /**
     * Is agent started?
     */
    private boolean started = false;

    /**
     * Agent description for DF.
     */
    private DFAgentDescription agentDescription;

    /**
     * Map of response by command identifier.
     */
    private Map<String, Command> commandByIdentifier = new HashMap<String, Command>();

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
     * Register ontology that agent can handle.
     *
     * @param ontology
     */
    public void registerOntology(Ontology ontology)
    {
        getContentManager().registerOntology(ontology);
    }

    /**
     * Perform command on local agent
     *
     * @param command
     */
    public Command performCommand(Command command)
    {
        if (isStarted() == false) {
            logger.error("Cannot perform command when the agent is not started.");
            return command;
        }
        try {
            this.putO2AObject(command, AgentController.SYNC); // FIXME: should not be used by application code (according to Jade docs)

            // Put empty response
            commandByIdentifier.put(command.getIdentifier(), command);
        }
        catch (InterruptedException exception) {
            logger.error("Failed to put command object to agent queue.", exception);
        }
        return command;
    }

    /**
     * Perform command on local agent and wait for it to be processed
     *
     * @param command
     * @return command
     */
    public Command performCommandAndWait(Command command)
    {
        performCommand(command);
        command.waitForProcessed();
        return command;
    }

    /**
     * Push new command response.
     *
     * @param commandIdentifier
     * @return command with given {@code commandIdentifier}
     */
    public Command getCommand(String commandIdentifier)
    {
        return commandByIdentifier.get(commandIdentifier);
    }

    /**
     * Disposal of a previously stored command.
     *
     * Use to prevent agent from remembering all the commands it has ever sent.
     *
     * @param commandIdentifier    identifier of the command
     */
    public void disposeCommand(String commandIdentifier)
    {
        commandByIdentifier.remove(commandIdentifier);
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
