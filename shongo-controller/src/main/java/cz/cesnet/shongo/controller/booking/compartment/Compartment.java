package cz.cesnet.shongo.controller.booking.compartment;

import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.executor.Executor;
import cz.cesnet.shongo.controller.api.CompartmentExecutable;
import cz.cesnet.shongo.controller.api.ConnectionExecutable;
import cz.cesnet.shongo.controller.api.EndpointExecutable;
import cz.cesnet.shongo.controller.booking.room.RoomEndpoint;
import cz.cesnet.shongo.controller.booking.executable.Endpoint;
import cz.cesnet.shongo.controller.booking.executable.Executable;
import cz.cesnet.shongo.controller.booking.executable.ExecutableManager;
import cz.cesnet.shongo.controller.booking.room.ResourceRoomEndpoint;
import cz.cesnet.shongo.report.Report;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a conference (e.g., web/video/audio conference).
 * <p/>
 * In each {@link Compartment} participates multiple {@link cz.cesnet.shongo.controller.booking.executable.Endpoint}s which are interconnected by {@link Connection}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class Compartment extends Executable
{
    /**
     * Total sum of endpoints (calculated as sum of {@link cz.cesnet.shongo.controller.booking.executable.Endpoint#getCount()} because one
     * {@link cz.cesnet.shongo.controller.booking.executable.Endpoint} can represent multiple endpoints).
     */
    private int totalEndpointCount = 0;

    @Override
    public void addChildExecutable(Executable executable)
    {
        if (executable instanceof Endpoint) {
            Endpoint endpoint = (Endpoint) executable;

            // Update total endpoint count
            totalEndpointCount += endpoint.getCount();
        }
        super.addChildExecutable(executable);
    }

    /**
     * @return list of {@link Endpoint}s in the {@link Compartment}
     */
    @Transient
    public List<Endpoint> getEndpoints()
    {
        List<Endpoint> endpoints = new ArrayList<Endpoint>();
        for (Executable childExecutable : getChildExecutables()) {
            if (!(childExecutable instanceof Endpoint)) {
                continue;
            }
            Endpoint endpoint = (Endpoint) childExecutable;
            if (endpoint instanceof RoomEndpoint) {
                continue;
            }
            endpoints.add(endpoint);
        }
        return endpoints;
    }

    /**
     * @return list of {@link RoomEndpoint}s in the {@link Compartment}
     */
    @Transient
    public List<RoomEndpoint> getRoomEndpoints()
    {
        List<RoomEndpoint> roomEndpoints = new ArrayList<RoomEndpoint>();
        for (Executable childExecutable : getChildExecutables()) {
            childExecutable = getLazyImplementation(childExecutable);
            if (!(childExecutable instanceof RoomEndpoint)) {
                continue;
            }
            RoomEndpoint roomEndpoint = (RoomEndpoint) childExecutable;
            roomEndpoints.add(roomEndpoint);
        }
        return roomEndpoints;
    }

    /**
     * @return list of {@link Connection}s in the {@link Compartment}
     */
    @Transient
    public List<Connection> getConnections()
    {
        List<Connection> connections = new ArrayList<Connection>();
        for (Executable childExecutable : getChildExecutables()) {
            if (!(childExecutable instanceof Connection)) {
                continue;
            }
            Connection connection = (Connection) childExecutable;
            connections.add(connection);
        }
        return connections;
    }

    /**
     * @return {@link #totalEndpointCount}
     */
    @Column
    @Access(AccessType.FIELD)
    public int getTotalEndpointCount()
    {
        return totalEndpointCount;
    }

    @Override
    protected cz.cesnet.shongo.controller.api.Executable createApi()
    {
        return new CompartmentExecutable();
    }

    @Override
    public CompartmentExecutable toApi(EntityManager entityManager, Report.UserType userType)
    {
        return (CompartmentExecutable) super.toApi(entityManager, userType);
    }

    @Override
    public void toApi(cz.cesnet.shongo.controller.api.Executable executableApi, EntityManager entityManager,
            Report.UserType userType)
    {
        super.toApi(executableApi, entityManager, userType);

        CompartmentExecutable compartmentApi = (CompartmentExecutable) executableApi;
        for (Endpoint endpoint : getEndpoints()) {
            compartmentApi.addEndpoint((EndpointExecutable) endpoint.toApi(entityManager, userType));
        }
        for (RoomEndpoint roomEndpoint : getRoomEndpoints()) {
            if (roomEndpoint instanceof ResourceRoomEndpoint) {
                ResourceRoomEndpoint resourceRoomEndpoint = (ResourceRoomEndpoint) roomEndpoint;
                compartmentApi.addRoom(resourceRoomEndpoint.toApi(entityManager, userType));
            }
        }
        for (Connection connection : getConnections()) {
            ConnectionExecutable connectionApi = new ConnectionExecutable();
            connectionApi.setEndpointFromId(ObjectIdentifier.formatId(connection.getEndpointFrom()));
            connectionApi.setEndpointToId(ObjectIdentifier.formatId(connection.getEndpointTo()));
            connectionApi.setAlias(connection.getAlias().toApi());
            connectionApi.setState(connection.getState().toApi());
            compartmentApi.addConnection(connectionApi);
        }
    }

    @Override
    public State onStart(Executor executor, ExecutableManager executableManager)
    {
        executor.getLogger().debug("Starting compartment '{}'...", getId());

        return super.onStart(executor, executableManager);
    }

    @Override
    public State onStop(Executor executor, ExecutableManager executableManager)
    {
        executor.getLogger().debug("Stopping compartment '{}'...", getId());

        return super.onStop(executor, executableManager);
    }
}
