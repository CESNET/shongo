package cz.cesnet.shongo.controller.executor;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.api.Executable;
import cz.cesnet.shongo.controller.common.RoomConfiguration;
import cz.cesnet.shongo.controller.common.RoomSetting;
import cz.cesnet.shongo.controller.report.ReportException;
import cz.cesnet.shongo.controller.resource.Address;
import cz.cesnet.shongo.controller.resource.Alias;
import cz.cesnet.shongo.fault.TodoImplementException;

import javax.persistence.*;
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
    protected Executable createApi()
    {
        return new Executable.ResourceRoom();
    }

    @Override
    public Executable.ResourceRoom toApi(Domain domain)
    {
        return (Executable.ResourceRoom) super.toApi(domain);
    }

    @Override
    public void toApi(Executable executableApi, Domain domain)
    {
        super.toApi(executableApi, domain);

        roomEndpoint.toApi(executableApi, domain);

        if (executableApi instanceof Executable.ResourceRoom) {
            RoomConfiguration roomConfiguration = getMergedRoomConfiguration();
            Executable.ResourceRoom resourceRoomEndpointApi = (Executable.ResourceRoom) executableApi;
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
    protected State onStart(ExecutorThread executorThread, EntityManager entityManager)
    {
        if (!roomEndpoint.modifyRoom(getMergedRoomConfiguration(), executorThread, entityManager)) {
            executorThread.getLogger().error("Starting of used room endpoint '{}' failed.", getId());
            return State.STARTING_FAILED;
        }
        return super.onStart(executorThread, entityManager);
    }

    @Override
    protected State onStop(ExecutorThread executorThread, EntityManager entityManager)
    {
        if (!roomEndpoint.modifyRoom(roomEndpoint.getRoomConfiguration(), executorThread, entityManager)) {
            throw new IllegalStateException("Modifying to original configuration failed and it should always succeed.");
        }
        return super.onStop(executorThread, entityManager);
    }
}
