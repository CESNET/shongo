package cz.cesnet.shongo.controller.compartment;

import cz.cesnet.shongo.PersistentObject;

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
public class Compartment extends PersistentObject
{
    /**
     * List of {@link Endpoint} which participates in the {@link Compartment}.
     */
    private List<Endpoint> endpoints = new ArrayList<Endpoint>();

    /**
     * List of {@link Connection} which are established in the {@link Compartment}.
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
    public int getTotalEndpointCount()
    {
        return totalEndpointCount;
    }
}
