package cz.cesnet.shongo.controller.executor;

import cz.cesnet.shongo.controller.Domain;
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
    @Transient
    public String getName()
    {
        return String.format("compartment '%d'", getId());
    }

    @Override
    protected cz.cesnet.shongo.controller.api.Executable createApi()
    {
        return new cz.cesnet.shongo.controller.api.Compartment();
    }

    @Override
    public cz.cesnet.shongo.controller.api.Compartment toApi(Domain domain)
    {
        return (cz.cesnet.shongo.controller.api.Compartment) super.toApi(domain);
    }

    @Override
    public void toApi(cz.cesnet.shongo.controller.api.Executable executableApi, Domain domain)
    {
        super.toApi(executableApi, domain);

        cz.cesnet.shongo.controller.api.Compartment compartmentApi =
                (cz.cesnet.shongo.controller.api.Compartment) executableApi;
        compartmentApi.setIdentifier(domain.formatIdentifier(getId()));
        compartmentApi.setSlot(getSlot());
        compartmentApi.setState(getState().toApi());
        for (Endpoint endpoint : getEndpoints()) {
            cz.cesnet.shongo.controller.api.Compartment.Endpoint endpointApi =
                    new cz.cesnet.shongo.controller.api.Compartment.Endpoint();
            endpointApi.setIdentifier(endpoint.getId().toString());
            endpointApi.setDescription(endpoint.getReportDescription());
            for (Alias alias : endpoint.getAssignedAliases()) {
                endpointApi.addAlias(alias.toApi());
            }
            compartmentApi.addEndpoint(endpointApi);
        }
        for (RoomEndpoint roomEndpoint : getRoomEndpoints()) {
            cz.cesnet.shongo.controller.api.Compartment.RoomEndpoint roomEndpointApi =
                    new cz.cesnet.shongo.controller.api.Compartment.RoomEndpoint();
            roomEndpointApi.setIdentifier(roomEndpoint.getId().toString());
            roomEndpointApi.setLicenseCount(roomEndpoint.getRoomConfiguration().getLicenseCount());
            roomEndpointApi.setDescription(roomEndpoint.getReportDescription());
            for (Alias alias : roomEndpoint.getAssignedAliases()) {
                roomEndpointApi.addAlias(alias.toApi());
            }
            roomEndpointApi.setState(roomEndpoint.getState().toApi());
            compartmentApi.addRoomEndpoint(roomEndpointApi);
        }
        for (Connection connection : getConnections()) {
            if (connection instanceof ConnectionByAddress) {
                ConnectionByAddress connectionByAddress = (ConnectionByAddress) connection;
                cz.cesnet.shongo.controller.api.Compartment.ConnectionByAddress connectionByAddressApi =
                        new cz.cesnet.shongo.controller.api.Compartment.ConnectionByAddress();
                connectionByAddressApi.setEndpointFromIdentifier(connection.getEndpointFrom().getId().toString());
                connectionByAddressApi.setEndpointToIdentifier(connection.getEndpointTo().getId().toString());
                connectionByAddressApi.setAddress(connectionByAddress.getAddress().getValue());
                connectionByAddressApi.setTechnology(connectionByAddress.getTechnology());
                connectionByAddressApi.setState(connectionByAddress.getState().toApi());
                compartmentApi.addConnection(connectionByAddressApi);
            }
            else if (connection instanceof ConnectionByAlias) {
                ConnectionByAlias connectionByAlias = (ConnectionByAlias) connection;
                cz.cesnet.shongo.controller.api.Compartment.ConnectionByAlias connectionByAliasApi =
                        new cz.cesnet.shongo.controller.api.Compartment.ConnectionByAlias();
                connectionByAliasApi.setEndpointFromIdentifier(connection.getEndpointFrom().getId().toString());
                connectionByAliasApi.setEndpointToIdentifier(connection.getEndpointTo().getId().toString());
                connectionByAliasApi.setAlias(connectionByAlias.getAlias().toApi());
                connectionByAliasApi.setState(connectionByAlias.getState().toApi());
                compartmentApi.addConnection(connectionByAliasApi);
            }
            else {
                throw new TodoImplementException(connection.getClass().getCanonicalName());
            }
        }
    }

    private State startImplementation(ExecutorThread executorThread, EntityManager entityManager)
    {
        // Create virtual rooms
        boolean roomStarted = (startChildren(RoomEndpoint.class, executorThread, entityManager) > 0);
        if (roomStarted) {
            executorThread.getLogger().info("Waiting for virtual rooms to be created...");
            try {
                Thread.sleep(executorThread.getExecutor().getCompartmentWaitingRoom().getMillis());
            }
            catch (InterruptedException exception) {
                exception.printStackTrace();
            }
        }

        // Start other endpoints (e.g., assign aliases to them)
        startChildren(Endpoint.class, executorThread, entityManager);

        // Start connections
        startChildren(Connection.class, executorThread, entityManager);

        return super.onStart(executorThread, entityManager);
    }

    @Override
    public State onStart(ExecutorThread executorThread, EntityManager entityManager)
    {
        return startImplementation(executorThread, entityManager);
    }

    @Override
    public State onResume(ExecutorThread executorThread, EntityManager entityManager)
    {
        return startImplementation(executorThread, entityManager);
    }

    @Override
    public State onStop(ExecutorThread executorThread, EntityManager entityManager)
    {
        // Stop connections
        stopChildren(Connection.class, executorThread, entityManager);

        // Stop endpoints (virtual rooms too)
        stopChildren(Endpoint.class, executorThread, entityManager);

        return super.onStop(executorThread, entityManager);
    }
}
