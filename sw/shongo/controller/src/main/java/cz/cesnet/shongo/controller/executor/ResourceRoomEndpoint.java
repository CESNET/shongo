package cz.cesnet.shongo.controller.executor;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.connector.api.jade.multipoint.rooms.CreateRoom;
import cz.cesnet.shongo.connector.api.jade.multipoint.rooms.DeleteRoom;
import cz.cesnet.shongo.connector.api.jade.multipoint.rooms.ModifyRoom;
import cz.cesnet.shongo.controller.ControllerAgent;
import cz.cesnet.shongo.controller.Executor;
import cz.cesnet.shongo.controller.api.Executable;
import cz.cesnet.shongo.controller.common.IdentifierFormat;
import cz.cesnet.shongo.controller.common.RoomConfiguration;
import cz.cesnet.shongo.controller.common.RoomSetting;
import cz.cesnet.shongo.controller.executor.report.CommandFailureReport;
import cz.cesnet.shongo.controller.report.ReportException;
import cz.cesnet.shongo.controller.resource.*;
import cz.cesnet.shongo.controller.scheduler.report.ResourceReport;
import cz.cesnet.shongo.fault.TodoImplementException;
import cz.cesnet.shongo.jade.AgentActionCommand;
import cz.cesnet.shongo.jade.Command;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToOne;
import javax.persistence.Transient;
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
public class ResourceRoomEndpoint extends RoomEndpoint implements ManagedEndpoint
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
    @org.hibernate.annotations.Index(name="room_id")
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
        return new Executable.ResourceRoom();
    }

    @Override
    public Executable.ResourceRoom toApi()
    {
        return (Executable.ResourceRoom) super.toApi();
    }

    @Override
    public void toApi(cz.cesnet.shongo.controller.api.Executable executableApi)
    {
        Executable.ResourceRoom resourceRoomEndpointApi = (Executable.ResourceRoom) executableApi;
        resourceRoomEndpointApi.setSlot(getSlot());
        resourceRoomEndpointApi.setState(getState().toApi());
        resourceRoomEndpointApi.setStateReport(getReportText());
        resourceRoomEndpointApi.setLicenseCount(getLicenseCount());
        resourceRoomEndpointApi.setResourceId(IdentifierFormat.formatGlobalId(getDeviceResource()));
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
    public void addAssignedAlias(Alias assignedAlias) throws ReportException
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
    public String getReportDescription()
    {
        return String.format("room in %s",
                ResourceReport.formatResource(getDeviceResource()));
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
            throw new IllegalStateException("Resource " + getReportDescription() + " is not managed!");
        }
    }

    @Override
    protected State onStart(Executor executor)
    {
        executor.getLogger().debug("Starting room '{}' for {} licenses.", new Object[]{getId(), getLicenseCount()});
        List<Alias> aliases = getAliases();
        for (Alias alias : aliases) {
            executor.getLogger().debug("Room '{}' has allocated alias '{}'.", getId(), alias.getValue());
        }

        if (getDeviceResource().isManaged()) {
            ManagedMode managedMode = (ManagedMode) getDeviceResource().getMode();
            String agentName = managedMode.getConnectorAgentName();
            ControllerAgent controllerAgent = executor.getControllerAgent();

            cz.cesnet.shongo.api.Room room = new cz.cesnet.shongo.api.Room();
            room.setTechnologies(getTechnologies());
            room.setLicenseCount(getLicenseCount());
            room.setDescription(getRoomDescription());
            for (RoomSetting roomSetting : getRoomSettings()) {
                room.fillOptions(roomSetting.toApi());
            }
            for (Alias alias : getAliases()) {
                room.addAlias(alias.toApi());
            }
            Command command = controllerAgent.performCommand(new AgentActionCommand(agentName,
                    new CreateRoom(room)));
            if (command.getState() == Command.State.SUCCESSFUL) {
                setRoomId((String) command.getResult());
                return State.STARTED;
            }
            else {
                addReport(new CommandFailureReport(command.getFailure()));
                return State.STARTING_FAILED;
            }
        }
        else {
            throw new TodoImplementException("TODO: Implement creating room in not managed device resource.");
        }
    }

    @Override
    public boolean modifyRoom(String roomDescription, RoomConfiguration roomConfiguration, List<Alias> roomAliases,
            Executor executor)
    {
        executor.getLogger().debug("Modifying room '{}' (named '{}') for {} licenses.",
                new Object[]{getId(), roomDescription, roomConfiguration.getLicenseCount()});

        if (getDeviceResource().isManaged()) {
            ManagedMode managedMode = (ManagedMode) getDeviceResource().getMode();
            String agentName = managedMode.getConnectorAgentName();
            ControllerAgent controllerAgent = executor.getControllerAgent();

            cz.cesnet.shongo.api.Room room = new cz.cesnet.shongo.api.Room();
            room.setId(roomId);
            room.setDescription(roomDescription);
            room.setTechnologies(roomConfiguration.getTechnologies());
            room.setLicenseCount(roomConfiguration.getLicenseCount());
            for (RoomSetting roomSetting : roomConfiguration.getRoomSettings()) {
                room.fillOptions(roomSetting.toApi());
            }
            for (Alias alias : roomAliases) {
                room.addAlias(alias.toApi());
            }
            Command command = controllerAgent.performCommand(new AgentActionCommand(agentName, new ModifyRoom(room)));
            if (command.getState() == Command.State.SUCCESSFUL) {
                return true;
            }
            else {
                addReport(new CommandFailureReport(command.getFailure()));
                return false;
            }
        }
        else {
            throw new TodoImplementException("TODO: Implement modifying room in not managed device resource.");
        }
    }

    @Override
    protected State onStop(Executor executor)
    {
        executor.getLogger().debug("Stopping room '{}' for {} licenses.", getId(), getLicenseCount());

        if (getDeviceResource().isManaged()) {
            ManagedMode managedMode = (ManagedMode) getDeviceResource().getMode();
            String agentName = managedMode.getConnectorAgentName();
            ControllerAgent controllerAgent = executor.getControllerAgent();
            String roomId = getRoomId();
            if (roomId == null) {
                throw new IllegalStateException("Cannot delete virtual room because it's identifier is null.");
            }
            Command command = controllerAgent.performCommand(
                    new AgentActionCommand(agentName, new DeleteRoom(roomId)));
            if (command.getState() == Command.State.SUCCESSFUL) {
                return State.STOPPED;
            }
            else {
                addReport(new CommandFailureReport(command.getFailure()));
                return State.STOPPING_FAILED;
            }
        }
        else {
            throw new TodoImplementException("TODO: Implement stopping room in not managed device resource.");
        }
    }
}
