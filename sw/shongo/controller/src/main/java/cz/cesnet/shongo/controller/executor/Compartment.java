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
     * List of {@link Endpoint}s which participates in the {@link Compartment}.
     */
    private List<Endpoint> endpoints = new ArrayList<Endpoint>();

    /**
     * List of {@link VirtualRoom}s which participates in the {@link Compartment}.
     */
    private List<VirtualRoom> virtualRooms = new ArrayList<VirtualRoom>();

    /**
     * List of {@link Connection}s which are established in the {@link Compartment}.
     */
    private List<Connection> connections = new ArrayList<Connection>();

    /**
     * Total sum of endpoints (calculated as sum of {@link Endpoint#getCount()} because one
     * {@link Endpoint} can represent multiple endpoints).
     */
    private int totalEndpointCount = 0;

    /**
     * @return {@link #endpoints}
     */
    @OneToMany(cascade = CascadeType.ALL)
    @Access(AccessType.FIELD)
    public List<Endpoint> getEndpoints()
    {
        return endpoints;
    }

    /**
     * @param endpoint to be added to the {@link #endpoints}
     */
    public void addEndpoint(Endpoint endpoint)
    {
        endpoints.add(endpoint);

        // Update total endpoint count
        totalEndpointCount += endpoint.getCount();
    }

    /**
     * @param endpoint to be removed from the {@link #endpoints}
     */
    public void removeEndpoint(Endpoint endpoint)
    {
        // Update total endpoint count
        totalEndpointCount -= endpoint.getCount();

        endpoints.remove(endpoint);
    }

    /**
     * @return {@link #virtualRooms}
     */
    @OneToMany(cascade = CascadeType.ALL)
    @Access(AccessType.FIELD)
    public List<VirtualRoom> getVirtualRooms()
    {
        return virtualRooms;
    }

    /**
     * @param virtualRoom to be added to the {@link #virtualRooms}
     */
    public void addVirtualRoom(VirtualRoom virtualRoom)
    {
        virtualRooms.add(virtualRoom);
    }

    /**
     * @param virtualRoom to be removed from the {@link #virtualRooms}
     */
    public void removeVirtualRoom(VirtualRoom virtualRoom)
    {
        virtualRooms.remove(virtualRoom);
    }

    /**
     * @return {@link #connections}
     */
    @OneToMany(cascade = CascadeType.ALL)
    @Access(AccessType.FIELD)
    public List<Connection> getConnections()
    {
        return connections;
    }

    /**
     * @param connection to be added to the {@link #connections}
     */
    public void addConnection(Connection connection)
    {
        connections.add(connection);
    }

    /**
     * @param connection to be removed from the {@link #connections}
     */
    public void removeConnection(Connection connection)
    {
        connections.remove(connection);
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

    private void startImplementation(ExecutorThread executorThread, EntityManager entityManager, boolean startedNow)
    {
        // Create virtual rooms
        boolean virtualRoomCreated = false;
        for (VirtualRoom virtualRoom : getVirtualRooms()) {
            if (virtualRoom.getState() == VirtualRoom.State.NOT_CREATED) {
                entityManager.getTransaction().begin();
                virtualRoom.create(executorThread);
                entityManager.getTransaction().commit();
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
            if (connection.getState() == Connection.State.NOT_ESTABLISHED) {
                entityManager.getTransaction().begin();
                connection.establish(executorThread);
                entityManager.getTransaction().commit();
            }
        }
    }

    @Override
    public void start(ExecutorThread executorThread, EntityManager entityManager)
    {
        startImplementation(executorThread, entityManager, true);
    }

    @Override
    public void resume(ExecutorThread executorThread, EntityManager entityManager)
    {
        startImplementation(executorThread, entityManager, false);
    }

    @Override
    public void stop(ExecutorThread executorThread, EntityManager entityManager)
    {
        // Disconnect endpoints
        for (Connection connection : getConnections()) {
            if (connection.getState() == Connection.State.ESTABLISHED) {
                entityManager.getTransaction().begin();
                connection.close(executorThread);
                entityManager.getTransaction().commit();
            }
        }
        // Stop virtual rooms
        for (VirtualRoom virtualRoom : getVirtualRooms()) {
            if (virtualRoom.getState() == VirtualRoom.State.CREATED) {
                entityManager.getTransaction().begin();
                virtualRoom.delete(executorThread);
                entityManager.getTransaction().commit();
            }
        }
    }
}
