package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.controller.Component;
import cz.cesnet.shongo.controller.Configuration;
import cz.cesnet.shongo.controller.ControllerAgent;
import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.resource.ManagedMode;
import cz.cesnet.shongo.controller.resource.Mode;
import cz.cesnet.shongo.controller.resource.ResourceManager;
import cz.cesnet.shongo.fault.FaultException;
import cz.cesnet.shongo.jade.command.SendCommand;
import cz.cesnet.shongo.jade.ontology.*;
import jade.content.AgentAction;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

/**
 * Resource service implementation.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ResourceControlServiceImpl extends Component
        implements ResourceControlService, Component.DomainAware, Component.ControllerAgentAware,
                   Component.EntityManagerFactoryAware
{
    /**
     * @see Domain
     */
    private Domain domain;

    /**
     * @see ControllerAgent
     */
    private ControllerAgent controllerAgent;

    /**
     * @see javax.persistence.EntityManagerFactory
     */
    private EntityManagerFactory entityManagerFactory;

    @Override
    public void setDomain(Domain domain)
    {
        this.domain = domain;
    }

    @Override
    public void setControllerAgent(ControllerAgent controllerAgent)
    {
        this.controllerAgent = controllerAgent;
    }

    @Override
    public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory)
    {
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public void init(Configuration configuration)
    {
        checkDependency(domain, Domain.class);
        checkDependency(controllerAgent, ControllerAgent.class);
        super.init(configuration);
    }

    @Override
    public String getServiceName()
    {
        return "ResourceControl";
    }

    @Override
    public String dial(SecurityToken token, String deviceResourceIdentifier, String target) throws FaultException
    {
        String agentName = getAgentName(deviceResourceIdentifier);
        controllerAgent.performCommand(SendCommand.createSendCommand(agentName, new Dial(target)));
        return String.format("Dialing in '%s' for '%s'...", agentName, target);
    }

    @Override
    public String standBy(SecurityToken token, String deviceResourceIdentifier) throws FaultException
    {
        String agentName = getAgentName(deviceResourceIdentifier);
        controllerAgent.performCommand(SendCommand.createSendCommand(agentName, new StandBy()));
        return String.format("Setting '%s' to standby mode via '%s' agent.", deviceResourceIdentifier, agentName);
    }

    @Override
    public String hangUpAll(SecurityToken token, String deviceResourceIdentifier) throws FaultException
    {
        String agentName = getAgentName(deviceResourceIdentifier);
        controllerAgent.performCommand(SendCommand.createSendCommand(agentName, new HangUpAll()));
        return String.format("Hanging up all calls in '%s'", agentName);
    }

    @Override
    public String mute(SecurityToken token, String deviceResourceIdentifier) throws FaultException
    {
        String agentName = getAgentName(deviceResourceIdentifier);
        controllerAgent.performCommand(SendCommand.createSendCommand(agentName, new Mute()));
        return String.format("Muting the '%s'.", agentName);
    }

    @Override
    public String unmute(SecurityToken token, String deviceResourceIdentifier) throws FaultException
    {
        String agentName = getAgentName(deviceResourceIdentifier);
        controllerAgent.performCommand(SendCommand.createSendCommand(agentName, new Unmute()));
        return String.format("Unmuting the '%s'.", agentName);
    }

    @Override
    public String setMicrophoneLevel(SecurityToken token, String deviceResourceIdentifier, int level) throws FaultException
    {
        String agentName = getAgentName(deviceResourceIdentifier);
        controllerAgent.performCommand(SendCommand.createSendCommand(agentName, new SetMicrophoneLevel(level)));
        return String.format("Setting up microphone level to %d", level);
    }

    @Override
    public String setPlaybackLevel(SecurityToken token, String deviceResourceIdentifier, int level) throws FaultException
    {
        String agentName = getAgentName(deviceResourceIdentifier);
        controllerAgent.performCommand(SendCommand.createSendCommand(agentName, new SetPlaybackLevel(level)));
        return String.format("Setting up playback level to %d", level);
    }

    @Override
    public void dialParticipant(SecurityToken token, String deviceResourceIdentifier, String roomId,
            String roomUserId, Alias alias) throws FaultException
    {
        String agentName = getAgentName(deviceResourceIdentifier);
        AgentAction act = new DialParticipant(roomId, roomUserId, alias);
        controllerAgent.performCommand(SendCommand.createSendCommand(agentName, act));
    }

    /**
     * @param deviceResourceIdentifier
     * @return agent name of managed resource with given {@code deviceResourceIdentifier}
     * @throws FaultException when resource doesn't exist or when is not managed
     */
    private String getAgentName(String deviceResourceIdentifier) throws FaultException
    {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        Long deviceResourceId = domain.parseIdentifier(deviceResourceIdentifier);
        ResourceManager resourceManager = new ResourceManager(entityManager);
        DeviceResource deviceResource = resourceManager.getDevice(deviceResourceId);
        entityManager.close();
        Mode mode = deviceResource.getMode();
        if (mode instanceof ManagedMode) {
            ManagedMode managedMode = (ManagedMode) mode;
            return managedMode.getConnectorAgentName();
        }
        throw new FaultException("Resource '%s' is not managed!", deviceResourceIdentifier);
    }
}
