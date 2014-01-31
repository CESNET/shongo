package cz.cesnet.shongo.controller.booking.room;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.ParticipantRole;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.RecordingFolder;
import cz.cesnet.shongo.api.Room;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.connector.api.jade.multipoint.rooms.CreateRoom;
import cz.cesnet.shongo.connector.api.jade.multipoint.rooms.DeleteRoom;
import cz.cesnet.shongo.connector.api.jade.multipoint.rooms.ModifyRoom;
import cz.cesnet.shongo.connector.api.jade.recording.DeleteRecordingFolder;
import cz.cesnet.shongo.connector.api.jade.recording.ModifyRecordingFolder;
import cz.cesnet.shongo.controller.*;
import cz.cesnet.shongo.controller.api.RoomExecutable;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.alias.Alias;
import cz.cesnet.shongo.controller.booking.executable.ExecutableManager;
import cz.cesnet.shongo.controller.booking.executable.ManagedEndpoint;
import cz.cesnet.shongo.controller.booking.recording.RecordableEndpoint;
import cz.cesnet.shongo.controller.booking.recording.RecordingCapability;
import cz.cesnet.shongo.controller.booking.resource.*;
import cz.cesnet.shongo.controller.booking.room.settting.RoomSetting;
import cz.cesnet.shongo.controller.executor.ExecutionReportSet;
import cz.cesnet.shongo.controller.executor.Executor;
import cz.cesnet.shongo.controller.scheduler.SchedulerException;
import cz.cesnet.shongo.jade.SendLocalCommand;
import cz.cesnet.shongo.report.Report;

import javax.persistence.*;
import java.util.*;

