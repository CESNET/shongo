package cz.cesnet.shongo.controller.scheduler;

import com.jgraph.layout.JGraphFacade;
import com.jgraph.layout.graph.JGraphSimpleLayout;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.ResourceDatabase;
import cz.cesnet.shongo.controller.allocation.*;
import cz.cesnet.shongo.controller.request.*;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.resource.Resource;
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
 * Represents a scheduler task for {@link CompartmentRequest} which results into {@link AllocatedCompartment}.
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
     * Set of already added resources to the task.
     */
    private Set<Resource> resources = new HashSet<Resource>();

    /**
     * List of all endpoints.
     */
    private List<AllocatedItem> allocatedItems = new ArrayList<AllocatedItem>();

    /**
     * List of all endpoints.
     */
    private List<AllocatedEndpoint> allocatedEndpoints = new ArrayList<AllocatedEndpoint>();

    /**
     * Total sum of endpoints (calculated as sum of {@link AllocatedEndpoint#getCount()}).
     */
    private int totalAllocatedEndpointCount;

    /**
     * Graph of connectivity between endpoints.
     */
    private UndirectedGraph<AllocatedEndpoint, ConnectivityEdge> connectivityGraph;

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
        resources.clear();
        allocatedItems.clear();
        allocatedEndpoints.clear();
        totalAllocatedEndpointCount = 0;
        connectivityGraph = new SimpleGraph<AllocatedEndpoint, ConnectivityEdge>(ConnectivityEdge.class);
    }

    /**
     * Add resource to the task
     *
     * @param resourceSpecification
     */
    public void addResource(ResourceSpecification resourceSpecification) throws FaultException
    {
        if (resourceSpecification instanceof ExternalEndpointSpecification) {
            AllocatedExternalEndpoint allocatedExternalEndpoint =
                    new AllocatedExternalEndpoint((ExternalEndpointSpecification) resourceSpecification);
            addAllocatedItem(allocatedExternalEndpoint);
        }
        else if (resourceSpecification instanceof ExistingResourceSpecification) {
            ExistingResourceSpecification existingResource = (ExistingResourceSpecification) resourceSpecification;
            Resource resource = existingResource.getResource();
            if (resources.contains(resource)) {
                // Same resource is requested multiple times
                throw new FaultException("Resource is requested multiple times in specified time slot:\n"
                        + "  Resource: %s",
                        resource.getId().toString());
            }
            if (!resource.isSchedulable()) {
                // Requested resource cannot be allocated
                throw new FaultException("Requested resource cannot be allocated (schedulable = false):\n"
                        + "  Resource: %s",
                        resource.getId().toString());
            }
            if (!resourceDatabase.isResourceAvailable(resource, interval)) {
                // Requested resource is not available in requested slot
                throw new FaultException("Requested resource is not available in specified time slot:\n"
                        + " Time Slot: %s\n"
                        + "  Resource: %s",
                        TemporalHelper.formatInterval(interval),
                        resource.getId().toString());
            }
            addAllocatedItemByResource(resource);
        }
        else if (resourceSpecification instanceof LookupResourceSpecification) {
            LookupResourceSpecification lookupResource = (LookupResourceSpecification) resourceSpecification;
            Set<Technology> technologies = lookupResource.getTechnologies();

            // Lookup device resources
            List<DeviceResource> deviceResources = resourceDatabase.findAvailableTerminal(interval, technologies);

            // Select first available device resource
            // TODO: Select best resource based on some criteria
            DeviceResource deviceResource = null;
            for (DeviceResource possibleDeviceResource : deviceResources) {
                if (resources.contains(possibleDeviceResource)) {
                    continue;
                }
                deviceResource = possibleDeviceResource;
                break;
            }

            // If some was found
            if (deviceResource != null) {
                addAllocatedItemByResource(deviceResource);
            }
            else {
                // Resource was not found
                StringBuilder builder = new StringBuilder();
                for (Technology technology : technologies) {
                    if (builder.length() > 0) {
                        builder.append(", ");
                    }
                    builder.append(technology.getName());
                }
                throw new FaultException(
                        "No available resource was found for the following specification:\n"
                                + "    Time Slot: %s\n"
                                + " Technologies: %s",
                        TemporalHelper.formatInterval(interval), builder.toString());
            }
        }
        else {
            throw new FaultException("Allocation of '%s' resource is not implemented yet.",
                    resourceSpecification.getClass());
        }
    }

    /**
     * Add requested endpoint to task.
     *
     * @param allocatedItem
     */
    public void addAllocatedItem(AllocatedItem allocatedItem)
    {
        allocatedItems.add(allocatedItem);

        // Setup endpoints
        if (allocatedItem instanceof AllocatedEndpoint) {
            AllocatedEndpoint allocatedEndpoint = (AllocatedEndpoint) allocatedItem;
            allocatedEndpoints.add(allocatedEndpoint);
            totalAllocatedEndpointCount += allocatedEndpoint.getCount();

            // Setup connectivity graph
            connectivityGraph.addVertex(allocatedEndpoint);
            for (AllocatedEndpoint existingEndpoint : connectivityGraph.vertexSet()) {
                if (existingEndpoint == allocatedEndpoint) {
                    continue;
                }
                Set<Technology> technologies = new HashSet<Technology>(allocatedEndpoint.getSupportedTechnologies());
                technologies.retainAll(existingEndpoint.getSupportedTechnologies());
                if (technologies.size() > 0) {
                    connectivityGraph.addEdge(allocatedEndpoint, existingEndpoint, new ConnectivityEdge(technologies));
                }
            }
        }
    }

    /**
     * Create {@link AllocatedItem} from given {@code resource} and pass it to
     * the {@link #addAllocatedItem(AllocatedItem)}.
     *
     * @param resource
     */
    private void addAllocatedItemByResource(Resource resource)
    {
        if (resource instanceof DeviceResource) {
            AllocatedDevice allocatedDevice = new AllocatedDevice();
            allocatedDevice.setResource(resource);
            addAllocatedItem(allocatedDevice);
        }
        else {
            AllocatedResource allocatedResource = new AllocatedResource();
            allocatedResource.setResource(resource);
            addAllocatedItem(allocatedResource);
        }
        resources.add(resource);
    }

    /**
     * Find plan for connecting endpoints without virtual room.
     *
     * @return plan if possible, null otherwise
     */
    private AllocatedCompartment createNoVirtualRoomAllocatedCompartment()
    {
        // Only two standalone endpoints may be connected without virtual room
        if (totalAllocatedEndpointCount != 2 || allocatedEndpoints.size() != 2) {
            return null;
        }
        for (AllocatedEndpoint endpoint : allocatedEndpoints) {
            if (!endpoint.isStandalone()) {
                return null;
            }
        }

        // Check connectivity
        AllocatedEndpoint endpointFrom = allocatedEndpoints.get(0);
        AllocatedEndpoint endpointTo = allocatedEndpoints.get(1);
        ConnectivityEdge connectivityEdge = connectivityGraph.getEdge(endpointFrom, endpointTo);
        if (connectivityEdge == null) {
            AllocatedEndpoint endpointTemp = endpointFrom;
            endpointFrom = endpointTo;
            endpointTo = endpointTemp;
            connectivityEdge = connectivityGraph.getEdge(endpointFrom, endpointTo);
            if (connectivityEdge == null) {
                return null;
            }
        }

        // Create allocated compartment
        AllocatedCompartment allocatedCompartment = new AllocatedCompartment();
        for (AllocatedItem allocatedItem : allocatedItems) {
            allocatedItem.setSlot(interval);
            allocatedCompartment.addAllocatedItem(allocatedItem);
        }
        allocatedCompartment.addConnection(endpointFrom, endpointTo);
        return allocatedCompartment;
    }

    /**
     * @return collection of technology sets which interconnects all endpoints
     */
    private Collection<Set<Technology>> getSingleVirtualRoomPlanTechnologySets()
    {
        List<Set<Technology>> technologiesList = new ArrayList<Set<Technology>>();
        for (AllocatedEndpoint endpoint : allocatedEndpoints) {
            technologiesList.add(endpoint.getSupportedTechnologies());
        }
        return Technology.interconnect(technologiesList);
    }

    /**
     * Find plan for connecting endpoints by a single virtual room
     *
     * @return plan if possible, null otherwise
     */
    private AllocatedCompartment createSingleVirtualRoomAllocatedCompartment()
    {
        Collection<Set<Technology>> technologySets = getSingleVirtualRoomPlanTechnologySets();

        // Get available virtual rooms
        List<AvailableVirtualRoom> availableVirtualRooms = resourceDatabase.findAvailableVirtualRoomsByVariants(
                interval, totalAllocatedEndpointCount, technologySets);
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

        // Create allocated compartment
        AllocatedCompartment allocatedCompartment = new AllocatedCompartment();
        AllocatedVirtualRoom allocatedVirtualRoom = new AllocatedVirtualRoom();
        allocatedVirtualRoom.setSlot(interval);
        allocatedVirtualRoom.setResource(availableVirtualRoom.getDeviceResource());
        allocatedVirtualRoom.setPortCount(totalAllocatedEndpointCount);
        allocatedCompartment.addAllocatedItem(allocatedVirtualRoom);
        for (AllocatedItem allocatedItem : allocatedItems) {
            allocatedItem.setSlot(interval);
            allocatedCompartment.addAllocatedItem(allocatedItem);
            if (allocatedItem instanceof AllocatedEndpoint) {
                allocatedCompartment.addConnection(allocatedVirtualRoom, (AllocatedEndpoint) allocatedItem);
            }
        }
        return allocatedCompartment;
    }

    /**
     * @return created {@link AllocatedCompartment} if possible, null otherwise
     * @throws FaultException
     */
    public AllocatedCompartment createAllocatedCompartment() throws FaultException
    {
        if (totalAllocatedEndpointCount <= 1) {
            throw new FaultException("At least two devices/ports must be requested.");
        }

        AllocatedCompartment noVirtualRoomAllocatedCompartment = createNoVirtualRoomAllocatedCompartment();
        if (noVirtualRoomAllocatedCompartment != null) {
            return noVirtualRoomAllocatedCompartment;
        }

        AllocatedCompartment singleVirtualRoomAllocatedCompartment = createSingleVirtualRoomAllocatedCompartment();
        if (singleVirtualRoomAllocatedCompartment != null) {
            return singleVirtualRoomAllocatedCompartment;
        }

        // TODO: Resolve multiple virtual rooms and/or gateways for connecting endpoints

        // No virtual rooms is available
        throw new FaultException("No single virtual room was found for following specification:\n"
                + "       Time slot: %s\n"
                + "      Technology: %s\n"
                + " Number of ports: %d",
                TemporalHelper.formatInterval(interval),
                Technology.formatTechnologySets(getSingleVirtualRoomPlanTechnologySets()),
                totalAllocatedEndpointCount);
    }

    /**
     * Show current connectivity graph in dialog
     */
    public void showConnectivityGraph()
    {
        JGraph graph = new JGraph(new JGraphModelAdapter<AllocatedEndpoint, ConnectivityEdge>(connectivityGraph));

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
