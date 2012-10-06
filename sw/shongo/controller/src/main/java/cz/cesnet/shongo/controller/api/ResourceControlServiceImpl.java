package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.Room;
import cz.cesnet.shongo.api.RoomSummary;
import cz.cesnet.shongo.controller.Component;
import cz.cesnet.shongo.controller.Configuration;
import cz.cesnet.shongo.controller.ControllerAgent;
import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.resource.ManagedMode;
import cz.cesnet.shongo.controller.resource.Mode;
import cz.cesnet.shongo.controller.resource.ResourceManager;
import cz.cesnet.shongo.fault.FaultException;
import cz.cesnet.shongo.jade.command.ActionRequestCommand;
import cz.cesnet.shongo.jade.command.Command;
import cz.cesnet.shongo.jade.ontology.*;
import jade.content.AgentAction;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.Collection;
import java.util.List;

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
    public String dial(SecurityToken token, String deviceResourceIdentifier, String address) throws FaultException
    {
        return (String) commandDevice(deviceResourceIdentifier, new Dial(address));
    }

    @Override
    public String dial(SecurityToken token, String deviceResourceIdentifier, Alias alias) throws FaultException
    {
        return (String) commandDevice(deviceResourceIdentifier, new Dial(alias));
    }

    @Override
    public void standBy(SecurityToken token, String deviceResourceIdentifier) throws FaultException
    {
        commandDevice(deviceResourceIdentifier, new StandBy());
    }

    @Override
    public void hangUp(SecurityToken token, String deviceResourceIdentifier, String callId) throws FaultException
    {
        commandDevice(deviceResourceIdentifier, new HangUp(callId));
    }

    @Override
    public void hangUpAll(SecurityToken token, String deviceResourceIdentifier) throws FaultException
    {
        commandDevice(deviceResourceIdentifier, new HangUpAll());
    }

    @Override
    public void mute(SecurityToken token, String deviceResourceIdentifier) throws FaultException
    {
        commandDevice(deviceResourceIdentifier, new Mute());
    }

    @Override
    public void unmute(SecurityToken token, String deviceResourceIdentifier) throws FaultException
    {
        commandDevice(deviceResourceIdentifier, new Unmute());
    }

    @Override
    public void setMicrophoneLevel(SecurityToken token, String deviceResourceIdentifier, int level)
            throws FaultException
    {
        commandDevice(deviceResourceIdentifier, new SetMicrophoneLevel(level));
    }

    @Override
    public void setPlaybackLevel(SecurityToken token, String deviceResourceIdentifier, int level) throws FaultException
    {
        commandDevice(deviceResourceIdentifier, new SetPlaybackLevel(level));
    }

    @Override
    public String dialParticipant(SecurityToken token, String deviceResourceIdentifier, String roomId, String address)
            throws FaultException
    {
        return (String) commandDevice(deviceResourceIdentifier, new DialParticipant(roomId, address));
    }

    @Override
    public String dialParticipant(SecurityToken token, String deviceResourceIdentifier, String roomId, Alias alias)
            throws FaultException
    {
        return (String) commandDevice(deviceResourceIdentifier, new DialParticipant(roomId, alias));
    }

    @Override
    public void disconnectParticipant(SecurityToken token, String deviceResourceIdentifier, String roomId,
            String roomUserId) throws FaultException
    {
        commandDevice(deviceResourceIdentifier, new DisconnectParticipant(roomId, roomUserId));
    }

    @Override
    public String createRoom(SecurityToken token, String deviceResourceIdentifier, Room room) throws FaultException
    {
        return (String) commandDevice(deviceResourceIdentifier, new CreateRoom(room));
    }

    @Override
    public void deleteRoom(SecurityToken token, String deviceResourceIdentifier, String roomId) throws FaultException
    {
        commandDevice(deviceResourceIdentifier, new DeleteRoom(roomId));
    }

    @Override
    public Collection<RoomSummary> listRooms(SecurityToken token, String deviceResourceIdentifier) throws FaultException
    {
        List<RoomSummary> roomSummaries = (List<RoomSummary>) commandDevice(deviceResourceIdentifier, new ListRooms());
        return roomSummaries;
    }

    /**
     * Asks the local controller agent to send a command to be performed by a device.
     *
     * @param deviceResourceIdentifier identifier of device to perform a command
     * @param action                   command to be performed by the device
     * @throws FaultException
     */
    private Object commandDevice(String deviceResourceIdentifier, AgentAction action) throws FaultException
    {
        String agentName = getAgentName(deviceResourceIdentifier);
        Command command = controllerAgent.performCommandAndWait(new ActionRequestCommand(agentName, action));
        if (command.getState() == Command.State.SUCCESSFUL) {
            return command.getResult();
        }
        throw new FaultException(command.getStateDescription());

    }

    /**
     * Gets name of agent managing a given device.
     *
     * @param deviceResourceIdentifier identifier of device agent of which to get
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
