package cz.cesnet.shongo.controller.scheduler;

import com.jgraph.layout.JGraphFacade;
import com.jgraph.layout.graph.JGraphSimpleLayout;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.Cache;
import cz.cesnet.shongo.controller.allocation.*;
import cz.cesnet.shongo.controller.cache.AvailableAlias;
import cz.cesnet.shongo.controller.cache.AvailableVirtualRoom;
import cz.cesnet.shongo.controller.report.Report;
import cz.cesnet.shongo.controller.report.ReportException;
import cz.cesnet.shongo.controller.request.CallInitiation;
import cz.cesnet.shongo.controller.request.ExistingEndpointSpecification;
import cz.cesnet.shongo.controller.request.ExternalEndpointSpecification;
import cz.cesnet.shongo.controller.request.LookupEndpointSpecification;
import cz.cesnet.shongo.controller.resource.Alias;
import cz.cesnet.shongo.controller.resource.AliasProviderCapability;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.resource.Resource;
import cz.cesnet.shongo.controller.scheduler.report.*;
import cz.cesnet.shongo.fault.FaultException;
import org.jgraph.JGraph;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.ext.JGraphModelAdapter;
import org.jgrapht.graph.SimpleGraph;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static Logger logger = LoggerFactory.getLogger(Task.class);

    /**
     * List of reports.
     */
    private List<Report> reports = new ArrayList<Report>();

    /**
     * Interval for which the task is performed.
     */
    private Interval interval;

    /**
     * @see Cache
     */
    private Cache cache;

    /**
     * @see {@link Cache.Transaction}
     */
    private Cache.Transaction cacheTransaction;

    /**
     * List of all {@link AllocatedItem} added to the task.
     */
    private List<AllocatedItem> allocatedItems = new ArrayList<AllocatedItem>();

    /**
     * List of all {@link AllocatedEndpoint} added to the task.
     */
    private List<AllocatedEndpoint> allocatedEndpoints = new ArrayList<AllocatedEndpoint>();

    /**
     * Total sum of endpoints (calculated as sum of {@link AllocatedEndpoint#getCount()} because one
     * {@link AllocatedEndpoint} can represent multiple endpoints).
     */
    private int totalAllocatedEndpointCount;

    /**
     * Map of call initiations by {@link AllocatedEndpoint}.
     */
    private Map<AllocatedEndpoint, CallInitiation> callInitiationByAllocatedItem =
            new HashMap<AllocatedEndpoint, CallInitiation>();

    /**
     * Default call initiation.
     */
    private CallInitiation callInitiation = CallInitiation.TERMINAL;

    /**
     * Graph of connectivity between endpoints.
     */
    private UndirectedGraph<AllocatedEndpoint, ConnectivityEdge> connectivityGraph;

    /**
     * Constructor.
     *
     * @param interval sets the {@link #interval}
     * @param cache    sets the {@link #cache}
     */
    public Task(Interval interval, Cache cache)
    {
        clear();

        this.interval = interval;
        this.cache = cache;
    }

    /**
     * @return {@link #reports}
     */
    public List<Report> getReports()
    {
        return reports;
    }

    /**
     * @param callInitiation sets the {@link #callInitiation}
     */
    public void setCallInitiation(CallInitiation callInitiation)
    {
        if (callInitiation == null) {
            throw new IllegalArgumentException("Call initiation must not be null.");
        }
        this.callInitiation = callInitiation;
    }

    /**
     * Clear all information added to the task.
     */
    public void clear()
    {
        cacheTransaction = new Cache.Transaction();
        allocatedItems.clear();
        allocatedEndpoints.clear();
        totalAllocatedEndpointCount = 0;
        connectivityGraph = new SimpleGraph<AllocatedEndpoint, ConnectivityEdge>(ConnectivityEdge.class);
    }

    /**
     * @param report to be added to the {@link #reports}
     */
    private void addReport(Report report)
    {
        reports.add(report);
    }

    /**
     * Add resource to the task
     *
     * @param resourceSpecification
     */
    public void addResource(ResourceSpecification resourceSpecification) throws ReportException
    {
        if (resourceSpecification instanceof ExternalEndpointSpecification) {
            AllocatedExternalEndpoint allocatedExternalEndpoint =
                    new AllocatedExternalEndpoint((ExternalEndpointSpecification) resourceSpecification);
            addAllocatedItem(allocatedExternalEndpoint, resourceSpecification.getCallInitiation());
        }
        else if (resourceSpecification instanceof ExistingEndpointSpecification) {
            ExistingEndpointSpecification existingResource = (ExistingEndpointSpecification) resourceSpecification;
            Resource resource = existingResource.getResource();
            if (cacheTransaction.containsResource(resource)) {
                // Same resource is requested multiple times
                throw new ResourceRequestedMultipleTimesReport(resource).exception();
            }
            if (!resource.isAllocatable()) {
                // Requested resource cannot be allocated
                throw new ResourceNotAllocatableReport(resource).exception();
            }
            if (!cache.isResourceAvailable(resource, interval, cacheTransaction)) {
                // Requested resource is not available in requested slot
                throw new ResourceNotAvailableReport(resource).exception();
            }
            addAllocatedItemByResource(resource, resourceSpecification.getCallInitiation());
        }
        else if (resourceSpecification instanceof LookupEndpointSpecification) {
            LookupEndpointSpecification lookupResource = (LookupEndpointSpecification) resourceSpecification;
            Set<Technology> technologies = lookupResource.getTechnologies();

            // Lookup device resources
            List<DeviceResource> deviceResources = cache.findAvailableTerminal(interval, technologies,
                    cacheTransaction);

            // Select first available device resource
            // TODO: Select best resource based on some criteria
            DeviceResource deviceResource = null;
            for (DeviceResource possibleDeviceResource : deviceResources) {
                deviceResource = possibleDeviceResource;
                break;
            }

            // If some was found
            if (deviceResource != null) {
                addAllocatedItemByResource(deviceResource, resourceSpecification.getCallInitiation());
            }
            else {
                throw new ResourceNotFoundReport(technologies).exception();
            }
        }
        else {
            throw new IllegalStateException(String.format("Allocation of '%s' resource is not implemented yet.",
                    resourceSpecification.getClass()));
        }
    }

    /**
     * Add requested {@code allocatedItem} to task.
     *
     * @param allocatedItem
     * @param callInitiation
     */
    public void addAllocatedItem(AllocatedItem allocatedItem, CallInitiation callInitiation)
    {
        allocatedItems.add(allocatedItem);
        cacheTransaction.addAllocationItem(allocatedItem);

        // Setup endpoints
        if (allocatedItem instanceof AllocatedEndpoint) {
            AllocatedEndpoint allocatedEndpoint = (AllocatedEndpoint) allocatedItem;
            allocatedEndpoints.add(allocatedEndpoint);
            totalAllocatedEndpointCount += allocatedEndpoint.getCount();
            if (callInitiation != null) {
                callInitiationByAllocatedItem.put(allocatedEndpoint, callInitiation);
            }

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
     * Add requested {@code allocatedItem} to task.
     *
     * @param allocatedItem
     */
    public void addAllocatedItem(AllocatedItem allocatedItem)
    {
        addAllocatedItem(allocatedItem, null);
    }

    /**
     * Create {@link AllocatedItem} from given {@code resource} and pass it to
     * the {@link #addAllocatedItem(AllocatedItem, CallInitiation)}.
     *
     * @param resource
     */
    private void addAllocatedItemByResource(Resource resource, CallInitiation callInitiation)
    {
        if (resource instanceof DeviceResource) {
            AllocatedDevice allocatedDevice = new AllocatedDevice();
            allocatedDevice.setResource(resource);
            addAllocatedItem(allocatedDevice, callInitiation);
        }
        else {
            AllocatedResource allocatedResource = new AllocatedResource();
            allocatedResource.setResource(resource);
            addAllocatedItem(allocatedResource, callInitiation);
        }

        // Add parent resource
        Resource parentResource = resource.getParentResource();
        if (parentResource != null && !cacheTransaction.containsResource(parentResource)) {
            addAllocatedItemByResource(parentResource, callInitiation);
        }
    }

    /**
     * Add new connection to the given {@code allocatedCompartment}.
     *
     * @param allocatedCompartment
     * @param allocatedEndpointFrom
     * @param allocatedEndpointTo
     */
    private void addConnectionToAllocatedCompartment(AllocatedCompartment allocatedCompartment,
            AllocatedEndpoint allocatedEndpointFrom, AllocatedEndpoint allocatedEndpointTo) throws ReportException
    {
        // Determine call initiation from given endpoints
        CallInitiation callInitiation = determineCallInitiation(allocatedEndpointFrom, allocatedEndpointTo);

        // Change preferred order of endpoints based on call initiation
        switch (callInitiation) {
            case VIRTUAL_ROOM:
                // If the call should be initiated by a virtual room and it is the second endpoint, exchange them
                if (!(allocatedEndpointFrom instanceof AllocatedVirtualRoom) && allocatedEndpointTo instanceof AllocatedVirtualRoom) {
                    AllocatedEndpoint allocatedEndpointTemp = allocatedEndpointFrom;
                    allocatedEndpointFrom = allocatedEndpointTo;
                    allocatedEndpointTo = allocatedEndpointTemp;
                }
                break;
            case TERMINAL:
                // If the call should be initiated by a terminal and it is the second endpoint, exchange them
                if (allocatedEndpointFrom instanceof AllocatedVirtualRoom && !(allocatedEndpointTo instanceof AllocatedVirtualRoom)) {
                    AllocatedEndpoint allocatedEndpointTemp = allocatedEndpointFrom;
                    allocatedEndpointFrom = allocatedEndpointTo;
                    allocatedEndpointTo = allocatedEndpointTemp;
                }
                break;
            default:
                throw new IllegalStateException("Unknown call initiation '" + callInitiation.toString() + "'.");
        }

        // Determine technology by which the resources will connect
        Technology technology = null;
        Set<Technology> technologies = new HashSet<Technology>(allocatedEndpointFrom.getSupportedTechnologies());
        technologies.retainAll(allocatedEndpointTo.getSupportedTechnologies());
        switch (technologies.size()) {
            case 0:
                // No common technology
                throw new IllegalArgumentException(
                        "Cannot connect endpoints because they doesn't have any common technology!");
            case 1:
                // One common technology
                technology = technologies.iterator().next();
                break;
            default:
                // Multiple common technologies, thus determine preferred technology
                Technology preferredTechnology = null;
                if (allocatedEndpointFrom instanceof AllocatedDevice) {
                    AllocatedDevice allocatedDevice = (AllocatedDevice) allocatedEndpointFrom;
                    preferredTechnology = allocatedDevice.getDeviceResource().getPreferredTechnology();
                }
                if (preferredTechnology == null && allocatedEndpointTo instanceof AllocatedDevice) {
                    AllocatedDevice allocatedDevice = (AllocatedDevice) allocatedEndpointTo;
                    preferredTechnology = allocatedDevice.getDeviceResource().getPreferredTechnology();
                }
                // Use preferred technology
                if (technologies.contains(preferredTechnology)) {
                    technology = preferredTechnology;
                }
                else {
                    technology = technologies.iterator().next();
                }
        }

        Report connection = new CreatingConnectionBetweenReport(allocatedEndpointFrom, allocatedEndpointTo, technology);
        try {
            addConnectionToAllocatedCompartment(allocatedCompartment, allocatedEndpointFrom, allocatedEndpointTo,
                    technology);
        }
        catch (ReportException firstException) {
            connection.addChildMessage(firstException.getReport());
            try {
                addConnectionToAllocatedCompartment(allocatedCompartment, allocatedEndpointTo, allocatedEndpointFrom,
                        technology);
            }
            catch (ReportException secondException) {
                connection.addChildMessage(secondException.getReport());
                Report connectionFailed = new CannotCreateConnectionBetweenReport(allocatedEndpointFrom,
                        allocatedEndpointTo);
                connectionFailed.addChildMessage(connection);
                throw connectionFailed.exception();
            }
        }
        addReport(connection);
    }

    /**
     * Add new connection to the given {@code allocatedCompartment}.
     *
     * @param allocatedCompartment
     * @param allocatedEndpointFrom
     * @param allocatedEndpointTo
     * @param technology
     * @throws FaultException
     */
    private void addConnectionToAllocatedCompartment(AllocatedCompartment allocatedCompartment,
            AllocatedEndpoint allocatedEndpointFrom, AllocatedEndpoint allocatedEndpointTo, Technology technology)
            throws ReportException
    {
        // Created connection
        Connection connection = null;

        // TODO: implement connections to multiple endpoints
        if (allocatedEndpointTo.getCount() > 1) {
            throw new CannotCreateConnectionFromToMultipleReport(allocatedEndpointFrom, allocatedEndpointTo).exception();
        }

        // Find existing alias for connection
        Alias alias = null;
        List<Alias> aliases = allocatedEndpointTo.getAssignedAliases();
        for (Alias possibleAlias : aliases) {
            if (possibleAlias.getTechnology().equals(technology)) {
                alias = possibleAlias;
                break;
            }
        }
        // Create connection by alias
        if (alias != null) {
            ConnectionByAlias connectionByAlias = new ConnectionByAlias();
            connectionByAlias.setAlias(alias);
            connection = connectionByAlias;
        }
        // Create connection by address
        else if (technology.isAllowedConnectionByAddress() && allocatedEndpointTo.getAddress() != null) {
            ConnectionByAddress connectionByAddress = new ConnectionByAddress();
            connectionByAddress.setAddress(allocatedEndpointTo.getAddress());
            connection = connectionByAddress;
        }
        else {
            // Allocated alias for the target endpoint
            try {
                AllocatedAlias allocatedAlias = allocateAlias((AllocatedItem) allocatedEndpointTo, technology);
                allocatedEndpointTo.assignAlias(allocatedAlias.getAlias());
                allocatedCompartment.addAllocatedItem(allocatedAlias);
                // Create connection by the created alias
                ConnectionByAlias connectionByAlias = new ConnectionByAlias();
                connectionByAlias.setAlias(allocatedAlias.getAlias());
                connection = connectionByAlias;
            } catch (ReportException exception) {
                Report report = new CannotCreateConnectionFromToReport(allocatedEndpointFrom, allocatedEndpointTo);
                report.addChildMessage(exception.getReport());
                throw report.exception();
            }
        }

        if (connection == null) {
            throw new CannotCreateConnectionFromToReport(allocatedEndpointFrom, allocatedEndpointTo).exception();
        }

        connection.setAllocatedEndpointFrom((AllocatedItem) allocatedEndpointFrom);
        connection.setAllocatedEndpointTo((AllocatedItem) allocatedEndpointTo);
        allocatedCompartment.addConnection(connection);
    }

    /**
     * Allocate new {@link Alias} for given {@code allocatedItemTo}.
     *
     * @param allocatedItem
     * @param technology
     * @return
     * @throws FaultException
     */
    private AllocatedAlias allocateAlias(AllocatedItem allocatedItem, Technology technology) throws ReportException
    {
        AvailableAlias availableAlias = null;
        // First try to allocate alias from a resource capabilities
        if (allocatedItem instanceof AllocatedResource) {
            AllocatedResource allocatedResource = (AllocatedResource) allocatedItem;
            Resource resource = allocatedResource.getResource();
            List<AliasProviderCapability> aliasProviderCapabilities =
                    resource.getCapabilities(AliasProviderCapability.class);
            for (AliasProviderCapability aliasProviderCapability : aliasProviderCapabilities) {
                if (aliasProviderCapability.getTechnology().equals(technology)) {
                    availableAlias = cache.getAvailableAlias(aliasProviderCapability, interval, cacheTransaction);
                }
            }
        }
        // Allocate alias from all resources in the cache
        if (availableAlias == null) {
            availableAlias = cache.getAvailableAlias(cacheTransaction, technology, interval);
        }
        if (availableAlias == null) {
            throw new NoAvailableAliasReport(technology).exception();
        }
        AllocatedAlias allocatedAlias = new AllocatedAlias();
        allocatedAlias.setAliasProviderCapability(availableAlias.getAliasProviderCapability());
        allocatedAlias.setAlias(availableAlias.getAlias());
        allocatedAlias.setSlot(interval);

        cacheTransaction.addAllocationItem(allocatedAlias);

        return allocatedAlias;
    }

    /**
     * @param endpointFrom
     * @param endpointTo
     * @return call initiation from given {@link AllocatedItem}s
     */
    private CallInitiation determineCallInitiation(AllocatedEndpoint endpointFrom, AllocatedEndpoint endpointTo)
    {
        CallInitiation callInitiation = null;
        CallInitiation callInitiationFrom = callInitiationByAllocatedItem.get(endpointFrom);
        CallInitiation callInitiationTo = callInitiationByAllocatedItem.get(endpointTo);
        if (callInitiationFrom != null) {
            callInitiation = callInitiationFrom;
        }
        if (callInitiationTo != null) {
            if (callInitiation == null) {
                callInitiation = callInitiationTo;
            }
            else if (callInitiation != callInitiationTo) {
                // Rewrite call initiation only when the second endpoint isn't virtual room and it want to be called
                // from the virtual room
                if (!(endpointTo instanceof AllocatedVirtualRoom) && callInitiationTo == CallInitiation.VIRTUAL_ROOM) {
                    callInitiation = callInitiationTo;
                }
            }
        }
        // If no call initiation was specified for the endpoints, use the default
        if (callInitiation == null) {
            callInitiation = this.callInitiation;
        }
        return callInitiation;
    }

    /**
     * Find plan for connecting endpoints without virtual room.
     *
     * @return plan if possible, null otherwise
     */
    private AllocatedCompartment createNoVirtualRoomAllocatedCompartment() throws ReportException
    {
        // Maximal two endpoints may be connected without virtual room
        if (totalAllocatedEndpointCount > 2 || allocatedEndpoints.size() > 2) {
            return null;
        }

        // Two endpoints must be standalone and interconnectable
        AllocatedEndpoint endpointFrom = null;
        AllocatedEndpoint endpointTo = null;
        if (allocatedEndpoints.size() == 2) {
            if (totalAllocatedEndpointCount != 2) {
                throw new IllegalStateException();
            }
            endpointFrom = allocatedEndpoints.get(0);
            endpointTo = allocatedEndpoints.get(1);

            // Check if endpoints are standalone
            if (!endpointFrom.isStandalone() || !endpointTo.isStandalone()) {
                return null;
            }

            // Check connectivity
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
        } else {
            // Only allocated resource is allowed
            AllocatedEndpoint allocatedEndpoint = allocatedEndpoints.get(0);
            if (!(allocatedEndpoint instanceof AllocatedResource)) {
                return null;
            }
        }

        // Create allocated compartment
        AllocatedCompartment allocatedCompartment = new AllocatedCompartment();
        for (AllocatedItem allocatedItem : allocatedItems) {
            allocatedItem.setSlot(interval);
            allocatedCompartment.addAllocatedItem(allocatedItem);
        }
        // Add connection between two standalone endpoints
        if (endpointFrom != null && endpointTo != null) {
            addConnectionToAllocatedCompartment(allocatedCompartment, endpointFrom, endpointTo);
        }
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
    private AllocatedCompartment createSingleVirtualRoomAllocatedCompartment() throws ReportException
    {
        Collection<Set<Technology>> technologySets = getSingleVirtualRoomPlanTechnologySets();

        // Get available virtual rooms
        List<AvailableVirtualRoom> availableVirtualRooms = cache.findAvailableVirtualRoomsByVariants(
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

        // Create allocated virtual room
        AllocatedVirtualRoom allocatedVirtualRoom = new AllocatedVirtualRoom();
        allocatedVirtualRoom.setSlot(interval);
        allocatedVirtualRoom.setResource(availableVirtualRoom.getDeviceResource());
        allocatedVirtualRoom.setPortCount(totalAllocatedEndpointCount);
        addReport(new AllocatingVirtualRoomReport(allocatedVirtualRoom));

        // Create allocated compartment
        AllocatedCompartment allocatedCompartment = new AllocatedCompartment();
        allocatedCompartment.addAllocatedItem(allocatedVirtualRoom);
        for (AllocatedItem allocatedItem : allocatedItems) {
            allocatedItem.setSlot(interval);
            allocatedCompartment.addAllocatedItem(allocatedItem);
            if (allocatedItem instanceof AllocatedEndpoint) {
                addConnectionToAllocatedCompartment(allocatedCompartment, allocatedVirtualRoom,
                        (AllocatedEndpoint) allocatedItem);
            }
        }
        return allocatedCompartment;
    }

    /**
     * @return created {@link AllocatedCompartment} if possible, null otherwise
     * @throws FaultException
     */
    public AllocatedCompartment createAllocatedCompartment() throws ReportException
    {
        if (totalAllocatedEndpointCount <= 1) {
            // Check whether an existing resource is requested
            boolean resourceRequested = false;
            for (AllocatedItem allocatedItem : allocatedItems) {
                if (allocatedItem instanceof AllocatedResource) {
                    resourceRequested = true;
                }
            }
            if (!resourceRequested) {
                throw new NotEnoughEndpointInCompartmentReport().exception();
            }
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

        throw new NoAvailableVirtualRoomReport(getSingleVirtualRoomPlanTechnologySets(), totalAllocatedEndpointCount)
                .exception();
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

    public void printReports()
    {

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