/**
 * Represents a {@link DeviceResource} which acts as {@link RoomEndpoint}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class ResourceRoomEndpoint extends RoomEndpoint
        implements ManagedEndpoint, RecordableEndpoint, Reporter.ResourceContext
{
    /**
     * {@link DeviceResource}.
     */
    private RoomProviderCapability roomProviderCapability;

    /**
     * {@link cz.cesnet.shongo.Technology} specific id of the {@link RoomConfiguration}.
     */
    private String roomId;

    /**
     * @see RecordableEndpoint#getRecordingFolderId
     */
    private Map<RecordingCapability, String> recordingFolderIds = new HashMap<RecordingCapability, String>();

    /**
     * Constructor.
     */
    public ResourceRoomEndpoint()
    {
    }

    /**
     * @return {@link #roomProviderCapability}
     */
    @OneToOne
    public RoomProviderCapability getRoomProviderCapability()
    {
        return roomProviderCapability;
    }

    /**
     * @param roomProviderCapability sets the {@link #roomProviderCapability}
     */
    public void setRoomProviderCapability(RoomProviderCapability roomProviderCapability)
    {
        this.roomProviderCapability = roomProviderCapability;
    }

    /**
     * @return {@link DeviceResource} for the {@link #roomProviderCapability}
     */
    @Transient
    @Override
    public DeviceResource getResource()
    {
        return roomProviderCapability.getDeviceResource();
    }

    /**
     * @return {@link #roomId}
     */
    @Override
    @Column
    @org.hibernate.annotations.Index(name = "room_id")
    public String getRoomId()
    {
        return roomId;
    }

    /**
     * @param roomId sets the {@link #roomId}
     */
    public void setRoomId(String roomId)
    {
        this.roomId = roomId;
    }

    @ElementCollection
    @Column(name = "recording_folder_id")
    @MapKeyJoinColumn(name = "recording_capability_id")
    @Access(AccessType.FIELD)
    @Override
    public Map<RecordingCapability, String> getRecordingFolderIds()
    {
        return Collections.unmodifiableMap(recordingFolderIds);
    }

    @Transient
    @Override
    public String getRecordingFolderId(RecordingCapability recordingCapability)
    {
        return recordingFolderIds.get(recordingCapability);
    }

    @Transient
    @Override
    public void putRecordingFolderId(RecordingCapability recordingCapability, String recordingFolderId)
    {
        this.recordingFolderIds.put(recordingCapability, recordingFolderId);
    }

    @Override
    protected cz.cesnet.shongo.controller.api.Executable createApi()
    {
        return new RoomExecutable();
    }

    @Override
    public RoomExecutable toApi(Report.UserType userType)
    {
        return (RoomExecutable) super.toApi(userType);
    }

    @Override
    public void toApi(cz.cesnet.shongo.controller.api.Executable executableApi, Report.UserType userType)
    {
        super.toApi(executableApi, userType);

        RoomExecutable roomExecutableEndpointApi =
                (RoomExecutable) executableApi;
        roomExecutableEndpointApi.setLicenseCount(getLicenseCount());
        roomExecutableEndpointApi.setResourceId(ObjectIdentifier.formatId(getResource()));
        roomExecutableEndpointApi.setRoomId(getRoomId());
        for (Technology technology : getTechnologies()) {
            roomExecutableEndpointApi.addTechnology(technology);
        }
        for (Alias alias : getAssignedAliases()) {
            roomExecutableEndpointApi.addAlias(alias.toApi());
        }
        for (RoomSetting roomSetting : getRoomSettings()) {
            roomExecutableEndpointApi.addRoomSetting(roomSetting.toApi());
        }
    }

    /**
     * @return {@link RoomConfiguration#licenseCount}
     */
    @Transient
    public int getLicenseCount()
    {
        RoomConfiguration roomConfiguration = getRoomConfiguration();
        if (roomConfiguration == null) {
            throw new IllegalStateException("Room configuration hasn't been set yet.");
        }
        return roomConfiguration.getLicenseCount();
    }

    /**
     * @return {@link RoomConfiguration#roomSettings} or empty collection if {@link #roomConfiguration} is null
     */
    @Transient
    private Collection<RoomSetting> getRoomSettings()
    {
        RoomConfiguration roomConfiguration = getRoomConfiguration();
        if (roomConfiguration == null) {
            throw new IllegalStateException("Room configuration hasn't been set yet.");
        }
        return roomConfiguration.getRoomSettings();
    }

    @Override
    @Transient
    public boolean isStandalone()
    {
        return getResource().isStandaloneTerminal();
    }

    @Override
    @Transient
    public List<Alias> getAliases()
    {
        List<Alias> aliases = new ArrayList<Alias>();
        TerminalCapability terminalCapability = getResource().getCapability(TerminalCapability.class);
        if (terminalCapability != null) {
            aliases.addAll(terminalCapability.getAliases());
        }
        aliases.addAll(super.getAssignedAliases());
        return aliases;
    }

    @Override
    public void addAssignedAlias(Alias assignedAlias) throws SchedulerException
    {
        getResource().evaluateAlias(assignedAlias);
        super.addAssignedAlias(assignedAlias);
    }

    @Override
    @Transient
    public Address getAddress()
    {
        return getResource().getAddress();
    }

    @Override
    @Transient
    public String getDescription()
    {
        return String.format("room in %s", getResource().getReportDescription());
    }

    @Override
    @Transient
    public String getConnectorAgentName()
    {
        Mode mode = getResource().getMode();
        if (mode instanceof ManagedMode) {
            ManagedMode managedMode = (ManagedMode) mode;
            return managedMode.getConnectorAgentName();
        }
        else {
            throw new IllegalStateException("Resource " + getDescription() + " is not managed!");
        }
    }

    @Transient
    @Override
    public Alias getRecordingAlias()
    {
        Alias callableAlias = null;
        for (Alias alias : getAliases()) {
            if (alias.isCallable()) {
                callableAlias = alias;
                break;
            }
        }
        if (callableAlias == null) {
            throw new RuntimeException("No callable alias exists for '" + ObjectIdentifier.formatId(this) + ".");
        }
        return callableAlias;
    }

    @Transient
    @Override
    public RecordingFolder getRecordingFolderApi()
    {
        RecordingFolder recordingFolder = new RecordingFolder();
        recordingFolder.setName(String.format("exe:%d:res:%d:room:%s", getId(), getResource().getId(), getRoomId()));
        recordingFolder.setUserPermissions(getRecordingFolderUserPermissions());
        return recordingFolder;
    }

    @Override
    @Transient
    public Room getRoomApi(ExecutableManager executableManager)
    {
        UsedRoomEndpoint usedRoomEndpoint = executableManager.getStartedUsedRoomEndpoint(this);
        if (usedRoomEndpoint != null) {
            return usedRoomEndpoint.getRoomApi(executableManager);
        }
        else {
            return super.getRoomApi(executableManager);
        }
    }

    @Override
    public void fillRoomApi(Room roomApi, ExecutableManager executableManager)
    {
        super.fillRoomApi(roomApi, executableManager);

        roomApi.setId(roomId);
        roomApi.setTechnologies(getTechnologies());
        roomApi.setLicenseCount(getLicenseCount() + getEndpointServiceCount());
        for (RoomSetting roomSetting : getRoomSettings()) {
            roomApi.addRoomSetting(roomSetting.toApi());
        }
        for (Alias alias : getAliases()) {
            roomApi.addAlias(alias.toApi());
        }
    }

    @Override
    public void modifyRoom(Room roomApi, Executor executor)
            throws ExecutionReportSet.RoomNotStartedException, ExecutionReportSet.CommandFailedException
    {
        if (roomApi.getId() == null) {
            cz.cesnet.shongo.api.Alias alias = roomApi.getAlias(AliasType.ROOM_NAME);
            throw new ExecutionReportSet.RoomNotStartedException(alias != null ? alias.getValue() : null);
        }

        DeviceResource deviceResource = getResource();
        ManagedMode managedMode = deviceResource.requireManaged();
        String agentName = managedMode.getConnectorAgentName();
        ControllerAgent controllerAgent = executor.getControllerAgent();

        // TODO: Retrieve current room state and only apply changes

        SendLocalCommand sendLocalCommand = controllerAgent.sendCommand(agentName, new ModifyRoom(roomApi));
        if (sendLocalCommand.getState() == SendLocalCommand.State.SUCCESSFUL) {
            setRoomId((String) sendLocalCommand.getResult());
        }
        else {
            throw new ExecutionReportSet.CommandFailedException(
                    sendLocalCommand.getName(), sendLocalCommand.getJadeReport());
        }
    }

    @Override
    protected State onStart(Executor executor, ExecutableManager executableManager)
    {
        State state = super.onStart(executor, executableManager);
        if (!state.equals(State.STARTED)) {
            return state;
        }

        DeviceResource deviceResource = getResource();
        ManagedMode managedMode = deviceResource.requireManaged();
        String agentName = managedMode.getConnectorAgentName();
        ControllerAgent controllerAgent = executor.getControllerAgent();

        Room roomApi = getRoomApi(executableManager);
        SendLocalCommand sendLocalCommand = controllerAgent.sendCommand(agentName, new CreateRoom(roomApi));
        if (sendLocalCommand.getState() == SendLocalCommand.State.SUCCESSFUL) {
            setRoomId((String) sendLocalCommand.getResult());
            return State.STARTED;
        }
        else {
            executableManager.createExecutionReport(this, new ExecutionReportSet.CommandFailedReport(
                    sendLocalCommand.getName(), sendLocalCommand.getJadeReport()));
            return State.STARTING_FAILED;
        }
    }

    @Transient
    private Map<String, RecordingFolder.UserPermission> getRecordingFolderUserPermissions()
    {
        Authorization authorization = Authorization.getInstance();
        Map<String, RecordingFolder.UserPermission> userPermissions =
                new HashMap<String, RecordingFolder.UserPermission>();
        for (UserInformation user : authorization.getUsersWithRole(this, ObjectRole.READER)) {
            userPermissions.put(user.getUserId(), RecordingFolder.UserPermission.READ);
        }
        for (UserInformation user : authorization.getUsersWithRole(this, ObjectRole.OWNER)) {
            userPermissions.put(user.getUserId(), RecordingFolder.UserPermission.WRITE);
        }
        return userPermissions;
    }

    @Override
    protected Boolean onUpdate(Executor executor, ExecutableManager executableManager)
    {
        // Update user permissions to recording folders
        if (recordingFolderIds.size() > 0) {
            Map<String, RecordingFolder.UserPermission> userPermissions = getRecordingFolderUserPermissions();
            for (Map.Entry<RecordingCapability, String> entry : recordingFolderIds.entrySet()) {
                DeviceResource deviceResource = entry.getKey().getDeviceResource();
                ManagedMode managedMode = deviceResource.requireManaged();
                String agentName = managedMode.getConnectorAgentName();
                ControllerAgent controllerAgent = executor.getControllerAgent();
                SendLocalCommand sendLocalCommand = controllerAgent.sendCommand(agentName,
                        new ModifyRecordingFolder(entry.getValue(), userPermissions));
                if (!SendLocalCommand.State.SUCCESSFUL.equals(sendLocalCommand.getState())) {
                    executableManager.createExecutionReport(this, new ExecutionReportSet.CommandFailedReport(
                            sendLocalCommand.getName(), sendLocalCommand.getJadeReport()));
                    return Boolean.FALSE;
                }
            }
        }

        // Update started room
        if (state.equals(State.STARTED)) {
            UsedRoomEndpoint usedRoomEndpoint = executableManager.getStartedUsedRoomEndpoint(this);
            if (usedRoomEndpoint != null && usedRoomEndpoint.isModified()) {
                // Room will be updated by UsedRoomEndpoint and thus skip this updating
                return null;
            }
            try {
                modifyRoom(getRoomApi(executableManager), executor);
                return Boolean.TRUE;
            }
            catch (ExecutionReportSet.RoomNotStartedException exception) {
                executableManager.createExecutionReport(this, exception.getReport());
            }
            catch (ExecutionReportSet.CommandFailedException exception) {
                executableManager.createExecutionReport(this, exception.getReport());
            }
            return Boolean.FALSE;
        }
        else {
            return null;
        }
    }

    @Override
    protected State onStop(Executor executor, ExecutableManager executableManager)
    {
        DeviceResource deviceResource = getResource();
        ManagedMode managedMode = deviceResource.requireManaged();
        String agentName = managedMode.getConnectorAgentName();
        ControllerAgent controllerAgent = executor.getControllerAgent();
        String roomId = getRoomId();
        if (roomId == null) {
            throw new RuntimeException("Cannot delete virtual room because it's identifier is null.");
        }
        SendLocalCommand sendLocalCommand = controllerAgent.sendCommand(agentName, new DeleteRoom(roomId));
        if (sendLocalCommand.getState() == SendLocalCommand.State.SUCCESSFUL) {
            return State.STOPPED;
        }
        else {
            executableManager.createExecutionReport(this, new ExecutionReportSet.CommandFailedReport(
                    sendLocalCommand.getName(), sendLocalCommand.getJadeReport()));
            return State.STOPPING_FAILED;
        }
    }

    @Override
    protected State onFinalize(Executor executor, ExecutableManager executableManager)
    {
        State state = State.FINALIZED;
        Iterator<Map.Entry<RecordingCapability, String>> iterator = recordingFolderIds.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<RecordingCapability, String> entry = iterator.next();
            DeviceResource deviceResource = entry.getKey().getDeviceResource();
            ManagedMode managedMode = deviceResource.requireManaged();
            String agentName = managedMode.getConnectorAgentName();
            ControllerAgent controllerAgent = executor.getControllerAgent();
            SendLocalCommand sendLocalCommand = controllerAgent.sendCommand(agentName,
                    new DeleteRecordingFolder(entry.getValue()));
            if (SendLocalCommand.State.SUCCESSFUL.equals(sendLocalCommand.getState())) {
                iterator.remove();
            }
            else {
                executableManager.createExecutionReport(this, new ExecutionReportSet.CommandFailedReport(
                        sendLocalCommand.getName(), sendLocalCommand.getJadeReport()));
                state = State.FINALIZATION_FAILED;
            }
        }
        return state;
    }

    @Override
    public void loadLazyProperties()
    {
        getResource();
        super.loadLazyProperties();
    }
}
