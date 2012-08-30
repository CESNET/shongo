package cz.cesnet.shongo.controller.scheduler.plan;

import com.jgraph.layout.JGraphFacade;
import com.jgraph.layout.graph.JGraphSimpleLayout;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.ResourceDatabase;
import cz.cesnet.shongo.controller.allocation.AvailableVirtualRoom;
import cz.cesnet.shongo.controller.request.CompartmentRequest;
import cz.cesnet.shongo.fault.FaultException;
import cz.cesnet.shongo.util.TemporalHelper;
import org.jgraph.JGraph;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.ext.JGraphModelAdapter;
import org.jgrapht.graph.SimpleGraph;
import org.joda.time.Interval;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.List;

/**
 * Represents a scheduler task for {@link CompartmentRequest} which results into {@link Plan}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Task
{
    /**
     * Interval for which the task is performed.
     */
    private Interval interval;

    /**
     * @see ResourceDatabase
     */
    private ResourceDatabase resourceDatabase;

    /**
     * List of all endpoints.
     */
    private List<Endpoint> endpoints = new ArrayList<Endpoint>();

    /**
     * Total sum of endpoints (calculated as sum of {@link Endpoint#getCount()}).
     */
    private int totalEndpointCount;

    /**
     * Graph of connectivity between endpoints.
     */
    private UndirectedGraph<Endpoint, ConnectivityEdge> endpointConnectivityGraph;

    /**
     * Constructor.
     *
     * @param interval         sets the {@link #interval}
     * @param resourceDatabase sets the {@link #resourceDatabase}
     */
    public Task(Interval interval, ResourceDatabase resourceDatabase)
    {
        clear();

        this.interval = interval;
        this.resourceDatabase = resourceDatabase;
    }

    /**
     * Clear all information added to the task.
     */
    public void clear()
    {
        endpoints.clear();
        totalEndpointCount = 0;
        endpointConnectivityGraph = new SimpleGraph<Endpoint, ConnectivityEdge>(ConnectivityEdge.class);
    }

    /**
     * Add endpoint to task.
     *
     * @param endpoint
     */
    public void addEndpoint(Endpoint endpoint)
    {
        endpoints.add(endpoint);
        totalEndpointCount += endpoint.getCount();

        // Setup connectivity graph
        endpointConnectivityGraph.addVertex(endpoint);
        for (Endpoint existingEndpoint : endpointConnectivityGraph.vertexSet()) {
            if (existingEndpoint == endpoint) {
                continue;
            }
            Set<Technology> technologies = new HashSet<Technology>(endpoint.getSupportedTechnologies());
            technologies.retainAll(existingEndpoint.getSupportedTechnologies());
            if (technologies.size() > 0) {
                endpointConnectivityGraph.addEdge(endpoint, existingEndpoint, new ConnectivityEdge(technologies));
            }
        }
    }

    /**
     * Find plan for connecting endpoints without virtual room.
     *
     * @return plan if possible, null otherwise
     */
    private Plan findNoVirtualRoomPlan()
    {
        // Only two standalone endpoint may be connected without virtual room
        if (totalEndpointCount != 2 || endpoints.size() != 2) {
            return null;
        }
        for (Endpoint endpoint : endpoints) {
            if (!endpoint.isStandalone()) {
                return null;
            }
        }

        // Check connectivity
        Endpoint endpointFrom = endpoints.get(0);
        Endpoint endpointTo = endpoints.get(1);
        ConnectivityEdge connectivityEdge = endpointConnectivityGraph.getEdge(endpointFrom, endpointTo);
        if (connectivityEdge == null) {
            Endpoint endpointTemp = endpointFrom;
            endpointFrom = endpointTo;
            endpointTo = endpointTemp;
            connectivityEdge = endpointConnectivityGraph.getEdge(endpointFrom, endpointTo);
            if (connectivityEdge == null) {
                return null;
            }
        }

        // Create plan
        Plan plan = new Plan();
        plan.addEndpoint(endpointFrom);
        plan.addEndpoint(endpointTo);
        plan.addConnection(endpointFrom, endpointTo);
        return plan;
    }

    /**
     * @return collection of technology sets which interconnects all endpoints
     */
    private Collection<Set<Technology>> getSingleVirtualRoomPlanTechnologySets()
    {
        List<Set<Technology>> technologiesList = new ArrayList<Set<Technology>>();
        for (Endpoint endpoint : endpoints) {
            technologiesList.add(endpoint.getSupportedTechnologies());
        }
        return Technology.interconnect(technologiesList);
    }

    /**
     * Find plan for connecting endpoints by a single virtual room
     *
     * @return plan if possible, null otherwise
     */
    private Plan findSingleVirtualRoomPlan()
    {
        Collection<Set<Technology>> technologySets = getSingleVirtualRoomPlanTechnologySets();

        // Get available virtual rooms
        List<AvailableVirtualRoom> availableVirtualRooms = resourceDatabase.findAvailableVirtualRoomsByVariants(
                interval, totalEndpointCount, technologySets);
        if (availableVirtualRooms.size() == 0) {

            return null;

        }
        // Sort virtual rooms from the most filled to the least filled
        Collections.sort(availableVirtualRooms, new Comparator<AvailableVirtualRoom>()
        {
            @Override
            public int compare(AvailableVirtualRoom first, AvailableVirtualRoom second)
            {
                return -Double.valueOf(first.getFullnessRatio()).compareTo(second.getFullnessRatio());
            }
        });
        // Get the first virtual room
        AvailableVirtualRoom availableVirtualRoom = availableVirtualRooms.get(0);

        // Create plan
        Plan plan = new Plan();
        VirtualRoom virtualRoom = new VirtualRoom(availableVirtualRoom.getDeviceResource(), totalEndpointCount);
        plan.addEndpoint(virtualRoom);
        for (Endpoint endpoint : endpoints) {
            plan.addEndpoint(endpoint);
            plan.addConnection(virtualRoom, endpoint);
        }
        return plan;
    }

    /**
     * @return found {@link Plan}
     * @throws FaultException
     */
    public Plan findPlan() throws FaultException
    {
        if (totalEndpointCount <= 1) {
            throw new FaultException("At least two devices/ports must be requested.");
        }

        Plan noVirtualRoomPlan = findNoVirtualRoomPlan();
        if (noVirtualRoomPlan != null) {
            return noVirtualRoomPlan;
        }

        Plan singleVirtualRoomPlan = findSingleVirtualRoomPlan();
        if (singleVirtualRoomPlan != null) {
            return singleVirtualRoomPlan;
        }

        // TODO: Resolve multiple virtual rooms and/or gateways for connecting endpoints

        // No virtual rooms is available
        throw new FaultException("No single virtual room was found for following specification:\n"
                + "       Time slot: %s\n"
                + "      Technology: %s\n"
                + " Number of ports: %d",
                TemporalHelper.formatInterval(interval),
                Technology.formatTechnologySets(getSingleVirtualRoomPlanTechnologySets()),
                totalEndpointCount);
    }

    /**
     * Show current connectivity graph in dialog
     */
    public void showConnectivityGraph()
    {
        JGraph graph = new JGraph(new JGraphModelAdapter(endpointConnectivityGraph));

        JGraphFacade graphFacade = new JGraphFacade(graph, graph.getSelectionCells());
        graphFacade.setIgnoresUnconnectedCells(true);
        graphFacade.setIgnoresCellsInGroups(true);
        graphFacade.setIgnoresHiddenCells(true);
        graphFacade.setDirected(false);
        graphFacade.resetControlPoints();

        JGraphSimpleLayout graphLayout = new JGraphSimpleLayout(JGraphSimpleLayout.TYPE_CIRCLE);
        graphLayout.run(graphFacade);

        Dimension dimension = new Dimension(graphLayout.getMaxx(), graphLayout.getMaxy());
        Rectangle2D bounds = graphFacade.getCellBounds();
        dimension.setSize(bounds.getWidth(), bounds.getHeight());
        dimension.setSize(dimension.getWidth() + 50, dimension.getHeight() + 80);

        Map nested = graphFacade.createNestedMap(true, true);
        graph.getGraphLayoutCache().edit(nested);

        JDialog dialog = new JDialog();
        dialog.getContentPane().add(graph);
        dialog.setSize(dimension);
        dialog.setModal(true);
        dialog.setVisible(true);
    }

    /**
     * Represents an edge in the connectivity graph of endpoints.
     */
    private static class ConnectivityEdge
    {
        /**
         * Technologies by which two endpoints can be connected.
         */
        private Set<Technology> technologies;

        /**
         * @param technologies sets the {@link #technologies}
         */
        public ConnectivityEdge(Set<Technology> technologies)
        {
            this.technologies = technologies;
        }

        /**
         * @return {@link #technologies}
         */
        public Set<Technology> getTechnologies()
        {
            return technologies;
        }

        @Override
        public String toString()
        {
            return Technology.formatTechnologies(technologies);
        }
    }
}
