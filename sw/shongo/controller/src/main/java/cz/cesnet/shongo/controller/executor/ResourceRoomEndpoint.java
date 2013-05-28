package cz.cesnet.shongo.controller.executor;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.Room;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.connector.api.jade.multipoint.rooms.CreateRoom;
import cz.cesnet.shongo.connector.api.jade.multipoint.rooms.DeleteRoom;
import cz.cesnet.shongo.connector.api.jade.multipoint.rooms.ModifyRoom;
import cz.cesnet.shongo.controller.ControllerAgent;
import cz.cesnet.shongo.controller.Executor;
import cz.cesnet.shongo.controller.Reporter;
import cz.cesnet.shongo.controller.Role;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.controller.common.RoomConfiguration;
import cz.cesnet.shongo.controller.common.RoomSetting;
import cz.cesnet.shongo.controller.resource.*;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.scheduler.SchedulerException;
import cz.cesnet.shongo.jade.SendLocalCommand;
import cz.cesnet.shongo.report.Report;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Represents a {@link DeviceResource} which acts as {@link RoomEndpoint} in a {@link Compartment}.
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
     * {@link cz.cesnet.shongo.Technology} specific id of the {@link cz.cesnet.shongo.controller.common.RoomConfiguration}.
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
        return new cz.cesnet.shongo.controller.api.Executable.ResourceRoom();
    }

    @Override
    public cz.cesnet.shongo.controller.api.Executable.ResourceRoom toApi(Report.MessageType messageType)
    {
        return (cz.cesnet.shongo.controller.api.Executable.ResourceRoom) super.toApi(messageType);
    }

    @Override
    public void toApi(cz.cesnet.shongo.controller.api.Executable executableApi, Report.MessageType messageType)
    {
        super.toApi(executableApi, messageType);

        cz.cesnet.shongo.controller.api.Executable.ResourceRoom resourceRoomEndpointApi =
                (cz.cesnet.shongo.controller.api.Executable.ResourceRoom) executableApi;
        resourceRoomEndpointApi.setLicenseCount(getLicenseCount());
        resourceRoomEndpointApi.setResourceId(EntityIdentifier.formatId(getDeviceResource()));
        resourceRoomEndpointApi.setRoomId(getRoomId());
        for (Technology technology : getTechnologies()) {
            resourceRoomEndpointApi.addTechnology(technology);
        }
        for (Alias alias : getAssignedAliases()) {
            resourceRoomEndpointApi.addAlias(alias.toApi());
        }
        for (RoomSetting roomSetting : getRoomSettings()) {
            resourceRoomEndpointApi.addRoomSetting(roomSetting.toApi());
        }
    }

    @Override
    @Transient
    public Set<Technology> getTechnologies()
    {
        RoomConfiguration roomConfiguration = getRoomConfiguration();
        if (roomConfiguration.getTechnologies().size() > 0) {
            return roomConfiguration.getTechnologies();
        }
        else {
            return getDeviceResource().getTechnologies();
        }
    }

    /**
     * @return {@link RoomConfiguration#licenseCount} or 0 if {@link #roomConfiguration} is null
     */
    @Transient
    public int getLicenseCount()
    {
        return getRoomConfiguration().getLicenseCount();
    }

    /**
     * @return {@link RoomConfiguration#roomSettings} or empty collection if {@link #roomConfiguration} is null
     */
    @Transient
    private Collection<RoomSetting> getRoomSettings()
    {
        return getRoomConfiguration().getRoomSettings();
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
    public String getReportDescription(Report.MessageType messageType)
    {
        return String.format("room in %s", getDeviceResource().getReportDescription(messageType));
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
    @Transient
    public cz.cesnet.shongo.api.Room getRoomApi()
    {
        cz.cesnet.shongo.api.Room roomApi = new cz.cesnet.shongo.api.Room();
        roomApi.setId(roomId);
        roomApi.setTechnologies(getTechnologies());
        roomApi.setLicenseCount(getLicenseCount());
        roomApi.setDescription(getRoomDescriptionApi());
        for (RoomSetting roomSetting : getRoomSettings()) {
            roomApi.addRoomSetting(roomSetting.toApi());
        }
        for (Alias alias : getAliases()) {
            roomApi.addAlias(alias.toApi());
        }
        Authorization authorization = Authorization.getInstance();
        for (UserInformation executableOwner : authorization.getUsersWithRole(this, Role.OWNER)) {
            roomApi.addParticipant(executableOwner);
        }
        return roomApi;
    }

    @Override
    public void modifyRoom(Room roomApi, Executor executor, ExecutableManager executableManager)
            throws ExecutorReportSet.RoomNotStartedException, ExecutorReportSet.CommandFailedException
    {
        if (roomApi.getId() == null) {
            cz.cesnet.shongo.api.Alias alias = roomApi.getAlias(AliasType.ROOM_NAME);
            throw new ExecutorReportSet.RoomNotStartedException(alias != null ? alias.getValue() : null);
        }
        executor.getLogger().debug("Modifying room '{}' (named '{}') for {} licenses.",
                new Object[]{getId(), roomApi.getDescription(), roomApi.getLicenseCount()});

        DeviceResource deviceResource = getDeviceResource();
        if (deviceResource.isManaged()) {
            ManagedMode managedMode = (ManagedMode) deviceResource.getMode();
            String agentName = managedMode.getConnectorAgentName();
            ControllerAgent controllerAgent = executor.getControllerAgent();

            // TODO: Retrieve current room state and only apply changes

            SendLocalCommand sendLocalCommand = controllerAgent.sendCommand(agentName, new ModifyRoom(roomApi));
            if (sendLocalCommand.getState() != SendLocalCommand.State.SUCCESSFUL) {
                throw new ExecutorReportSet.CommandFailedException(
                        sendLocalCommand.getName(), sendLocalCommand.getJadeReport());
            }
        }
        else {
            throw new TodoImplementException("TODO: Implement modifying room in not managed device resource.");
        }
    }

    @Override
    protected State onStart(Executor executor, ExecutableManager executableManager)
    {
        executor.getLogger().debug("Starting room '{}' for {} licenses.", new Object[]{getId(), getLicenseCount()});
        List<Alias> aliases = getAliases();
        for (Alias alias : aliases) {
            executor.getLogger().debug("Room '{}' has allocated alias '{}'.", getId(), alias.getValue());
        }

        DeviceResource deviceResource = getDeviceResource();
        if (deviceResource.isManaged()) {
            ManagedMode managedMode = (ManagedMode) deviceResource.getMode();
            String agentName = managedMode.getConnectorAgentName();
            ControllerAgent controllerAgent = executor.getControllerAgent();

            cz.cesnet.shongo.api.Room roomApi = getRoomApi();

            SendLocalCommand sendLocalCommand = controllerAgent.sendCommand(agentName, new CreateRoom(roomApi));
            if (sendLocalCommand.getState() == SendLocalCommand.State.SUCCESSFUL) {
                setRoomId((String) sendLocalCommand.getResult());
                return State.STARTED;
            }
            else {
                executableManager.createExecutableReport(this, new ExecutorReportSet.CommandFailedReport(
                        sendLocalCommand.getName(), sendLocalCommand.getJadeReport()));
                return State.STARTING_FAILED;
            }
        }
        else {
            throw new TodoImplementException("TODO: Implement creating room in not managed device resource.");
        }
    }

    @Override
    protected State onUpdate(Executor executor, ExecutableManager executableManager)
    {
        cz.cesnet.shongo.api.Room roomApi;
        UsedRoomEndpoint usedRoomEndpoint = executableManager.getStartedUsedRoomEndpoint(this);
        if (usedRoomEndpoint != null) {
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
            executableManager.createExecutableReport(this, exception.getReport());
        }
        catch (ExecutorReportSet.CommandFailedException exception) {
            executableManager.createExecutableReport(this, exception.getReport());
        }
        return null;
    }

    @Override
    protected State onStop(Executor executor, ExecutableManager executableManager)
    {
        executor.getLogger().debug("Stopping room '{}' for {} licenses.", getId(), getLicenseCount());

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
                executableManager.createExecutableReport(this, new ExecutorReportSet.CommandFailedReport(
                        sendLocalCommand.getName(), sendLocalCommand.getJadeReport()));
                return State.STOPPING_FAILED;
            }
        }
        else {
            throw new TodoImplementException("TODO: Implement stopping room in not managed device resource.");
        }
    }
}
