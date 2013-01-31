package cz.cesnet.shongo.jade;

import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;
import cz.cesnet.shongo.fault.jade.CommandAgentNotStarted;
import cz.cesnet.shongo.jade.command.Command;
import cz.cesnet.shongo.jade.command.CommandBehaviour;
import jade.content.AgentAction;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.core.AID;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
     * @param command command to be performed
     */
    public Command performCommand(Command command)
    {
        if (isStarted() == false) {
            command.setFailed(new CommandAgentNotStarted(getAID().getLocalName()));
            return command;
        }
        try {
            // FIXME: should not be used by application code (according to Jade docs)
            // this.putO2AObject(command, ,AgentController.SYNC);
            // This works (it will pass tests)
            this.putO2AObject(command, false);
        }
        catch (InterruptedException exception) {
            logger.error("Failed to put command object to agent queue.", exception);
        }
        command.waitForProcessed();
        return command;
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

    /**
     * @param ontology to be registered to agnet
     */
    public void addOntology(Ontology ontology)
    {
        // Register ontology used by Shongo
        getContentManager().registerOntology(ontology);
    }

    @Override
    protected void setup()
    {
        // Register content language
        getContentManager().registerLanguage(new SLCodec());

        // Each agent is able to process commands passed via O2A channel
        addBehaviour(new CommandBehaviour());

        // Prepare agent description for DF
        agentDescription = new DFAgentDescription();
        agentDescription.setName(getAID());

        logger.debug("Agent [{}] is ready!", getAID().getName());

        started = true;
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
     * <p/>
     * Should be overridden by descendants to actually handle some action.
     *
     * @param action agent action to be performed
     * @param sender sender of the action request
     * @return return value of the performed command (null if the command does not return anything)
     * @throws UnknownAgentActionException
     */
    public Object handleAgentAction(AgentAction action, AID sender)
            throws UnknownAgentActionException, CommandException, CommandUnsupportedException
    {
        if (action == null) {
            throw new NullPointerException("action");
        }

        throw new UnknownAgentActionException(action);
    }
}
