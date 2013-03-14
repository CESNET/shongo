package cz.cesnet.shongo.controller.api.rpc;

import cz.cesnet.shongo.api.*;
import cz.cesnet.shongo.connector.api.jade.ConnectorCommand;
import cz.cesnet.shongo.connector.api.jade.common.GetDeviceLoadInfo;
import cz.cesnet.shongo.connector.api.jade.common.GetSupportedMethods;
import cz.cesnet.shongo.connector.api.jade.endpoint.*;
import cz.cesnet.shongo.connector.api.jade.multipoint.io.*;
import cz.cesnet.shongo.connector.api.jade.multipoint.rooms.*;
import cz.cesnet.shongo.connector.api.jade.multipoint.users.*;
import cz.cesnet.shongo.controller.*;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.resource.ManagedMode;
import cz.cesnet.shongo.controller.resource.Mode;
import cz.cesnet.shongo.controller.resource.ResourceManager;
import cz.cesnet.shongo.fault.FaultException;
import cz.cesnet.shongo.fault.jade.CommandFailureException;
import cz.cesnet.shongo.jade.SendLocalCommand;

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
        implements ResourceControlService, Component.ControllerAgentAware,
                   Component.EntityManagerFactoryAware, Component.AuthorizationAware
{
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
        checkDependency(controllerAgent, ControllerAgent.class);
        checkDependency(authorization, Authorization.class);
        super.init(configuration);
    }

    @Override
    public String getServiceName()
    {
        return "ResourceControl";
    }

    private EntityIdentifier validate(SecurityToken token, String deviceResourceId) throws FaultException
    {
        EntityIdentifier entityId = EntityIdentifier.parse(deviceResourceId, EntityType.RESOURCE);
        authorization.validate(token, entityId, Permission.CONTROL_RESOURCE);
        return entityId;
    }

    @Override
    public Collection<String> getSupportedMethods(SecurityToken token, String deviceResourceId)
            throws FaultException
    {
        EntityIdentifier entityId = validate(token, deviceResourceId);
        return (List<String>) performDeviceAction(entityId, new GetSupportedMethods());
    }

    @Override
    public DeviceLoadInfo getDeviceLoadInfo(SecurityToken token, String deviceResourceId) throws FaultException
    {
        EntityIdentifier entityId = validate(token, deviceResourceId);
        return (DeviceLoadInfo) performDeviceAction(entityId, new GetDeviceLoadInfo());
    }

    @Override
    public String dial(SecurityToken token, String deviceResourceId, Alias alias) throws FaultException
    {
        EntityIdentifier entityId = validate(token, deviceResourceId);
        return (String) performDeviceAction(entityId, new Dial(alias));
    }

    @Override
    public void standBy(SecurityToken token, String deviceResourceId) throws FaultException
    {
        EntityIdentifier entityId = validate(token, deviceResourceId);
        performDeviceAction(entityId, new StandBy());
    }

    @Override
    public void hangUp(SecurityToken token, String deviceResourceId, String callId) throws FaultException
    {
        EntityIdentifier entityId = validate(token, deviceResourceId);
        performDeviceAction(entityId, new HangUp(callId));
    }

    @Override
    public void hangUpAll(SecurityToken token, String deviceResourceId) throws FaultException
    {
        EntityIdentifier entityId = validate(token, deviceResourceId);
        performDeviceAction(entityId, new HangUpAll());
    }

    @Override
    public void rebootDevice(SecurityToken token, String deviceResourceId) throws FaultException
    {
        EntityIdentifier entityId = validate(token, deviceResourceId);
        performDeviceAction(entityId, new RebootDevice());
    }

    @Override
    public void mute(SecurityToken token, String deviceResourceId) throws FaultException
    {
        EntityIdentifier entityId = validate(token, deviceResourceId);
        performDeviceAction(entityId, new Mute());
    }

    @Override
    public void unmute(SecurityToken token, String deviceResourceId) throws FaultException
    {
        EntityIdentifier entityId = validate(token, deviceResourceId);
        performDeviceAction(entityId, new Unmute());
    }

    @Override
    public void setMicrophoneLevel(SecurityToken token, String deviceResourceId, int level)
            throws FaultException
    {
        EntityIdentifier entityId = validate(token, deviceResourceId);
        performDeviceAction(entityId, new SetMicrophoneLevel(level));
    }

    @Override
    public void setPlaybackLevel(SecurityToken token, String deviceResourceId, int level) throws FaultException
    {
        EntityIdentifier entityId = validate(token, deviceResourceId);
        performDeviceAction(entityId, new SetPlaybackLevel(level));
    }

    @Override
    public void enableVideo(SecurityToken token, String deviceResourceId) throws FaultException
    {
        EntityIdentifier entityId = validate(token, deviceResourceId);
        performDeviceAction(entityId, new EnableVideo());
    }

    @Override
    public void disableVideo(SecurityToken token, String deviceResourceId) throws FaultException
    {
        EntityIdentifier entityId = validate(token, deviceResourceId);
        performDeviceAction(entityId, new DisableVideo());
    }

    @Override
    public void startPresentation(SecurityToken token, String deviceResourceId) throws FaultException
    {
        EntityIdentifier entityId = validate(token, deviceResourceId);
        performDeviceAction(entityId, new StartPresentation());
    }

    @Override
    public void stopPresentation(SecurityToken token, String deviceResourceId) throws FaultException
    {
        EntityIdentifier entityId = validate(token, deviceResourceId);
        performDeviceAction(entityId, new StopPresentation());
    }

    @Override
    public String dialParticipant(SecurityToken token, String deviceResourceId, String roomId, Alias alias)
            throws FaultException
    {
        EntityIdentifier entityId = validate(token, deviceResourceId);
        return (String) performDeviceAction(entityId, new DialParticipant(roomId, alias));
    }

    @Override
    public void disconnectParticipant(SecurityToken token, String deviceResourceId, String roomId,
            String roomUserId) throws FaultException
    {
        EntityIdentifier entityId = validate(token, deviceResourceId);
        performDeviceAction(entityId, new DisconnectParticipant(roomId, roomUserId));
    }

    @Override
    public Collection<RoomSummary> listRooms(SecurityToken token, String deviceResourceId) throws FaultException
    {
        EntityIdentifier entityId = validate(token, deviceResourceId);
        return (Collection<RoomSummary>) performDeviceAction(entityId, new ListRooms());
    }

    @Override
    public Room getRoom(SecurityToken token, String deviceResourceId, String roomId) throws FaultException
    {
        EntityIdentifier entityId = validate(token, deviceResourceId);
        return (Room) performDeviceAction(entityId, new GetRoom(roomId));
    }

    @Override
    public String createRoom(SecurityToken token, String deviceResourceId, Room room) throws FaultException
    {
        EntityIdentifier entityId = validate(token, deviceResourceId);
        return (String) performDeviceAction(entityId, new CreateRoom(room));
    }

    @Override
    public String modifyRoom(SecurityToken token, String deviceResourceId, Room room) throws FaultException
    {
        EntityIdentifier entityId = validate(token, deviceResourceId);
        return (String) performDeviceAction(entityId, new ModifyRoom(room));
    }

    @Override
    public void deleteRoom(SecurityToken token, String deviceResourceId, String roomId) throws FaultException
    {
        EntityIdentifier entityId = validate(token, deviceResourceId);
        performDeviceAction(entityId, new DeleteRoom(roomId));
    }

    @Override
    public Collection<RoomUser> listParticipants(SecurityToken token, String deviceResourceId, String roomId)
            throws FaultException
    {
        EntityIdentifier entityId = validate(token, deviceResourceId);
        return (List<RoomUser>) performDeviceAction(entityId, new ListParticipants(roomId));
    }

    @Override
    public RoomUser getParticipant(SecurityToken token, String deviceResourceId, String roomId,
            String roomUserId) throws FaultException
    {
        EntityIdentifier entityId = validate(token, deviceResourceId);
        return (RoomUser) performDeviceAction(entityId, new GetParticipant(roomId, roomUserId));
    }

    @Override
    public void modifyParticipant(SecurityToken token, String deviceResourceId, String roomId,
            String roomUserId, Map<String, Object> attributes) throws FaultException
    {
        EntityIdentifier entityId = validate(token, deviceResourceId);
        performDeviceAction(entityId, new ModifyParticipant(roomId, roomUserId, attributes));
    }

    @Override
    public void muteParticipant(SecurityToken token, String deviceResourceId, String roomId, String roomUserId)
            throws FaultException
    {
        EntityIdentifier entityId = validate(token, deviceResourceId);
        performDeviceAction(entityId, new MuteParticipant(roomId, roomUserId));
    }

    @Override
    public void unmuteParticipant(SecurityToken token, String deviceResourceId, String roomId,
            String roomUserId) throws FaultException
    {
        EntityIdentifier entityId = validate(token, deviceResourceId);
        performDeviceAction(entityId, new UnmuteParticipant(roomId, roomUserId));
    }

    @Override
    public void enableParticipantVideo(SecurityToken token, String deviceResourceId, String roomId,
            String roomUserId) throws FaultException
    {
        EntityIdentifier entityId = validate(token, deviceResourceId);
        performDeviceAction(entityId, new EnableParticipantVideo(roomId, roomUserId));
    }

    @Override
    public void disableParticipantVideo(SecurityToken token, String deviceResourceId, String roomId,
            String roomUserId) throws FaultException
    {
        EntityIdentifier entityId = validate(token, deviceResourceId);
        performDeviceAction(entityId, new DisableParticipantVideo(roomId, roomUserId));
    }

    @Override
    public void setParticipantMicrophoneLevel(SecurityToken token, String deviceResourceId, String roomId,
            String roomUserId, int level) throws FaultException
    {
        EntityIdentifier entityId = validate(token, deviceResourceId);
        performDeviceAction(entityId, new SetParticipantMicrophoneLevel(roomId, roomUserId, level));
    }

    @Override
    public void setParticipantPlaybackLevel(SecurityToken token, String deviceResourceId, String roomId,
            String roomUserId, int level) throws FaultException
    {
        EntityIdentifier entityId = validate(token, deviceResourceId);
        performDeviceAction(entityId, new SetParticipantPlaybackLevel(roomId, roomUserId, level));
    }

    @Override
    public void showMessage(SecurityToken token, String deviceResourceId, int duration, String text)
            throws FaultException
    {
        EntityIdentifier entityId = validate(token, deviceResourceId);
        performDeviceAction(entityId, new ShowMessage(duration, text));
    }

    /**
     * Asks the local controller agent to send a command to be performed by a device.
     *
     * @param entityId identifier of device to perform a command
     * @param action           command to be performed by the device
     * @throws FaultException
     */
    private Object performDeviceAction(EntityIdentifier entityId, ConnectorCommand action) throws FaultException
    {
        String agentName = getAgentName(entityId);
        SendLocalCommand sendLocalCommand = controllerAgent.sendCommand(agentName, action);
        if (sendLocalCommand.getState() == SendLocalCommand.State.SUCCESSFUL) {
            return sendLocalCommand.getResult();
        }
        throw new CommandFailureException(sendLocalCommand.getFailure());
    }

    /**
     * Gets name of agent managing a given device.
     *
     * @param entityId shongo-id of device agent of which to get
     * @return agent name of managed resource with given {@code deviceResourceId}
     * @throws FaultException when resource doesn't exist or when is not managed
     */
    protected String getAgentName(EntityIdentifier entityId) throws FaultException
    {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ResourceManager resourceManager = new ResourceManager(entityManager);
        DeviceResource deviceResource = resourceManager.getDevice(entityId.getPersistenceId());
        entityManager.close();
        Mode mode = deviceResource.getMode();
        if (mode instanceof ManagedMode) {
            ManagedMode managedMode = (ManagedMode) mode;
            return managedMode.getConnectorAgentName();
        }
        throw new FaultException("Resource '%s' is not managed!", entityId.toId());
    }
}
