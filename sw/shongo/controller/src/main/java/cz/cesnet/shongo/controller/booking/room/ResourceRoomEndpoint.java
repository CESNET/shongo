package cz.cesnet.shongo.controller.booking.room;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.Room;
import cz.cesnet.shongo.connector.api.jade.multipoint.rooms.CreateRoom;
import cz.cesnet.shongo.connector.api.jade.multipoint.rooms.DeleteRoom;
import cz.cesnet.shongo.connector.api.jade.multipoint.rooms.ModifyRoom;
import cz.cesnet.shongo.controller.ControllerAgent;
import cz.cesnet.shongo.controller.Reporter;
import cz.cesnet.shongo.controller.api.RoomExecutable;
import cz.cesnet.shongo.controller.booking.EntityIdentifier;
import cz.cesnet.shongo.controller.booking.alias.Alias;
import cz.cesnet.shongo.controller.booking.executable.ExecutableManager;
import cz.cesnet.shongo.controller.booking.executable.ManagedEndpoint;
import cz.cesnet.shongo.controller.booking.resource.*;
import cz.cesnet.shongo.controller.booking.room.settting.RoomSetting;
import cz.cesnet.shongo.controller.executor.ExecutionReportSet;
import cz.cesnet.shongo.controller.executor.Executor;
import cz.cesnet.shongo.controller.scheduler.SchedulerException;
import cz.cesnet.shongo.jade.SendLocalCommand;
import cz.cesnet.shongo.report.Report;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToOne;
import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Represents a {@link DeviceResource} which acts as {@link RoomEndpoint} in a {@link cz.cesnet.shongo.controller.booking.compartment.Compartment}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class ResourceRoomEndpoint extends RoomEndpoint implements ManagedEndpoint, Reporter.ResourceContext
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
        roomExecutableEndpointApi.setResourceId(EntityIdentifier.formatId(getResource()));
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

    @Override
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

    @Override
    protected State onUpdate(Executor executor, ExecutableManager executableManager)
    {
        UsedRoomEndpoint usedRoomEndpoint = executableManager.getStartedUsedRoomEndpoint(this);
        if (usedRoomEndpoint != null && State.MODIFIED.equals(usedRoomEndpoint.getState())) {
            // Used room will be automatically modified
            return State.SKIPPED;
        }
        try {
            modifyRoom(getRoomApi(executableManager), executor);
            return State.STARTED;
        }
        catch (ExecutionReportSet.RoomNotStartedException exception) {
            executableManager.createExecutionReport(this, exception.getReport());
        }
        catch (ExecutionReportSet.CommandFailedException exception) {
            executableManager.createExecutionReport(this, exception.getReport());
        }
        return null;
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
    public void loadLazyProperties()
    {
        getResource();
        super.loadLazyProperties();
    }
}
