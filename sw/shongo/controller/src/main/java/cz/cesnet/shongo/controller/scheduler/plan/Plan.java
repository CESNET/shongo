package cz.cesnet.shongo.controller.scheduler.plan;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.allocation.AllocatedCompartment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a scheduler plan for a single {@link AllocatedCompartment}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Plan
{
    /**
     * List of endpoints which are used in the plan.
     */
    List<Endpoint> endpoints = new ArrayList<Endpoint>();

    /**
     * List of connections which will be initiated in the plan.
     */
    List<Connection> connections = new ArrayList<Connection>();

    /**
     * Add new endpoint to the plan.
     *
     * @param endpoint
     */
    public void addEndpoint(Endpoint endpoint)
    {
        endpoints.add(endpoint);
    }

    /**
     * Add new connection which should be initiated by this endpoint.
     *
     * @param endpointFrom
     * @param endpointTo
     */
    public void addConnection(Endpoint endpointFrom, Endpoint endpointTo)
    {
        Set<Technology> technologies = new HashSet<Technology>(endpointFrom.getSupportedTechnologies());
        technologies.retainAll(endpointTo.getSupportedTechnologies());
        if (technologies.size() == 0) {
            throw new IllegalArgumentException("Technologies must not be empty!");
        }

        // TODO: Select prefered technology
        Technology technology = technologies.iterator().next();

        Connection connection = new Connection(endpointFrom, endpointTo, technology);
        connections.add(connection);
    }

    /**
     * @return {@link #endpoints}
     */
    public List<Endpoint> getEndpoints()
    {
        return endpoints;
    }

    /**
     * @return {@link #connections}
     */
    public List<Connection> getConnections()
    {
        return connections;
    }
}
