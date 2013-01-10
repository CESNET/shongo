package cz.cesnet.shongo.controller.executor;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.Executor;
import cz.cesnet.shongo.controller.common.RoomConfiguration;
import cz.cesnet.shongo.controller.common.RoomSetting;
import cz.cesnet.shongo.controller.report.ReportException;
import cz.cesnet.shongo.controller.resource.Address;
import cz.cesnet.shongo.controller.resource.Alias;
import cz.cesnet.shongo.fault.TodoImplementException;

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
        return new cz.cesnet.shongo.controller.api.Executable.ResourceRoom();
    }

    @Override
    public cz.cesnet.shongo.controller.api.Executable.ResourceRoom toApi(Domain domain)
    {
        return (cz.cesnet.shongo.controller.api.Executable.ResourceRoom) super.toApi(domain);
    }

    @Override
    public void toApi(cz.cesnet.shongo.controller.api.Executable executableApi, Domain domain)
    {
        super.toApi(executableApi, domain);

        roomEndpoint.toApi(executableApi, domain);

        if (executableApi instanceof cz.cesnet.shongo.controller.api.Executable.ResourceRoom) {
            RoomConfiguration roomConfiguration = getMergedRoomConfiguration();
            cz.cesnet.shongo.controller.api.Executable.ResourceRoom resourceRoomEndpointApi =
                    (cz.cesnet.shongo.controller.api.Executable.ResourceRoom) executableApi;
            resourceRoomEndpointApi.setId(domain.formatId(getId()));
            resourceRoomEndpointApi.setSlot(getSlot());
            resourceRoomEndpointApi.setState(getState().toApi());
            resourceRoomEndpointApi.setLicenseCount(roomConfiguration.getLicenseCount());
            for (Technology technology : roomConfiguration.getTechnologies()) {
                resourceRoomEndpointApi.addTechnology(technology);
            }
            for (Alias alias : getAliases()) {
                resourceRoomEndpointApi.addAlias(alias.toApi());
            }
            for (RoomSetting roomSetting : roomConfiguration.getRoomSettings()) {
                resourceRoomEndpointApi.addRoomSetting(roomSetting.toApi());
            }
        }
    }

    @Override
    @Transient
    public String getName()
    {
        return String.format("usage of virtual room '%d'", roomEndpoint.getId());
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
        List<Alias> aliases = roomEndpoint.getAliases();
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
    public String getReportDescription()
    {
        return roomEndpoint.getReportDescription();
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
    protected State onStart(Executor executor)
    {
        if (roomEndpoint.modifyRoom(getMergedRoomConfiguration(), executor)) {
            return State.STARTED;
        }
        else {
            executor.getLogger().error("Starting of used room endpoint '{}' failed.", getId());
            return State.STARTING_FAILED;
        }
    }

    @Override
    protected State onStop(Executor executor)
    {
        if (roomEndpoint.modifyRoom(roomEndpoint.getRoomConfiguration(), executor)) {
            return State.STOPPED;
        }
        else {
            executor.getLogger().error("Stopping of used room endpoint '{}' failed (should always succeed).", getId());
            return State.STOPPING_FAILED;
        }
    }
}
