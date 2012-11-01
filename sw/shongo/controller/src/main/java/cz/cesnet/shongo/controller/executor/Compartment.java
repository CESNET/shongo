package cz.cesnet.shongo.controller.executor;

import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.resource.Alias;
import cz.cesnet.shongo.fault.TodoImplementException;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a conference (e.g., video conference, audio conference, etc.).
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
        if ( executable instanceof Endpoint) {
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
        for ( Executable childExecutable : getChildExecutables()) {
            if ( !(childExecutable instanceof Endpoint)) {
                continue;
            }
            Endpoint endpoint = (Endpoint) childExecutable;
            if ( endpoint instanceof VirtualRoom) {
                continue;
            }
            endpoints.add(endpoint);
        }
        return endpoints;
    }

    /**
     * @return list of {@link VirtualRoom}s in the {@link Compartment}
     */
    @Transient
    public List<VirtualRoom> getVirtualRooms()
    {
        List<VirtualRoom> virtualRooms = new ArrayList<VirtualRoom>();
        for ( Executable childExecutable : getChildExecutables()) {
            if ( !(childExecutable instanceof VirtualRoom)) {
                continue;
            }
            VirtualRoom virtualRoom = (VirtualRoom) childExecutable;
            virtualRooms.add(virtualRoom);
        }
        return virtualRooms;
    }

    /**
     * @return list of {@link Connection}s in the {@link Compartment}
     */
    @Transient
    public List<Connection> getConnections()
    {
        List<Connection> connections = new ArrayList<Connection>();
        for ( Executable childExecutable : getChildExecutables()) {
            if ( !(childExecutable instanceof Connection)) {
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

    /**
     * @param domain
     * @return {@link Compartment} converted to the {@link cz.cesnet.shongo.controller.api.Compartment}
     */
    public cz.cesnet.shongo.controller.api.Compartment toApi(Domain domain)
    {
        cz.cesnet.shongo.controller.api.Compartment compartmentApi = new cz.cesnet.shongo.controller.api.Compartment();
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
        for (VirtualRoom virtualRoom : getVirtualRooms()) {
            cz.cesnet.shongo.controller.api.Compartment.VirtualRoom virtualRoomApi =

                    new cz.cesnet.shongo.controller.api.Compartment.VirtualRoom();
            virtualRoomApi.setIdentifier(virtualRoom.getId().toString());
            virtualRoomApi.setPortCount(virtualRoom.getPortCount());
            virtualRoomApi.setDescription(virtualRoom.getReportDescription());
            for (Alias alias : virtualRoom.getAssignedAliases()) {
                virtualRoomApi.addAlias(alias.toApi());
            }
            virtualRoomApi.setState(virtualRoom.getState().toApi());
            compartmentApi.addVirtualRoom(virtualRoomApi);
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
        return compartmentApi;
    }

    private State startImplementation(ExecutorThread executorThread, EntityManager entityManager, boolean startedNow)
    {
        // Create virtual rooms
        boolean virtualRoomCreated = false;
        for (VirtualRoom virtualRoom : getVirtualRooms()) {
            if (virtualRoom.getState() == State.NOT_STARTED) {
                virtualRoom.start(executorThread, entityManager);
                virtualRoomCreated = true;
            }
        }
        if (virtualRoomCreated) {
            executorThread.getLogger().info("Waiting for virtual rooms to be created...");
            try {
                Thread.sleep(executorThread.getExecutor().getCompartmentWaitingVirtualRoom().getMillis());
            }
            catch (InterruptedException exception) {
                exception.printStackTrace();
            }
        }

        // TODO: persist already assigned aliases
        if (startedNow) {
            // Assign aliases to endpoints
            for (Endpoint endpoint : getEndpoints()) {
                endpoint.assignAliases(executorThread);
            }
        }

        // Connect endpoints
        for (Connection connection : getConnections()) {
            if (connection.getState() == State.NOT_STARTED) {
                connection.start(executorThread, entityManager);
            }
        }
        return super.onStart(executorThread, entityManager);
    }

    @Override
    public State onStart(ExecutorThread executorThread, EntityManager entityManager)
    {
        return startImplementation(executorThread, entityManager, true);
    }

    @Override
    public State onResume(ExecutorThread executorThread, EntityManager entityManager)
    {
        return startImplementation(executorThread, entityManager, false);
    }

    @Override
    public State onStop(ExecutorThread executorThread, EntityManager entityManager)
    {
        // Disconnect endpoints
        for (Connection connection : getConnections()) {
            if (connection.getState() == State.STARTED) {
                connection.stop(executorThread, entityManager);
            }
        }
        // Stop virtual rooms
        for (VirtualRoom virtualRoom : getVirtualRooms()) {
            if (virtualRoom.getState() == State.STARTED) {
                virtualRoom.stop(executorThread, entityManager);
            }
        }
        return super.onStop(executorThread, entityManager);
    }
}
