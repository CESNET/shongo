package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.*;
import cz.cesnet.shongo.connector.api.ontology.ConnectorAgentAction;
import cz.cesnet.shongo.connector.api.ontology.actions.common.GetDeviceLoadInfo;
import cz.cesnet.shongo.connector.api.ontology.actions.common.GetSupportedMethods;
import cz.cesnet.shongo.connector.api.ontology.actions.endpoint.*;
import cz.cesnet.shongo.connector.api.ontology.actions.multipoint.io.*;
import cz.cesnet.shongo.connector.api.ontology.actions.multipoint.rooms.*;
import cz.cesnet.shongo.connector.api.ontology.actions.multipoint.users.*;
import cz.cesnet.shongo.controller.*;
import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.resource.ManagedMode;
import cz.cesnet.shongo.controller.resource.Mode;
import cz.cesnet.shongo.controller.resource.ResourceManager;
import cz.cesnet.shongo.fault.FaultException;
import cz.cesnet.shongo.fault.jade.CommandFailureException;
import cz.cesnet.shongo.jade.command.AgentActionCommand;
import cz.cesnet.shongo.jade.command.Command;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Resource service implementation.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ResourceControlServiceImpl extends Component
        implements ResourceControlService, Component.DomainAware, Component.ControllerAgentAware,
                   Component.EntityManagerFactoryAware, Component.AuthorizationAware
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

    /**
     * @see cz.cesnet.shongo.controller.Authorization
     */
    private Authorization authorization;

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
    public void setAuthorization(Authorization authorization)
    {
        this.authorization = authorization;
    }

    @Override
    public void init(Configuration configuration)
    {
        checkDependency(domain, Domain.class);
        checkDependency(controllerAgent, ControllerAgent.class);
        checkDependency(authorization, Authorization.class);
        super.init(configuration);
    }

    @Override
    public String getServiceName()
    {
        return "ResourceControl";
    }

    @Override
    public Collection<String> getSupportedMethods(SecurityToken token, String deviceResourceId)
            throws FaultException
    {
        authorization.validate(token);
        return (List<String>) commandDevice(deviceResourceId, new GetSupportedMethods());
    }

    @Override
    public DeviceLoadInfo getDeviceLoadInfo(SecurityToken token, String deviceResourceId) throws FaultException
    {
        authorization.validate(token);
        return (DeviceLoadInfo) commandDevice(deviceResourceId, new GetDeviceLoadInfo());
    }

    @Override
    public String dial(SecurityToken token, String deviceResourceId, String address) throws FaultException
    {
        authorization.validate(token);
        return (String) commandDevice(deviceResourceId, new Dial(address));
    }

    @Override
    public String dial(SecurityToken token, String deviceResourceId, Alias alias) throws FaultException
    {
        authorization.validate(token);
        return (String) commandDevice(deviceResourceId, new Dial(alias));
    }

    @Override
    public void standBy(SecurityToken token, String deviceResourceId) throws FaultException
    {
        authorization.validate(token);
        commandDevice(deviceResourceId, new StandBy());
    }

    @Override
    public void hangUp(SecurityToken token, String deviceResourceId, String callId) throws FaultException
    {
        authorization.validate(token);
        commandDevice(deviceResourceId, new HangUp(callId));
    }

    @Override
    public void hangUpAll(SecurityToken token, String deviceResourceId) throws FaultException
    {
        authorization.validate(token);
        commandDevice(deviceResourceId, new HangUpAll());
    }

    @Override
    public void rebootDevice(SecurityToken token, String deviceResourceId) throws FaultException
    {
        authorization.validate(token);
        commandDevice(deviceResourceId, new RebootDevice());
    }

    @Override
    public void mute(SecurityToken token, String deviceResourceId) throws FaultException
    {
        authorization.validate(token);
        commandDevice(deviceResourceId, new Mute());
    }

    @Override
    public void unmute(SecurityToken token, String deviceResourceId) throws FaultException
    {
        authorization.validate(token);
        commandDevice(deviceResourceId, new Unmute());
    }

    @Override
    public void setMicrophoneLevel(SecurityToken token, String deviceResourceId, int level)
            throws FaultException
    {
        authorization.validate(token);
        commandDevice(deviceResourceId, new SetMicrophoneLevel(level));
    }

    @Override
    public void setPlaybackLevel(SecurityToken token, String deviceResourceId, int level) throws FaultException
    {
        authorization.validate(token);
        commandDevice(deviceResourceId, new SetPlaybackLevel(level));
    }

    @Override
    public void enableVideo(SecurityToken token, String deviceResourceId) throws FaultException
    {
        authorization.validate(token);
        commandDevice(deviceResourceId, new EnableVideo());
    }

    @Override
    public void disableVideo(SecurityToken token, String deviceResourceId) throws FaultException
    {
        authorization.validate(token);
        commandDevice(deviceResourceId, new DisableVideo());
    }

    @Override
    public void startPresentation(SecurityToken token, String deviceResourceId) throws FaultException
    {
        authorization.validate(token);
        commandDevice(deviceResourceId, new StartPresentation());
    }

    @Override
    public void stopPresentation(SecurityToken token, String deviceResourceId) throws FaultException
    {
        authorization.validate(token);
        commandDevice(deviceResourceId, new StopPresentation());
    }

    @Override
    public String dialParticipant(SecurityToken token, String deviceResourceId, String roomId, String address)
            throws FaultException
    {
        authorization.validate(token);
        return (String) commandDevice(deviceResourceId, new DialParticipant(roomId, address));
    }

    @Override
    public String dialParticipant(SecurityToken token, String deviceResourceId, String roomId, Alias alias)
            throws FaultException
    {
        authorization.validate(token);
        return (String) commandDevice(deviceResourceId, new DialParticipant(roomId, alias));
    }

    @Override
    public void disconnectParticipant(SecurityToken token, String deviceResourceId, String roomId,
            String roomUserId) throws FaultException
    {
        authorization.validate(token);
        commandDevice(deviceResourceId, new DisconnectParticipant(roomId, roomUserId));
    }

    @Override
    public Collection<RoomSummary> listRooms(SecurityToken token, String deviceResourceId) throws FaultException
    {
        authorization.validate(token);
        Collection<RoomSummary> rooms = (Collection<RoomSummary>) commandDevice(deviceResourceId,
                new ListRooms());
        return rooms;
    }

    @Override
    public Room getRoom(SecurityToken token, String deviceResourceId, String roomId) throws FaultException
    {
        authorization.validate(token);
        return (Room) commandDevice(deviceResourceId, new GetRoom(roomId));
    }

    @Override
    public String createRoom(SecurityToken token, String deviceResourceId, Room room) throws FaultException
    {
        authorization.validate(token);
        return (String) commandDevice(deviceResourceId, new CreateRoom(room));
    }

    @Override
    public String modifyRoom(SecurityToken token, String deviceResourceId, Room room) throws FaultException
    {
        authorization.validate(token);
        return (String) commandDevice(deviceResourceId, new ModifyRoom(room));
    }

    @Override
    public void deleteRoom(SecurityToken token, String deviceResourceId, String roomId) throws FaultException
    {
        authorization.validate(token);
        commandDevice(deviceResourceId, new DeleteRoom(roomId));
    }

    @Override
    public Collection<RoomUser> listParticipants(SecurityToken token, String deviceResourceId, String roomId)
            throws FaultException
    {
        authorization.validate(token);
        return (List<RoomUser>) commandDevice(deviceResourceId, new ListParticipants(roomId));
    }

    @Override
    public RoomUser getParticipant(SecurityToken token, String deviceResourceId, String roomId,
            String roomUserId) throws FaultException
    {
        authorization.validate(token);
        return (RoomUser) commandDevice(deviceResourceId, new GetParticipant(roomId, roomUserId));
    }

    @Override
    public void modifyParticipant(SecurityToken token, String deviceResourceId, String roomId,
            String roomUserId, Map<String, Object> attributes) throws FaultException
    {
        authorization.validate(token);
        commandDevice(deviceResourceId, new ModifyParticipant(roomId, roomUserId, attributes));
    }

    @Override
    public void muteParticipant(SecurityToken token, String deviceResourceId, String roomId, String roomUserId)
            throws FaultException
    {
        authorization.validate(token);
        commandDevice(deviceResourceId, new MuteParticipant(roomId, roomUserId));
    }

    @Override
    public void unmuteParticipant(SecurityToken token, String deviceResourceId, String roomId,
            String roomUserId) throws FaultException
    {
        authorization.validate(token);
        commandDevice(deviceResourceId, new UnmuteParticipant(roomId, roomUserId));
    }

    @Override
    public void enableParticipantVideo(SecurityToken token, String deviceResourceId, String roomId,
            String roomUserId) throws FaultException
    {
        authorization.validate(token);
        commandDevice(deviceResourceId, new EnableParticipantVideo(roomId, roomUserId));
    }

    @Override
    public void disableParticipantVideo(SecurityToken token, String deviceResourceId, String roomId,
            String roomUserId) throws FaultException
    {
        authorization.validate(token);
        commandDevice(deviceResourceId, new DisableParticipantVideo(roomId, roomUserId));
    }

    @Override
    public void setParticipantMicrophoneLevel(SecurityToken token, String deviceResourceId, String roomId,
            String roomUserId, int level) throws FaultException
    {
        authorization.validate(token);
        commandDevice(deviceResourceId, new SetParticipantMicrophoneLevel(roomId, roomUserId, level));
    }

    @Override
    public void setParticipantPlaybackLevel(SecurityToken token, String deviceResourceId, String roomId,
            String roomUserId, int level) throws FaultException
    {
        authorization.validate(token);
        commandDevice(deviceResourceId, new SetParticipantPlaybackLevel(roomId, roomUserId, level));
    }

    @Override
    public void showMessage(SecurityToken token, String deviceResourceIdentifier, int duration, String text)
            throws FaultException
    {
        authorization.validate(token);
        commandDevice(deviceResourceIdentifier, new ShowMessage(duration, text));
    }

    /**
     * Asks the local controller agent to send a command to be performed by a device.
     *
     * @param deviceResourceId shongo-id of device to perform a command
     * @param action                   command to be performed by the device
     * @throws FaultException
     */
    private Object commandDevice(String deviceResourceId, ConnectorAgentAction action) throws FaultException
    {
        String agentName = getAgentName(deviceResourceId);
        Command command = controllerAgent.performCommandAndWait(new AgentActionCommand(agentName, action));
        if (command.getState() == Command.State.SUCCESSFUL) {
            return command.getResult();
        }
        CommandFailureException exception = command.getFailure();
        if (exception == null) {
            exception = new CommandFailureException();
        }
        throw exception;
    }

    /**
     * Gets name of agent managing a given device.
     *
     * @param deviceResourceId shongo-id of device agent of which to get
     * @return agent name of managed resource with given {@code deviceResourceId}
     * @throws FaultException when resource doesn't exist or when is not managed
     */
    private String getAgentName(String deviceResourceId) throws FaultException
    {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        Long id = domain.parseId(deviceResourceId);
        ResourceManager resourceManager = new ResourceManager(entityManager);
        DeviceResource deviceResource = resourceManager.getDevice(id);
        entityManager.close();
        Mode mode = deviceResource.getMode();
        if (mode instanceof ManagedMode) {
            ManagedMode managedMode = (ManagedMode) mode;
            return managedMode.getConnectorAgentName();
        }
        throw new FaultException("Resource '%s' is not managed!", id);
    }
}
