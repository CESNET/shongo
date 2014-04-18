package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.DataMap;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an allocated compartment.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class CompartmentExecutable extends Executable
{
    /**
     * List of {@link EndpointExecutable}s.
     */
    private List<EndpointExecutable> endpoints = new ArrayList<EndpointExecutable>();

    /**
     * List of {@link RoomExecutable}s.
     */
    private List<RoomExecutable> rooms = new ArrayList<RoomExecutable>();

    /**
     * List of {@link ConnectionExecutable}s.
     */
    private List<ConnectionExecutable> connections = new ArrayList<ConnectionExecutable>();

    /**
     * @return {@link #endpoints}
     */
    public List<EndpointExecutable> getEndpoints()
    {
        return endpoints;
    }

    /**
     * @param endpoints sets the {@link #endpoints}
     */
    public void setEndpoints(List<EndpointExecutable> endpoints)
    {
        this.endpoints = endpoints;
    }

    /**
     * @param endpoint to be added to the {@link #endpoints}
     */
    public void addEndpoint(EndpointExecutable endpoint)
    {
        endpoints.add(endpoint);
    }

    /**
     * @return {@link #rooms}
     */
    public List<RoomExecutable> getRooms()
    {
        return rooms;
    }

    /**
     * @param rooms sets the {@link #rooms}
     */
    public void setRooms(List<RoomExecutable> rooms)
    {
        this.rooms = rooms;
    }

    /**
     * @param room to be added to the {@link #rooms}
     */
    public void addRoom(RoomExecutable room)
    {
        rooms.add(room);
    }

    /**
     * @return {@link #connections}
     */
    public List<ConnectionExecutable> getConnections()
    {
        return connections;
    }

    /**
     * @param connections sets the {@link #connections}
     */
    public void setConnections(List<ConnectionExecutable> connections)
    {
        this.connections = connections;
    }

    /**
     * @param connection to be added to the {@link #connections}
     */
    public void addConnection(ConnectionExecutable connection)
    {
        connections.add(connection);
    }

    private static final String ENDPOINTS = "endpoints";
    private static final String ROOMS = "rooms";
    private static final String CONNECTIONS = "connections";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(ENDPOINTS, endpoints);
        dataMap.set(ROOMS, rooms);
        dataMap.set(CONNECTIONS, connections);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        endpoints = dataMap.getList(ENDPOINTS, EndpointExecutable.class);
        rooms = dataMap.getList(ROOMS, RoomExecutable.class);
        connections = dataMap.getList(CONNECTIONS, ConnectionExecutable.class);
    }
}
