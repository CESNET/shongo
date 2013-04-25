package cz.cesnet.shongo.controller.executor;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.Room;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.Executor;
import cz.cesnet.shongo.controller.Reporter;
import cz.cesnet.shongo.controller.Role;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.controller.common.RoomConfiguration;
import cz.cesnet.shongo.controller.common.RoomSetting;
import cz.cesnet.shongo.controller.resource.Address;
import cz.cesnet.shongo.controller.resource.Alias;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.resource.Resource;
import cz.cesnet.shongo.controller.scheduler.SchedulerException;
import cz.cesnet.shongo.report.Report;

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
public class UsedRoomEndpoint extends RoomEndpoint implements ManagedEndpoint, Reporter.ResourceContext
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
    @Transient
    public Resource getResource()
    {
        if (roomEndpoint instanceof ResourceRoomEndpoint) {
            ResourceRoomEndpoint resourceRoomEndpoint = (ResourceRoomEndpoint) roomEndpoint;
            return resourceRoomEndpoint.getResource();
        }
        else {
            throw new TodoImplementException(roomEndpoint.getClass().getName());
        }
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
            resourceRoomEndpointApi.setRoomId(resourceRoomEndpoint.getRoomId());
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
    public void addAssignedAlias(Alias assignedAlias) throws SchedulerException
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
    public String getReportDescription(Report.MessageType messageType)
    {
        return roomEndpoint.getReportDescription(messageType);
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
        roomApi.setDescription(getRoomDescriptionApi());
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
    public void modifyRoom(Room roomApi, Executor executor, ExecutableManager executableManager)
            throws ExecutorReportSet.RoomNotStartedException, ExecutorReportSet.CommandFailedException
    {
        roomEndpoint.modifyRoom(roomApi, executor, executableManager);
    }

    @Override
    protected State onStart(Executor executor, ExecutableManager executableManager)
    {
        try {
            roomEndpoint.modifyRoom(getRoomApi(), executor, executableManager);
            return State.STARTED;
        }
        catch (ExecutorReportSet.RoomNotStartedException exception) {
            executableManager.createExecutableReport(this, exception.getReport());
        }
        catch (ExecutorReportSet.CommandFailedException exception) {
            executableManager.createExecutableReport(this, exception.getReport());
        }
        return State.STARTING_FAILED;
    }

    @Override
    protected State onUpdate(Executor executor, ExecutableManager executableManager)
    {
        try {
            modifyRoom(getRoomApi(), executor, executableManager);
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
        try {
            roomEndpoint.modifyRoom(roomEndpoint.getRoomApi(), executor, executableManager);
            return State.STOPPED;
        }
        catch (ExecutorReportSet.RoomNotStartedException exception) {
            executableManager.createExecutableReport(this, exception.getReport());
        }
        catch (ExecutorReportSet.CommandFailedException exception) {
            executableManager.createExecutableReport(this, exception.getReport());
        }
        return State.STOPPING_FAILED;
    }
}
