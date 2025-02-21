    package cz.cesnet.shongo.controller.booking.room;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.RecordingFolder;
import cz.cesnet.shongo.api.Room;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.participant.AbstractParticipant;
import cz.cesnet.shongo.controller.booking.recording.RecordableEndpoint;
import cz.cesnet.shongo.controller.booking.recording.RecordingCapability;
import cz.cesnet.shongo.controller.booking.resource.DeviceResource;
import cz.cesnet.shongo.controller.executor.Executor;
import cz.cesnet.shongo.controller.Reporter;
import cz.cesnet.shongo.controller.api.UsedRoomExecutable;
import cz.cesnet.shongo.controller.booking.room.settting.RoomSetting;
import cz.cesnet.shongo.controller.booking.executable.Executable;
import cz.cesnet.shongo.controller.booking.executable.ExecutableManager;
import cz.cesnet.shongo.controller.booking.executable.ManagedEndpoint;
import cz.cesnet.shongo.controller.executor.*;
import cz.cesnet.shongo.controller.booking.resource.Address;
import cz.cesnet.shongo.controller.booking.alias.Alias;
import cz.cesnet.shongo.controller.scheduler.SchedulerException;
import cz.cesnet.shongo.report.Report;

import jakarta.persistence.*;
import java.util.*;

    /**
 * Represents a re-used {@link RoomEndpoint} for different
 * {@link RoomConfiguration}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class UsedRoomEndpoint extends RoomEndpoint
            implements ManagedEndpoint, RecordableEndpoint, Reporter.ResourceContext
{
    /**
     * {@link RoomEndpoint} which is re-used.
     */
    private RoomEndpoint reusedRoomEndpoint;

    /**
     * Specifies whether {@link #onStop} is active.
     */
    private boolean isStopping;

    /**
     * Constructor.
     */
    public UsedRoomEndpoint()
    {
    }

    /**
     * @return {@link #reusedRoomEndpoint}
     */
    @ManyToOne(optional = false)
    @Access(AccessType.FIELD)
    public RoomEndpoint getReusedRoomEndpoint()
    {
        return reusedRoomEndpoint;
    }

    /**
     * @param roomEndpoint sets the {@link #reusedRoomEndpoint}
     */
    public void setReusedRoomEndpoint(RoomEndpoint roomEndpoint)
    {
        this.reusedRoomEndpoint = roomEndpoint;
    }

    /**
     * @return merged {@link RoomConfiguration} of {@link #roomConfiguration} and {@link #reusedRoomEndpoint#roomConfiguration}
     */
    @Override
    @Transient
    public RoomConfiguration getRoomConfiguration()
    {
        RoomConfiguration roomConfiguration = super.getRoomConfiguration();
        RoomConfiguration roomEndpointConfiguration = reusedRoomEndpoint.getRoomConfiguration();
        RoomConfiguration mergedRoomConfiguration = new RoomConfiguration();
        mergedRoomConfiguration.setLicenseCount(
                roomConfiguration.getLicenseCount() + roomEndpointConfiguration.getLicenseCount());
        mergedRoomConfiguration.setTechnologies(roomConfiguration.getTechnologies());
        for (RoomSetting roomSetting : roomEndpointConfiguration.getRoomSettings()) {
            mergedRoomConfiguration.addRoomSetting(roomSetting);
        }
        for (RoomSetting roomSetting : roomConfiguration.getRoomSettings()) {
            mergedRoomConfiguration.addRoomSetting(roomSetting);
        }
        return mergedRoomConfiguration;
    }

    @Override
    @Transient
    public List<AbstractParticipant> getParticipants()
    {
        List<AbstractParticipant> reusedParticipants = reusedRoomEndpoint.getParticipants();
        if (reusedParticipants.isEmpty()) {
            return super.getParticipants();
        }
        else {
            List<AbstractParticipant> participants = new LinkedList<AbstractParticipant>();
            participants.addAll(reusedParticipants);
            participants.addAll(super.getParticipants());
            return participants;
        }
    }

    @Override
    @Transient
    public Collection<Executable> getExecutionDependencies()
    {
        List<Executable> dependencies = new ArrayList<Executable>();
        dependencies.add(reusedRoomEndpoint);
        return dependencies;
    }

    @Override
    protected cz.cesnet.shongo.controller.api.Executable createApi()
    {
        return new UsedRoomExecutable();
    }

    @Override
    public void toApi(cz.cesnet.shongo.controller.api.Executable executableApi, EntityManager entityManager,
            Report.UserType userType)
    {
        super.toApi(executableApi, entityManager, userType);

        UsedRoomExecutable usedRoomExecutableEndpointApi =
                (UsedRoomExecutable) executableApi;

        usedRoomExecutableEndpointApi.setReusedRoomExecutableId(ObjectIdentifier.formatId(reusedRoomEndpoint));

        RoomConfiguration roomConfiguration = getRoomConfiguration();
        usedRoomExecutableEndpointApi.setLicenseCount(roomConfiguration.getLicenseCount());
        for (Technology technology : roomConfiguration.getTechnologies()) {
            usedRoomExecutableEndpointApi.addTechnology(technology);
        }
        for (Alias alias : getAliases()) {
            usedRoomExecutableEndpointApi.addAlias(alias.toApi());
        }
        for (RoomSetting roomSetting : roomConfiguration.getRoomSettings()) {
            usedRoomExecutableEndpointApi.addRoomSetting(roomSetting.toApi());
        }
    }

    @Override
    public boolean canBeModified()
    {
        return state.equals(State.STARTED);
    }

    @Transient
    @Override
    public int getEndpointServiceCount()
    {
        return super.getEndpointServiceCount() + reusedRoomEndpoint.getEndpointServiceCount();
    }

    @Transient
    @Override
    public DeviceResource getResource()
    {
        return reusedRoomEndpoint.getResource();
    }

    @Override
    @Transient
    public String getRoomId()
    {
        return reusedRoomEndpoint.getRoomId();
    }

    @Override
    @Transient
    public boolean isStandalone()
    {
        return reusedRoomEndpoint.isStandalone();
    }

    @Override
    @Transient
    public List<Alias> getAliases()
    {
        List<Alias> aliases = new ArrayList<Alias>();
        aliases.addAll(reusedRoomEndpoint.getAliases());
        aliases.addAll(super.getAssignedAliases());
        return aliases;
    }

    @Override
    public void addAssignedAlias(Alias assignedAlias) throws SchedulerException
    {
        super.addAssignedAlias(assignedAlias);
    }

    @Override
    @Transient
    public Address getAddress()
    {
        return reusedRoomEndpoint.getAddress();
    }

    @Override
    @Transient
    public String getReportDescription()
    {
        return reusedRoomEndpoint.getReportDescription();
    }

    @Override
    @Transient
    public String getConnectorAgentName()
    {
        if (reusedRoomEndpoint instanceof ManagedEndpoint) {
            ManagedEndpoint managedEndpoint = (ManagedEndpoint) reusedRoomEndpoint;
            return managedEndpoint.getConnectorAgentName();
        }
        else {
            throw new TodoImplementException(reusedRoomEndpoint.getClass());
        }
    }

    @Override
    @Transient
    public Alias getRecordingAlias()
    {
        if (reusedRoomEndpoint instanceof RecordableEndpoint) {
            RecordableEndpoint recordableEndpoint = (RecordableEndpoint) reusedRoomEndpoint;
            return recordableEndpoint.getRecordingAlias();
        }
        else {
            throw new TodoImplementException(reusedRoomEndpoint.getClass());
        }
    }

    @Transient
    @Override
    public String getRecordingPrefixName()
    {
        if (reusedRoomEndpoint instanceof RecordableEndpoint) {
            RecordableEndpoint recordableEndpoint = (RecordableEndpoint) reusedRoomEndpoint;
            return recordableEndpoint.getRecordingPrefixName();
        }
        else {
            throw new TodoImplementException(reusedRoomEndpoint.getClass());
        }
    }

    @Override
    @Transient
    public RecordingFolder getRecordingFolderApi()
    {
        if (reusedRoomEndpoint instanceof RecordableEndpoint) {
            RecordableEndpoint recordableEndpoint = (RecordableEndpoint) reusedRoomEndpoint;
            return recordableEndpoint.getRecordingFolderApi();
        }
        else {
            throw new TodoImplementException(reusedRoomEndpoint.getClass());
        }
    }

    @Override
    @Transient
    public Map<RecordingCapability, String> getRecordingFolderIds()
    {
        if (reusedRoomEndpoint instanceof RecordableEndpoint) {
            RecordableEndpoint recordableEndpoint = (RecordableEndpoint) reusedRoomEndpoint;
            return recordableEndpoint.getRecordingFolderIds();
        }
        else {
            throw new TodoImplementException(reusedRoomEndpoint.getClass());
        }
    }

    @Override
    @Transient
    public String getRecordingFolderId(RecordingCapability recordingCapability)
    {
        if (reusedRoomEndpoint instanceof RecordableEndpoint) {
            RecordableEndpoint recordableEndpoint = (RecordableEndpoint) reusedRoomEndpoint;
            return recordableEndpoint.getRecordingFolderId(recordingCapability);
        }
        else {
            throw new TodoImplementException(reusedRoomEndpoint.getClass());
        }
    }

    @Override
    @Transient
    public void putRecordingFolderId(RecordingCapability recordingCapability, String recordingFolderId)
    {
        if (reusedRoomEndpoint instanceof RecordableEndpoint) {
            RecordableEndpoint recordableEndpoint = (RecordableEndpoint) reusedRoomEndpoint;
            recordableEndpoint.putRecordingFolderId(recordingCapability, recordingFolderId);
        }
        else {
            throw new TodoImplementException(reusedRoomEndpoint.getClass());
        }
    }

    @Override
    @Transient
    public void removeRecordingFolderId(RecordingCapability recordingCapability)
    {
        if (reusedRoomEndpoint instanceof RecordableEndpoint) {
            RecordableEndpoint recordableEndpoint = (RecordableEndpoint) reusedRoomEndpoint;
            recordableEndpoint.removeRecordingFolderId(recordingCapability);
        }
        else {
            throw new TodoImplementException(reusedRoomEndpoint.getClass());
        }
    }

    @Override
    public void fillRoomApi(Room roomApi, ExecutableManager executableManager)
    {
        super.fillRoomApi(roomApi, executableManager);

        // Use reused room configuration
        reusedRoomEndpoint.fillRoomApi(roomApi, executableManager);

        // Modify the room configuration (only when we aren't stopping the reused room)
        if (!isStopping) {
            RoomConfiguration roomConfiguration = getRoomConfiguration();
            roomApi.setDescription(getRoomDescriptionApi());
            roomApi.setLicenseCount(roomConfiguration.getLicenseCount() + getEndpointServiceCount());
            for (RoomSetting roomSetting : roomConfiguration.getRoomSettings()) {
                roomApi.addRoomSetting(roomSetting.toApi());
            }
            for (Alias alias : getAssignedAliases()) {
                roomApi.addAlias(alias.toApi());
            }
        }
    }

    @Override
    public void modifyRoom(Room roomApi, Executor executor)
            throws ExecutionReportSet.RoomNotStartedException, ExecutionReportSet.CommandFailedException
    {
        reusedRoomEndpoint.modifyRoom(roomApi, executor);
    }

    @Override
    protected Executable.State onStart(Executor executor, ExecutableManager executableManager)
    {
        State state = super.onStart(executor, executableManager);
        if (!state.equals(State.STARTED)) {
            return state;
        }
        try {
            modifyRoom(getRoomApi(executableManager), executor);
            return Executable.State.STARTED;
        }
        catch (ExecutionReportSet.RoomNotStartedException exception) {
            executableManager.createExecutionReport(this, exception.getReport());
        }
        catch (ExecutionReportSet.CommandFailedException exception) {
            executableManager.createExecutionReport(this, exception.getReport());
        }
        return Executable.State.STARTING_FAILED;
    }

    @Override
    protected Boolean onUpdate(Executor executor, ExecutableManager executableManager)
    {
        if (state.equals(State.STARTED)) {
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
    protected Executable.State onStop(Executor executor, ExecutableManager executableManager)
    {
        isStopping = true;
        try {
            modifyRoom(getRoomApi(executableManager), executor);
            return Executable.State.STOPPED;
        }
        catch (ExecutionReportSet.RoomNotStartedException exception) {
            executableManager.createExecutionReport(this, exception.getReport());
        }
        catch (ExecutionReportSet.CommandFailedException exception) {
            executableManager.createExecutionReport(this, exception.getReport());
        }
        finally {
            isStopping = false;
        }
        return Executable.State.STOPPING_FAILED;
    }

    @Override
    public String toString()
    {
        return String.format(UsedRoomEndpoint.class.getSimpleName() + " (id: %d, reusedId: %d)",
                id, reusedRoomEndpoint.getId());
    }
}
