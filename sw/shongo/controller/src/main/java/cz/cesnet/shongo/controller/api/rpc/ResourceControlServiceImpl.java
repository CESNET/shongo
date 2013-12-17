package cz.cesnet.shongo.controller.api.rpc;

import cz.cesnet.shongo.api.*;
import cz.cesnet.shongo.connector.api.jade.ConnectorCommand;
import cz.cesnet.shongo.connector.api.jade.common.GetDeviceLoadInfo;
import cz.cesnet.shongo.connector.api.jade.common.GetSupportedMethods;
import cz.cesnet.shongo.connector.api.jade.endpoint.*;
import cz.cesnet.shongo.connector.api.jade.multipoint.io.*;
import cz.cesnet.shongo.connector.api.jade.multipoint.rooms.*;
import cz.cesnet.shongo.connector.api.jade.recording.DeleteRecording;
import cz.cesnet.shongo.connector.api.jade.recording.GetRecording;
import cz.cesnet.shongo.connector.api.jade.recording.ListRecordings;
import cz.cesnet.shongo.controller.*;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.executable.Executable;
import cz.cesnet.shongo.controller.booking.executable.ExecutableManager;
import cz.cesnet.shongo.controller.booking.room.RoomEndpoint;
import cz.cesnet.shongo.controller.booking.resource.DeviceResource;
import cz.cesnet.shongo.controller.booking.resource.ManagedMode;
import cz.cesnet.shongo.controller.booking.resource.Mode;
import cz.cesnet.shongo.controller.booking.resource.ResourceManager;
import cz.cesnet.shongo.jade.SendLocalCommand;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resource service implementation.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ResourceControlServiceImpl extends AbstractServiceImpl
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
     * @see cz.cesnet.shongo.controller.authorization.Authorization
     */
    private Authorization authorization;

    /**
     * @see RecordingsCache
     */
    private final RecordingsCache recordingsCache;

    /**
     * Constructor.
     *
     * @param recordingsCache sets the {@link #recordingsCache}
     */
    public ResourceControlServiceImpl(RecordingsCache recordingsCache)
    {
        this.recordingsCache = recordingsCache;
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
    public void init(ControllerConfiguration configuration)
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

    @Override
    public Collection<String> getSupportedMethods(SecurityToken token, String deviceResourceId)
    {
        String agentName = validate(token, deviceResourceId);
        return (List<String>) performDeviceCommand(deviceResourceId, agentName, new GetSupportedMethods());
    }

    @Override
    public DeviceLoadInfo getDeviceLoadInfo(SecurityToken token, String deviceResourceId)
    {
        String agentName = validate(token, deviceResourceId);
        return (DeviceLoadInfo) performDeviceCommand(deviceResourceId, agentName, new GetDeviceLoadInfo());
    }

    @Override
    public String dial(SecurityToken token, String deviceResourceId, Alias alias)
    {
        String agentName = validate(token, deviceResourceId);
        return (String) performDeviceCommand(deviceResourceId, agentName, new Dial(alias));
    }

    @Override
    public void standBy(SecurityToken token, String deviceResourceId)
    {
        String agentName = validate(token, deviceResourceId);
        performDeviceCommand(deviceResourceId, agentName, new StandBy());
    }

    @Override
    public void hangUp(SecurityToken token, String deviceResourceId, String callId)
    {
        String agentName = validate(token, deviceResourceId);
        performDeviceCommand(deviceResourceId, agentName, new HangUp(callId));
    }

    @Override
    public void hangUpAll(SecurityToken token, String deviceResourceId)
    {
        String agentName = validate(token, deviceResourceId);
        performDeviceCommand(deviceResourceId, agentName, new HangUpAll());
    }

    @Override
    public void rebootDevice(SecurityToken token, String deviceResourceId)
    {
        String agentName = validate(token, deviceResourceId);
        performDeviceCommand(deviceResourceId, agentName, new RebootDevice());
    }

    @Override
    public void mute(SecurityToken token, String deviceResourceId)
    {
        String agentName = validate(token, deviceResourceId);
        performDeviceCommand(deviceResourceId, agentName, new Mute());
    }

    @Override
    public void unmute(SecurityToken token, String deviceResourceId)
    {
        String agentName = validate(token, deviceResourceId);
        performDeviceCommand(deviceResourceId, agentName, new Unmute());
    }

    @Override
    public void setMicrophoneLevel(SecurityToken token, String deviceResourceId, int level)
    {
        String agentName = validate(token, deviceResourceId);
        performDeviceCommand(deviceResourceId, agentName, new SetMicrophoneLevel(level));
    }

    @Override
    public void setPlaybackLevel(SecurityToken token, String deviceResourceId, int level)
    {
        String agentName = validate(token, deviceResourceId);
        performDeviceCommand(deviceResourceId, agentName, new SetPlaybackLevel(level));
    }

    @Override
    public void enableVideo(SecurityToken token, String deviceResourceId)
    {
        String agentName = validate(token, deviceResourceId);
        performDeviceCommand(deviceResourceId, agentName, new EnableVideo());
    }

    @Override
    public void disableVideo(SecurityToken token, String deviceResourceId)
    {
        String agentName = validate(token, deviceResourceId);
        performDeviceCommand(deviceResourceId, agentName, new DisableVideo());
    }

    @Override
    public void startPresentation(SecurityToken token, String deviceResourceId)
    {
        String agentName = validate(token, deviceResourceId);
        performDeviceCommand(deviceResourceId, agentName, new StartPresentation());
    }

    @Override
    public void stopPresentation(SecurityToken token, String deviceResourceId)
    {
        String agentName = validate(token, deviceResourceId);
        performDeviceCommand(deviceResourceId, agentName, new StopPresentation());
    }

    @Override
    public Collection<RoomSummary> listRooms(SecurityToken token, String deviceResourceId)
    {
        String agentName = validate(token, deviceResourceId);
        return (Collection<RoomSummary>) performDeviceCommand(deviceResourceId, agentName, new ListRooms());
    }

    @Override
    public Room getRoom(SecurityToken token, String deviceResourceId, String roomId)
    {
        String agentName = validateRoom(token, deviceResourceId, roomId);
        return (Room) performDeviceCommand(deviceResourceId, agentName, new GetRoom(roomId));
    }

    @Override
    public String createRoom(SecurityToken token, String deviceResourceId, Room room)
    {
        String agentName = validate(token, deviceResourceId);
        return (String) performDeviceCommand(deviceResourceId, agentName, new CreateRoom(room));
    }

    @Override
    public String modifyRoom(SecurityToken token, String deviceResourceId, Room room)
    {
        String agentName = validateRoom(token, deviceResourceId, room.getId());
        return (String) performDeviceCommand(deviceResourceId, agentName, new ModifyRoom(room));
    }

    @Override
    public void deleteRoom(SecurityToken token, String deviceResourceId, String roomId)
    {
        String agentName = validateRoom(token, deviceResourceId, roomId);
        performDeviceCommand(deviceResourceId, agentName, new DeleteRoom(roomId));
    }

    @Override
    public Collection<RoomParticipant> listRoomParticipants(SecurityToken token, String deviceResourceId, String roomId)
    {
        String agentName = validateRoom(token, deviceResourceId, roomId);
        return (List<RoomParticipant>) performDeviceCommand(deviceResourceId, agentName, new ListParticipants(roomId));
    }

    @Override
    public RoomParticipant getRoomParticipant(SecurityToken token, String deviceResourceId, String roomId,
            String roomParticipantId)
    {
        String agentName = validateRoom(token, deviceResourceId, roomId);
        return (RoomParticipant) performDeviceCommand(deviceResourceId, agentName,
                new GetParticipant(roomId, roomParticipantId));
    }

    @Override
    public Map<String, MediaData> getRoomParticipantSnapshots(SecurityToken token, String deviceResourceId,
            String roomId, Set<String> roomParticipantIds)
    {
        String agentName = validateRoom(token, deviceResourceId, roomId);
        return (Map<String, MediaData>) performDeviceCommand(deviceResourceId, agentName,
                new GetParticipantSnapshots(roomId, roomParticipantIds));
    }

    @Override
    public void modifyRoomParticipant(SecurityToken token, String deviceResourceId, RoomParticipant roomParticipant)
    {
        String agentName = validateRoom(token, deviceResourceId, roomParticipant.getRoomId());
        performDeviceCommand(deviceResourceId, agentName, new ModifyParticipant(roomParticipant));
    }

    @Override
    public String dialRoomParticipant(SecurityToken token, String deviceResourceId, String roomId, Alias alias)
    {
        String agentName = validateRoom(token, deviceResourceId, roomId);
        return (String) performDeviceCommand(deviceResourceId, agentName, new DialParticipant(roomId, alias));
    }

    @Override
    public void disconnectRoomParticipant(SecurityToken token, String deviceResourceId, String roomId,
            String roomParticipantId)
    {
        String agentName = validateRoom(token, deviceResourceId, roomId);
        performDeviceCommand(deviceResourceId, agentName, new DisconnectParticipant(roomId, roomParticipantId));
    }

    @Override
    public void muteRoomParticipant(SecurityToken token, String deviceResourceId, String roomId,
            String roomParticipantId)
    {
        String agentName = validateRoom(token, deviceResourceId, roomId);
        performDeviceCommand(deviceResourceId, agentName, new MuteParticipant(roomId, roomParticipantId));
    }

    @Override
    public void unmuteRoomParticipant(SecurityToken token, String deviceResourceId, String roomId,
            String roomParticipantId)
    {
        String agentName = validateRoom(token, deviceResourceId, roomId);
        performDeviceCommand(deviceResourceId, agentName, new UnmuteParticipant(roomId, roomParticipantId));
    }

    @Override
    public void enableRoomParticipantVideo(SecurityToken token, String deviceResourceId, String roomId,
            String roomParticipantId)
    {
        String agentName = validateRoom(token, deviceResourceId, roomId);
        performDeviceCommand(deviceResourceId, agentName, new EnableParticipantVideo(roomId, roomParticipantId));
    }

    @Override
    public void disableRoomParticipantVideo(SecurityToken token, String deviceResourceId, String roomId,
            String roomParticipantId)
    {
        String agentName = validateRoom(token, deviceResourceId, roomId);
        performDeviceCommand(deviceResourceId, agentName, new DisableParticipantVideo(roomId, roomParticipantId));
    }

    @Override
    public void setRoomParticipantMicrophoneLevel(SecurityToken token, String deviceResourceId, String roomId,
            String roomParticipantId, int level)
    {
        String agentName = validateRoom(token, deviceResourceId, roomId);
        performDeviceCommand(deviceResourceId, agentName,
                new SetParticipantMicrophoneLevel(roomId, roomParticipantId, level));
    }

    @Override
    public void setRoomParticipantPlaybackLevel(SecurityToken token, String deviceResourceId, String roomId,
            String roomParticipantId, int level)
    {
        String agentName = validateRoom(token, deviceResourceId, roomId);
        performDeviceCommand(deviceResourceId, agentName,
                new SetParticipantPlaybackLevel(roomId, roomParticipantId, level));
    }

    @Override
    public void showMessage(SecurityToken token, String deviceResourceId, int duration, String text)
    {
        String agentName = validate(token, deviceResourceId);
        performDeviceCommand(deviceResourceId, agentName, new ShowMessage(duration, text));
    }

    @Override
    public Collection<Recording> listRecordings(SecurityToken token, String deviceResourceId, String recordingFolderId)
    {
        String agentName = validate(token, deviceResourceId);
        return (Collection<Recording>) performDeviceCommand(deviceResourceId, agentName,
                new ListRecordings(recordingFolderId));
    }

    @Override
    public void deleteRecording(SecurityToken token, String deviceResourceId, String recordingId)
    {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ResourceManager resourceManager = new ResourceManager(entityManager);
        ExecutableManager executableManager = new ExecutableManager(entityManager);
        try {
            authorization.validate(token);
            ObjectIdentifier deviceResourceIdentifier = ObjectIdentifier.parse(deviceResourceId, ObjectType.RESOURCE);
            DeviceResource deviceResource = resourceManager.getDevice(deviceResourceIdentifier.getPersistenceId());
            String agentName = getAgentName(deviceResource);
            Recording recording = (Recording) performDeviceCommand(
                    deviceResourceId, agentName, new GetRecording(recordingId));
            String recordingFolderId = recording.getRecordingFolderId();
            Executable executable = executableManager.getExecutableByRecordingFolder(deviceResource, recordingFolderId);
            if (executable == null
                    || !authorization.hasObjectPermission(token, executable, ObjectPermission.WRITE)) {
                ControllerReportSetHelper.throwSecurityNotAuthorizedFault(
                        "delete recording %s in device %s", recordingId, deviceResourceIdentifier);
            }
            performDeviceCommand(deviceResourceId, agentName, new DeleteRecording(recordingId));
            recordingsCache.removeRecording(deviceResourceId, recordingFolderId, recordingId);
        }
        finally {
            entityManager.close();
        }
    }

    /**
     * Asks the local controller agent to send a command to be performed by a device.
     *
     * @param agentName on which the command should be performed
     * @param command    command to be performed by the device
     */
    private Object performDeviceCommand(String deviceResourceId, String agentName, ConnectorCommand command)
    {
        SendLocalCommand sendLocalCommand = controllerAgent.sendCommand(agentName, command);
        if (sendLocalCommand.getState() == SendLocalCommand.State.SUCCESSFUL) {
            return sendLocalCommand.getResult();
        }
        throw new ControllerReportSet.DeviceCommandFailedException(
                deviceResourceId, command.toString(), sendLocalCommand.getJadeReport());
    }

    /**
     * @param securityToken    to be validated against given {@code deviceResourceId}
     * @param deviceResourceId
     * @return agent name
     */
    private String validate(SecurityToken securityToken, String deviceResourceId)
    {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ResourceManager resourceManager = new ResourceManager(entityManager);
        try {
            authorization.validate(securityToken);
            ObjectIdentifier deviceResourceIdentifier = ObjectIdentifier.parse(deviceResourceId, ObjectType.RESOURCE);
            DeviceResource deviceResource = resourceManager.getDevice(deviceResourceIdentifier.getPersistenceId());
            String agentName = getAgentName(deviceResource);
            if (!authorization.hasObjectPermission(securityToken, deviceResource, ObjectPermission.CONTROL_RESOURCE)) {
                ControllerReportSetHelper.throwSecurityNotAuthorizedFault(
                        "control device %s", deviceResourceIdentifier);
            }
            return agentName;
        }
        finally {
            entityManager.close();
        }
    }

    /**
     * @param securityToken    to be validated against given {@code deviceResourceId}
     * @param deviceResourceId
     * @return agent name
     */
    private String validateRoom(SecurityToken securityToken, String deviceResourceId, String roomId)
    {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ResourceManager resourceManager = new ResourceManager(entityManager);
        try {
            authorization.validate(securityToken);
            ObjectIdentifier deviceResourceIdentifier = ObjectIdentifier.parse(deviceResourceId, ObjectType.RESOURCE);
            DeviceResource deviceResource = resourceManager.getDevice(deviceResourceIdentifier.getPersistenceId());
            String agentName = getAgentName(deviceResource);
            if (!authorization.hasObjectPermission(securityToken, deviceResource, ObjectPermission.CONTROL_RESOURCE)) {
                ExecutableManager executableManager = new ExecutableManager(entityManager);
                RoomEndpoint roomEndpoint = executableManager.getRoomEndpoint(
                        deviceResourceIdentifier.getPersistenceId(), roomId);
                if (roomEndpoint == null
                        || !authorization.hasObjectPermission(securityToken, roomEndpoint, ObjectPermission.READ)) {
                    ControllerReportSetHelper.throwSecurityNotAuthorizedFault(
                            "control device %s", deviceResourceIdentifier);
                }
            }
            return agentName;
        }
        finally {
            entityManager.close();
        }
    }

    /**
     * Gets name of agent managing a given device.
     *
     * @param deviceResource of which the agent name should be get
     * @return agent name of managed resource with given {@code deviceResourceId}
     */
    protected String getAgentName(DeviceResource deviceResource)
    {

        Mode mode = deviceResource.getMode();
        if (mode instanceof ManagedMode) {
            ManagedMode managedMode = (ManagedMode) mode;
            return managedMode.getConnectorAgentName();
        }
        throw new RuntimeException(String.format("Resource '%s' is not managed!",
                ObjectIdentifier.formatId(deviceResource)));

    }
}
