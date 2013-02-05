package cz.cesnet.shongo.controller.executor;

import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.Executor;
import cz.cesnet.shongo.controller.resource.Alias;
import cz.cesnet.shongo.fault.TodoImplementException;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a conference (e.g., web/video/audio conference).
 * <p/>
 * In each {@link Compartment} participates multiple {@link Endpoint}s which are interconnected by {@link Connection}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class Compartment extends Executable
{
    /**
     * Total sum of endpoints (calculated as sum of {@link Endpoint#getCount()} because one
     * {@link Endpoint} can represent multiple endpoints).
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
        return new cz.cesnet.shongo.controller.api.Executable.Compartment();
    }

    @Override
    public cz.cesnet.shongo.controller.api.Executable.Compartment toApi()
    {
        return (cz.cesnet.shongo.controller.api.Executable.Compartment) super.toApi();
    }

    @Override
    public void toApi(cz.cesnet.shongo.controller.api.Executable executableApi)
    {
        super.toApi(executableApi);

        cz.cesnet.shongo.controller.Domain localDomain = Domain.getLocalDomain();

        cz.cesnet.shongo.controller.api.Executable.Compartment compartmentApi =
                (cz.cesnet.shongo.controller.api.Executable.Compartment) executableApi;
        compartmentApi.setId(localDomain.formatId(this));
        compartmentApi.setSlot(getSlot());
        compartmentApi.setState(getState().toApi());
        compartmentApi.setStateReport(getReportText());
        for (Endpoint endpoint : getEndpoints()) {
            cz.cesnet.shongo.controller.api.Executable.Compartment.Endpoint endpointApi =
                    new cz.cesnet.shongo.controller.api.Executable.Compartment.Endpoint();
            endpointApi.setId(localDomain.formatId(endpoint));
            endpointApi.setDescription(endpoint.getReportDescription());
            for (Alias alias : endpoint.getAssignedAliases()) {
                endpointApi.addAlias(alias.toApi());
            }
            compartmentApi.addEndpoint(endpointApi);
        }
        for (RoomEndpoint roomEndpoint : getRoomEndpoints()) {
            if (roomEndpoint instanceof ResourceRoomEndpoint) {
                ResourceRoomEndpoint resourceRoomEndpoint = (ResourceRoomEndpoint) roomEndpoint;
                compartmentApi.addRoom(resourceRoomEndpoint.toApi());
            }
        }
        for (Connection connection : getConnections()) {
            cz.cesnet.shongo.controller.api.Executable.Compartment.Connection connectionApi =
                    new cz.cesnet.shongo.controller.api.Executable.Compartment.Connection();
            connectionApi.setEndpointFromId(
                    localDomain.formatId(connection.getEndpointFrom()));
            connectionApi.setEndpointToId(
                    localDomain.formatId(connection.getEndpointTo()));
            connectionApi.setAlias(connection.getAlias().toApi());
            connectionApi.setState(connection.getState().toApi());
            connectionApi.setStateReport(getReportText());
            compartmentApi.addConnection(connectionApi);
        }
    }

    @Override
    public State onStart(Executor executor)
    {
        executor.getLogger().debug("Starting compartment '{}'...", getId());

        return super.onStart(executor);
    }

    @Override
    public State onStop(Executor executor)
    {
        executor.getLogger().debug("Stopping compartment '{}'...", getId());

        return super.onStop(executor);
    }
}
