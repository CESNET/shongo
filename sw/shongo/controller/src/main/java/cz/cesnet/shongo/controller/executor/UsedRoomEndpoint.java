package cz.cesnet.shongo.controller.executor;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.connector.api.jade.multipoint.rooms.ModifyRoom;
import cz.cesnet.shongo.controller.ControllerAgent;
import cz.cesnet.shongo.controller.Executor;
import cz.cesnet.shongo.controller.Role;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.controller.common.RoomConfiguration;
import cz.cesnet.shongo.controller.common.RoomSetting;
import cz.cesnet.shongo.controller.executor.report.CommandFailureReport;
import cz.cesnet.shongo.controller.report.ReportException;
import cz.cesnet.shongo.controller.resource.Address;
import cz.cesnet.shongo.controller.resource.Alias;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.resource.ManagedMode;
import cz.cesnet.shongo.fault.TodoImplementException;
import cz.cesnet.shongo.jade.SendLocalCommand;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Represents a re-used {@link cz.cesnet.shongo.controller.executor.RoomEndpoint} for different
 * {@link RoomConfiguration}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class UsedRoomEndpoint extends RoomEndpoint implements ManagedEndpoint
{
    /**
     * {@link RoomEndpoint} which is re-used.
     */
    private RoomEndpoint roomEndpoint;

    /**
     * Constructor.
     */
    public UsedRoomEndpoint()
    {
    }

    /**
     * @return {@link #roomEndpoint}
     */
    @OneToOne
    @Access(AccessType.FIELD)
    public RoomEndpoint getRoomEndpoint()
    {
        return roomEndpoint;
    }

    /**
     * @param roomEndpoint sets the {@link #roomEndpoint}
     */
    public void setRoomEndpoint(RoomEndpoint roomEndpoint)
    {
        this.roomEndpoint = roomEndpoint;
    }

    /**
     * @return merged {@link RoomConfiguration} of {@link #roomConfiguration} and {@link #roomEndpoint#roomConfiguration}
     */
    @Transient
    private RoomConfiguration getMergedRoomConfiguration()
    {
        RoomConfiguration roomConfiguration = getRoomConfiguration();
        RoomConfiguration roomEndpointConfiguration = roomEndpoint.getRoomConfiguration();
        RoomConfiguration mergedRoomConfiguration = new RoomConfiguration();
        mergedRoomConfiguration.setLicenseCount(
                roomConfiguration.getLicenseCount() + roomEndpointConfiguration.getLicenseCount());
        mergedRoomConfiguration.setTechnologies(roomConfiguration.getTechnologies());
        mergedRoomConfiguration.setRoomSettings(roomConfiguration.getRoomSettings());
        if (roomEndpointConfiguration.getRoomSettings().size() > 0) {
            throw new TodoImplementException("Merging room settings.");
        }
        return mergedRoomConfiguration;
    }

    @Override
    @Transient
    public Collection<Executable> getExecutionDependencies()
    {
        List<Executable> dependencies = new ArrayList<Executable>();
        dependencies.add(roomEndpoint);
        return dependencies;
    }

    @Override
    protected cz.cesnet.shongo.controller.api.Executable createApi()
    {
        return roomEndpoint.createApi();
    }

    @Override
    public void toApi(cz.cesnet.shongo.controller.api.Executable executableApi)
    {
        cz.cesnet.shongo.controller.api.Executable.ResourceRoom resourceRoomEndpointApi =
                (cz.cesnet.shongo.controller.api.Executable.ResourceRoom) executableApi;
        resourceRoomEndpointApi.setId(EntityIdentifier.formatId(this));
        resourceRoomEndpointApi.setSlot(getSlot());
        resourceRoomEndpointApi.setState(getState().toApi());
        resourceRoomEndpointApi.setStateReport(getReportText());

        if (roomEndpoint instanceof ResourceRoomEndpoint) {
            ResourceRoomEndpoint resourceRoomEndpoint = (ResourceRoomEndpoint) roomEndpoint;
            resourceRoomEndpointApi.setResourceId(EntityIdentifier.formatId(resourceRoomEndpoint.getDeviceResource()));
        }
        else {
            throw new TodoImplementException(roomEndpoint.getClass().getName());
        }

        RoomConfiguration roomConfiguration = getMergedRoomConfiguration();

        // Set merged license count
        resourceRoomEndpointApi.setLicenseCount(roomConfiguration.getLicenseCount());

        // Set merged technologies
        resourceRoomEndpointApi.clearTechnologies();
        for (Technology technology : roomConfiguration.getTechnologies()) {
            resourceRoomEndpointApi.addTechnology(technology);
        }

        // Set merged aliases
        for (Alias alias : getAliases()) {
            resourceRoomEndpointApi.addAlias(alias.toApi());
        }

        // Set merged room settings
        resourceRoomEndpointApi.clearRoomSettings();
        for (RoomSetting roomSetting : roomConfiguration.getRoomSettings()) {
            resourceRoomEndpointApi.addRoomSetting(roomSetting.toApi());
        }
    }

    @Override
    @Transient
    public String getRoomId()
    {
        return roomEndpoint.getRoomId();
    }

    @Override
    @Transient
    public Set<Technology> getTechnologies()
    {
        return getRoomConfiguration().getTechnologies();
    }

    @Override
    @Transient
    public boolean isStandalone()
    {
        return roomEndpoint.isStandalone();
    }

    @Override
    @Transient
    public List<Alias> getAliases()
    {
        List<Alias> aliases = new ArrayList<Alias>();
        aliases.addAll(roomEndpoint.getAliases());
        aliases.addAll(super.getAssignedAliases());
        return aliases;
    }

    @Override
    public void addAssignedAlias(Alias assignedAlias) throws ReportException
    {
        super.addAssignedAlias(assignedAlias);
    }

    @Override
    @Transient
    public Address getAddress()
    {
        return roomEndpoint.getAddress();
    }

    @Override
    @Transient
    public String getDescription()
    {
        return roomEndpoint.getDescription();
    }

    @Override
    @Transient
    public String getConnectorAgentName()
    {
        if (roomEndpoint instanceof ManagedEndpoint) {
            ManagedEndpoint managedEndpoint = (ManagedEndpoint) roomEndpoint;
            return managedEndpoint.getConnectorAgentName();
        }
        return null;
    }

    @Override
    @Transient
    public cz.cesnet.shongo.api.Room getRoomApi()
    {
        RoomConfiguration roomConfiguration = getMergedRoomConfiguration();

        cz.cesnet.shongo.api.Room roomApi = roomEndpoint.getRoomApi();
        roomApi.setDescription(getRoomDescription());
        roomApi.setLicenseCount(roomConfiguration.getLicenseCount());
        for (RoomSetting roomSetting : roomConfiguration.getRoomSettings()) {
            roomApi.addRoomSetting(roomSetting.toApi());
        }
        for (Alias alias : getAssignedAliases()) {
            roomApi.addAlias(alias.toApi());
        }
        Authorization authorization = Authorization.getInstance();
        for (UserInformation executableOwner : authorization.getUsersWithRole(this, Role.OWNER)) {
            roomApi.addParticipant(executableOwner);
        }
        return roomApi;
    }

    @Override
    protected State onStart(Executor executor)
    {
        if (modifyRoom(getRoomApi(), executor)) {
            return State.STARTED;
        }
        else {
            executor.getLogger().error("Starting used room '{}' failed.", getId());
            return State.STARTING_FAILED;
        }
    }

    @Override
    protected State onStop(Executor executor)
    {
        if (modifyRoom(roomEndpoint.getRoomApi(), executor)) {
            return State.STOPPED;
        }
        else {
            executor.getLogger().error("Stopping used room '{}' failed (should always succeed).", getId());
            return State.STOPPING_FAILED;
        }
    }

    public boolean modifyRoom(cz.cesnet.shongo.api.Room roomApi, Executor executor)
    {
        executor.getLogger().debug("Modifying room '{}' (named '{}') for {} licenses.",
                new Object[]{getId(), roomApi.getDescription(), roomApi.getLicenseCount()});

        DeviceResource deviceResource;
        if (roomEndpoint instanceof ResourceRoomEndpoint) {
            ResourceRoomEndpoint resourceRoomEndpoint = (ResourceRoomEndpoint) roomEndpoint;
            deviceResource = resourceRoomEndpoint.getDeviceResource();
        }
        else {
            throw new TodoImplementException(roomEndpoint.getClass().getName());
        }

        if (deviceResource.isManaged()) {
            ManagedMode managedMode = (ManagedMode) deviceResource.getMode();
            String agentName = managedMode.getConnectorAgentName();
            ControllerAgent controllerAgent = executor.getControllerAgent();

            // TODO: Retrieve current room state and only apply changes

            SendLocalCommand sendLocalCommand = controllerAgent.sendCommand(agentName, new ModifyRoom(roomApi));
            if (sendLocalCommand.getState() == SendLocalCommand.State.SUCCESSFUL) {
                return true;
            }
            else {
                addReport(new CommandFailureReport(sendLocalCommand.getFailure()));
                return false;
            }
        }
        else {
            throw new TodoImplementException("TODO: Implement modifying room in not managed device resource.");
        }
    }
}
