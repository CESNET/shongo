package cz.cesnet.shongo.controller.booking.room;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.Room;
import cz.cesnet.shongo.connector.api.jade.multipoint.rooms.CreateRoom;
import cz.cesnet.shongo.connector.api.jade.multipoint.rooms.DeleteRoom;
import cz.cesnet.shongo.connector.api.jade.multipoint.rooms.ModifyRoom;
import cz.cesnet.shongo.controller.ControllerAgent;
import cz.cesnet.shongo.controller.executor.Executor;
import cz.cesnet.shongo.controller.Reporter;
import cz.cesnet.shongo.controller.api.RoomExecutable;
import cz.cesnet.shongo.controller.booking.alias.Alias;
import cz.cesnet.shongo.controller.booking.resource.TerminalCapability;
import cz.cesnet.shongo.controller.booking.EntityIdentifier;
import cz.cesnet.shongo.controller.booking.room.settting.RoomSetting;
import cz.cesnet.shongo.controller.booking.executable.ExecutableManager;
import cz.cesnet.shongo.controller.executor.ExecutorReportSet;
import cz.cesnet.shongo.controller.booking.executable.ManagedEndpoint;
import cz.cesnet.shongo.controller.booking.resource.*;
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
    public DeviceResource getDeviceResource()
    {
        return roomProviderCapability.getDeviceResource();
    }

    /**
     * @return {@link #roomId} isProvidable
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
        roomExecutableEndpointApi.setResourceId(EntityIdentifier.formatId(getDeviceResource()));
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
     * @return {@link RoomConfiguration#licenseCount} or 0 if {@link #roomConfiguration} is null
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
        return getDeviceResource().isStandaloneTerminal();
    }

    @Override
    @Transient
    public List<Alias> getAliases()
    {
        List<Alias> aliases = new ArrayList<Alias>();
        TerminalCapability terminalCapability = getDeviceResource().getCapability(TerminalCapability.class);
        if (terminalCapability != null) {
            aliases.addAll(terminalCapability.getAliases());
        }
        aliases.addAll(super.getAssignedAliases());
        return aliases;
    }

    @Override
    public void addAssignedAlias(Alias assignedAlias) throws SchedulerException
    {
        getDeviceResource().evaluateAlias(assignedAlias);
        super.addAssignedAlias(assignedAlias);
    }

    @Override
    @Transient
    public Address getAddress()
    {
        return getDeviceResource().getAddress();
    }

    @Override
    @Transient
    public String getDescription()
    {
        return String.format("room in %s", getDeviceResource().getReportDescription());
    }

    @Override
    @Transient
    public String getConnectorAgentName()
    {
        Mode mode = getDeviceResource().getMode();
        if (mode instanceof ManagedMode) {
            ManagedMode managedMode = (ManagedMode) mode;
            return managedMode.getConnectorAgentName();
        }
        else {
            throw new IllegalStateException("Resource " + getDescription() + " is not managed!");
        }
    }

    @Override
    @Transient
    public Resource getResource()
    {
        return getDeviceResource();
    }

    @Override
    public void fillRoomApi(Room roomApi)
    {
        super.fillRoomApi(roomApi);

        roomApi.setId(roomId);
        roomApi.setTechnologies(getTechnologies());
        roomApi.setLicenseCount(getLicenseCount());
        for (RoomSetting roomSetting : getRoomSettings()) {
            roomApi.addRoomSetting(roomSetting.toApi());
        }
        for (Alias alias : getAliases()) {
            roomApi.addAlias(alias.toApi());
        }
    }

    @Override
    public void modifyRoom(Room roomApi, Executor executor, ExecutableManager executableManager)
            throws ExecutorReportSet.RoomNotStartedException, ExecutorReportSet.CommandFailedException
    {
        if (roomApi.getId() == null) {
            cz.cesnet.shongo.api.Alias alias = roomApi.getAlias(AliasType.ROOM_NAME);
            throw new ExecutorReportSet.RoomNotStartedException(alias != null ? alias.getValue() : null);
        }

        DeviceResource deviceResource = getDeviceResource();
        if (deviceResource.isManaged()) {
            ManagedMode managedMode = (ManagedMode) deviceResource.getMode();
            String agentName = managedMode.getConnectorAgentName();
            ControllerAgent controllerAgent = executor.getControllerAgent();

            // TODO: Retrieve current room state and only apply changes

            SendLocalCommand sendLocalCommand = controllerAgent.sendCommand(agentName, new ModifyRoom(roomApi));
            if (sendLocalCommand.getState() == SendLocalCommand.State.SUCCESSFUL) {
                setRoomId((String) sendLocalCommand.getResult());
            }
            else {
                throw new ExecutorReportSet.CommandFailedException(
                        sendLocalCommand.getName(), sendLocalCommand.getJadeReport());
            }
        }
        else {
            throw new IllegalStateException("Device resource is not managed.");
        }
    }

    @Override
    protected State onStart(Executor executor, ExecutableManager executableManager)
    {
        DeviceResource deviceResource = getDeviceResource();
        if (deviceResource.isManaged()) {
            ManagedMode managedMode = (ManagedMode) deviceResource.getMode();
            String agentName = managedMode.getConnectorAgentName();
            ControllerAgent controllerAgent = executor.getControllerAgent();

            Room roomApi = getRoomApi();

            SendLocalCommand sendLocalCommand = controllerAgent.sendCommand(agentName, new CreateRoom(roomApi));
            if (sendLocalCommand.getState() == SendLocalCommand.State.SUCCESSFUL) {
                setRoomId((String) sendLocalCommand.getResult());
                return State.STARTED;
            }
            else {
                executableManager.createExecutionReport(this, new ExecutorReportSet.CommandFailedReport(
                        sendLocalCommand.getName(), sendLocalCommand.getJadeReport()));
                return State.STARTING_FAILED;
            }
        }
        else {
            throw new IllegalStateException("Device resource is not managed.");
        }
    }

    @Override
    protected State onUpdate(Executor executor, ExecutableManager executableManager)
    {
        UsedRoomEndpoint usedRoomEndpoint = executableManager.getStartedUsedRoomEndpoint(this);
        Room roomApi;
        if (usedRoomEndpoint != null) {
            if (usedRoomEndpoint.getState().equals(State.MODIFIED)) {
                // Used room will be automatically modified
                return State.SKIPPED;
            }
            roomApi = usedRoomEndpoint.getRoomApi();
        }
        else {
            roomApi = getRoomApi();
        }
        try {
            modifyRoom(roomApi, executor, executableManager);
            return State.STARTED;
        }
        catch (ExecutorReportSet.RoomNotStartedException exception) {
            executableManager.createExecutionReport(this, exception.getReport());
        }
        catch (ExecutorReportSet.CommandFailedException exception) {
            executableManager.createExecutionReport(this, exception.getReport());
        }
        return null;
    }

    @Override
    protected State onStop(Executor executor, ExecutableManager executableManager)
    {
        DeviceResource deviceResource = getDeviceResource();
        if (deviceResource.isManaged()) {
            ManagedMode managedMode = (ManagedMode) deviceResource.getMode();
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
                executableManager.createExecutionReport(this, new ExecutorReportSet.CommandFailedReport(
                        sendLocalCommand.getName(), sendLocalCommand.getJadeReport()));
                return State.STOPPING_FAILED;
            }
        }
        else {
            throw new IllegalStateException("Device resource is not managed.");
        }
    }

    @Override
    public void loadLazyProperties()
    {
        getResource();
        super.loadLazyProperties();
    }
}
