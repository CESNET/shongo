package cz.cesnet.shongo.controller.executor;

import cz.cesnet.shongo.controller.Executor;
import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.report.Report;

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
    public cz.cesnet.shongo.controller.api.Executable.Compartment toApi(Report.MessageType messageType)
    {
        return (cz.cesnet.shongo.controller.api.Executable.Compartment) super.toApi(messageType);
    }

    @Override
    public void toApi(cz.cesnet.shongo.controller.api.Executable executableApi, Report.MessageType messageType)
    {
        super.toApi(executableApi, messageType);

        cz.cesnet.shongo.controller.api.Executable.Compartment compartmentApi =
                (cz.cesnet.shongo.controller.api.Executable.Compartment) executableApi;
        for (Endpoint endpoint : getEndpoints()) {
            compartmentApi.addEndpoint((cz.cesnet.shongo.controller.api.Executable.Endpoint) endpoint.toApi(messageType));
        }
        for (RoomEndpoint roomEndpoint : getRoomEndpoints()) {
            if (roomEndpoint instanceof ResourceRoomEndpoint) {
                ResourceRoomEndpoint resourceRoomEndpoint = (ResourceRoomEndpoint) roomEndpoint;
                compartmentApi.addRoom(resourceRoomEndpoint.toApi(messageType));
            }
        }
        for (Connection connection : getConnections()) {
            cz.cesnet.shongo.controller.api.Executable.Compartment.Connection connectionApi =
                    new cz.cesnet.shongo.controller.api.Executable.Compartment.Connection();
            connectionApi.setEndpointFromId(EntityIdentifier.formatId(connection.getEndpointFrom()));
            connectionApi.setEndpointToId(EntityIdentifier.formatId(connection.getEndpointTo()));
            connectionApi.setAlias(connection.getAlias().toApi());
            connectionApi.setState(connection.getState().toApi());
            connectionApi.setStateReport(getReportText(messageType));
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
