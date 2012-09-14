package cz.cesnet.shongo.controller.scheduler;

import com.jgraph.layout.JGraphFacade;
import com.jgraph.layout.graph.JGraphSimpleLayout;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.cache.AvailableVirtualRoom;
import cz.cesnet.shongo.controller.compartment.*;
import cz.cesnet.shongo.controller.report.Report;
import cz.cesnet.shongo.controller.report.ReportException;
import cz.cesnet.shongo.controller.request.*;
import cz.cesnet.shongo.controller.reservation.AliasReservation;
import cz.cesnet.shongo.controller.reservation.CompartmentReservation;
import cz.cesnet.shongo.controller.reservation.EndpointReservation;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.resource.Alias;
import cz.cesnet.shongo.controller.resource.Resource;
import cz.cesnet.shongo.controller.scheduler.report.*;
import cz.cesnet.shongo.fault.TodoImplementException;
import org.jgraph.JGraph;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.ext.JGraphModelAdapter;
import org.jgrapht.graph.SimpleGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.List;

/**
 * Represents {@link ReservationTask} for a {@link CompartmentSpecification}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class CompartmentReservationTask extends ReservationTask<CompartmentSpecification, CompartmentReservation>
{
    private static Logger logger = LoggerFactory.getLogger(CompartmentReservationTask.class);

    /**
     * {@link Compartment} which is created by the {@link CompartmentReservationTask}.
     */
    private Compartment compartment = new Compartment();

    /**
     * Map of call initiations by {@link Endpoint}.
     */
    private Map<Endpoint, CallInitiation> callInitiationByAllocatedItem =
            new HashMap<Endpoint, CallInitiation>();

    /**
     * Graph of connectivity between endpoints.
     */
    private UndirectedGraph<Endpoint, ConnectivityEdge> connectivityGraph =
            new SimpleGraph<Endpoint, ConnectivityEdge>(ConnectivityEdge.class);

    /**
     * Constructor.
     *
     * @param context sets the {@link #context}
     */
    public CompartmentReservationTask(Context context)
    {
        super(new CompartmentSpecification(), context);
    }

    /**
     * Constructor.
     *
     * @param context        sets the {@link #context}
     * @param callInitiation sets the default {@link CallInitiation}
     */
    public CompartmentReservationTask(Context context, CallInitiation callInitiation)
    {
        super(new CompartmentSpecification(callInitiation), context);
    }

    /**
     * Constructor.
     *
     * @param specification sets the {@link #specification}
     * @param context       sets the {@link #context}
     */
    public CompartmentReservationTask(CompartmentSpecification specification, Context context)
    {
        super(specification, context);
    }

    /**
     * @return default {@link CallInitiation}
     */
    public CallInitiation getCallInitiation()
    {
        CallInitiation callInitiation = getSpecification().getCallInitiation();
        if (callInitiation == null) {
            callInitiation = CallInitiation.TERMINAL;
        }
        return callInitiation;
    }

    @Override
    protected void addChildReservation(Reservation reservation, Specification specification)
    {
        super.addChildReservation(reservation, specification);

        if (reservation instanceof EndpointReservation) {
            EndpointReservation endpointReservation = (EndpointReservation) reservation;
            EndpointSpecification endpointSpecification = (EndpointSpecification) specification;
            Endpoint endpoint = endpointReservation.getEndpoint();

            compartment.addEndpoint(endpoint);

            CallInitiation callInitiation = endpointSpecification.getCallInitiation();
            if (callInitiation != null) {
                callInitiationByAllocatedItem.put(endpoint, callInitiation);
            }

            // Setup connectivity graph
            connectivityGraph.addVertex(endpoint);
            for (Endpoint existingEndpoint : connectivityGraph.vertexSet()) {
                if (existingEndpoint == endpoint) {
                    continue;
                }
                Set<Technology> technologies = new HashSet<Technology>(endpoint.getSupportedTechnologies());
                technologies.retainAll(existingEndpoint.getSupportedTechnologies());
                if (technologies.size() > 0) {
                    connectivityGraph.addEdge(endpoint, existingEndpoint, new ConnectivityEdge(technologies));
                }
            }
        }
    }

    /**
     * Add new connection to the {@link #compartment}.
     *
     * @param endpointFrom
     * @param endpointTo
     */
    private void addConnection(Endpoint endpointFrom, Endpoint endpointTo) throws ReportException
    {
        // Determine call initiation from given endpoints
        CallInitiation callInitiation = determineCallInitiation(endpointFrom, endpointTo);

        // Change preferred order of endpoints based on call initiation
        switch (callInitiation) {
            case VIRTUAL_ROOM:
                // If the call should be initiated by a virtual room and it is the second endpoint, exchange them
                if (!(endpointFrom instanceof VirtualRoom) && endpointTo instanceof VirtualRoom) {
                    Endpoint endpointTmp = endpointFrom;
                    endpointFrom = endpointTo;
                    endpointTo = endpointTmp;
                }
                break;
            case TERMINAL:
                // If the call should be initiated by a terminal and it is the second endpoint, exchange them
                if (endpointFrom instanceof VirtualRoom && !(endpointTo instanceof VirtualRoom)) {
                    Endpoint endpointTmp = endpointFrom;
                    endpointFrom = endpointTo;
                    endpointTo = endpointTmp;
                }
                break;
            default:
                throw new IllegalStateException("Unknown call initiation '" + callInitiation.toString() + "'.");
        }

        // Determine technology by which the resources will connect
        Technology technology = null;
        Set<Technology> technologies = new HashSet<Technology>(endpointFrom.getSupportedTechnologies());
        technologies.retainAll(endpointTo.getSupportedTechnologies());
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
                if (endpointFrom instanceof DeviceResourceEndpoint) {
                    DeviceResourceEndpoint deviceResourceEndpoint = (DeviceResourceEndpoint) endpointFrom;
                    preferredTechnology = deviceResourceEndpoint.getDeviceResource().getPreferredTechnology();
                }
                if (preferredTechnology == null && endpointTo instanceof DeviceResourceEndpoint) {
                    DeviceResourceEndpoint deviceResourceEndpoint = (DeviceResourceEndpoint) endpointTo;
                    preferredTechnology = deviceResourceEndpoint.getDeviceResource().getPreferredTechnology();
                }
                // Use preferred technology
                if (technologies.contains(preferredTechnology)) {
                    technology = preferredTechnology;
                }
                else {
                    technology = technologies.iterator().next();
                }
        }

        Report connection = new CreatingConnectionBetweenReport(endpointFrom, endpointTo, technology);
        try {
            addConnection(endpointFrom, endpointTo, technology);
        }
        catch (ReportException firstException) {
            connection.addChildMessage(firstException.getReport());
            try {
                addConnection(endpointTo, endpointFrom, technology);
            }
            catch (ReportException secondException) {
                connection.addChildMessage(secondException.getReport());
                Report connectionFailed = new CannotCreateConnectionBetweenReport(endpointFrom, endpointTo);
                connectionFailed.addChildMessage(connection);
                throw connectionFailed.exception();
            }
        }
        addReport(connection);
    }

    /**
     * Add new connection to the given {@code reservation}.
     *
     * @param endpointFrom
     * @param endpointTo
     * @param technology
     * @throws ReportException
     */
    private void addConnection(Endpoint endpointFrom, Endpoint endpointTo, Technology technology)
            throws ReportException
    {
        // Created connection
        Connection connection = null;

        // TODO: implement connections to multiple endpoints
        if (endpointTo.getCount() > 1) {
            throw new CannotCreateConnectionFromToMultipleReport(endpointFrom, endpointTo).exception();
        }

        // Find existing alias for connection
        Alias alias = null;
        List<Alias> aliases = endpointTo.getAssignedAliases();
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
        else if (technology.isAllowedConnectionByAddress() && endpointTo.getAddress() != null) {
            ConnectionByAddress connectionByAddress = new ConnectionByAddress();
            connectionByAddress.setAddress(endpointTo.getAddress());
            connection = connectionByAddress;
        }
        else {
            // Allocate alias for the target endpoint
            try {
                Resource resource = null;
                if (endpointTo instanceof DeviceResourceEndpoint) {
                    DeviceResourceEndpoint deviceResourceEndpoint = (DeviceResourceEndpoint) endpointTo;
                    resource = deviceResourceEndpoint.getDeviceResource();
                }
                AliasSpecification aliasSpecification = new AliasSpecification(technology, resource);
                AliasReservation aliasReservation = addChildSpecification(aliasSpecification, AliasReservation.class);

                // Assign alias to endpoint
                endpointTo.assignAlias(aliasReservation.getAlias());

                // Create connection by the created alias
                ConnectionByAlias connectionByAlias = new ConnectionByAlias();
                connectionByAlias.setAlias(aliasReservation.getAlias());
                connection = connectionByAlias;
            }
            catch (ReportException exception) {
                Report report = new CannotCreateConnectionFromToReport(endpointFrom, endpointTo);
                report.addChildMessage(exception.getReport());
                throw report.exception();
            }
        }

        if (connection == null) {
            throw new CannotCreateConnectionFromToReport(endpointFrom, endpointTo).exception();
        }

        connection.setEndpointFrom(endpointFrom);
        connection.setEndpointTo(endpointTo);
        compartment.addConnection(connection);
    }

    /**
     * @param endpointFrom first {@link Endpoint}
     * @param endpointTo   second {@link Endpoint}
     * @return {@link CallInitiation} from given {@link Endpoint}s
     */
    private CallInitiation determineCallInitiation(Endpoint endpointFrom, Endpoint endpointTo)
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
                if (!(endpointTo instanceof VirtualRoom) && callInitiationTo == CallInitiation.VIRTUAL_ROOM) {
                    callInitiation = callInitiationTo;
                }
            }
        }
        // If no call initiation was specified for the endpoints, use the default
        if (callInitiation == null) {
            callInitiation = this.getCallInitiation();
        }
        return callInitiation;
    }

    /**
     * Find plan for connecting endpoints without virtual room.
     *
     * @return plan if possible, null otherwise
     */
    private CompartmentReservation createNoVirtualRoomReservation() throws ReportException
    {
        List<Endpoint> endpoints = compartment.getEndpoints();
        // Maximal two endpoints may be connected without virtual room
        if (compartment.getTotalEndpointCount() > 2 || endpoints.size() > 2) {
            return null;
        }

        // Two endpoints must be standalone and interconnectable
        Endpoint endpointFrom = null;
        Endpoint endpointTo = null;
        if (endpoints.size() == 2) {
            if (compartment.getTotalEndpointCount() != 2) {
                throw new IllegalStateException();
            }
            endpointFrom = endpoints.get(0);
            endpointTo = endpoints.get(1);

            // Check if endpoints are standalone
            if (!endpointFrom.isStandalone() || !endpointTo.isStandalone()) {
                return null;
            }

            // Check connectivity
            ConnectivityEdge connectivityEdge = connectivityGraph.getEdge(endpointFrom, endpointTo);
            if (connectivityEdge == null) {
                Endpoint endpointTemp = endpointFrom;
                endpointFrom = endpointTo;
                endpointTo = endpointTemp;
                connectivityEdge = connectivityGraph.getEdge(endpointFrom, endpointTo);
                if (connectivityEdge == null) {
                    return null;
                }
            }
        }
        else {
            // Only allocated resource is allowed
            Endpoint allocatedEndpoint = endpoints.get(0);
            if (!(allocatedEndpoint instanceof DeviceResourceEndpoint)) {
                return null;
            }
        }

        // Create allocated compartment
        CompartmentReservation compartmentReservation = new CompartmentReservation();
        for (Reservation childReservation : getChildReservations()) {
            compartmentReservation.addChildReservation(childReservation);
        }
        // Add connection between two standalone endpoints
        if (endpointFrom != null && endpointTo != null) {
            addConnection(endpointFrom, endpointTo);
        }
        return compartmentReservation;
    }

    /**
     * @return collection of technology sets which interconnects all endpoints
     */
    private Collection<Set<Technology>> getSingleVirtualRoomPlanTechnologySets()
    {
        List<Set<Technology>> technologiesList = new ArrayList<Set<Technology>>();
        for (Endpoint endpoint : compartment.getEndpoints()) {
            technologiesList.add(endpoint.getSupportedTechnologies());
        }
        return Technology.interconnect(technologiesList);
    }

    /**
     * Find plan for connecting endpoints by a single virtual room
     *
     * @return plan if possible, null otherwise
     */
    private CompartmentReservation createSingleVirtualRoomReservation() throws ReportException
    {
        Collection<Set<Technology>> technologySets = getSingleVirtualRoomPlanTechnologySets();

        // Get available virtual rooms
        List<AvailableVirtualRoom> availableVirtualRooms = getCache().findAvailableVirtualRoomsByVariants(
                getInterval(), compartment.getTotalEndpointCount(), technologySets);
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
        VirtualRoom virtualRoom = new VirtualRoom();
        if (true) {
            throw new TodoImplementException();
        }
        //virtualRoom.setResource(availableVirtualRoom.getDeviceResource());
        //virtualRoom.setPortCount(totalAllocatedEndpointCount);
        compartment.addEndpoint(virtualRoom);
        addReport(new AllocatingVirtualRoomReport(virtualRoom));

        // Create allocated compartment
        CompartmentReservation compartmentReservation = new CompartmentReservation();
        compartmentReservation.setCompartment(compartment);
        for (Reservation childReservation : getChildReservations()) {
            compartmentReservation.addChildReservation(childReservation);
            if (childReservation instanceof EndpointReservation) {
                addConnection(virtualRoom, ((EndpointReservation) childReservation).getEndpoint());
            }
        }
        return compartmentReservation;
    }

    /**
     * Show current connectivity graph in dialog
     */
    public void showConnectivityGraph()
    {
        JGraph graph = new JGraph(new JGraphModelAdapter<Endpoint, ConnectivityEdge>(connectivityGraph));

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

    @Override
    protected CompartmentReservation createReservation(CompartmentSpecification specification) throws ReportException
    {
        for (Specification childSpecification : specification.getSpecifications()) {
            addChildSpecification(childSpecification);
        }

        if (compartment.getTotalEndpointCount() <= 1) {
            // Check whether an existing resource is requested
            boolean resourceRequested = false;
            for (Reservation childReservation : getChildReservations()) {
                if (true) {
                    throw new TodoImplementException();
                }
                //if (childReservation instanceof AllocatedResource) {
                //    resourceRequested = true;
                //}
            }
            if (!resourceRequested) {
                throw new NotEnoughEndpointInCompartmentReport().exception();
            }
        }

        CompartmentReservation noVirtualRoomReservation = createNoVirtualRoomReservation();
        if (noVirtualRoomReservation != null) {
            return noVirtualRoomReservation;
        }

        CompartmentReservation singleVirtualRoomReservation = createSingleVirtualRoomReservation();
        if (singleVirtualRoomReservation != null) {
            return singleVirtualRoomReservation;
        }

        // TODO: Resolve multiple virtual rooms and/or gateways for connecting endpoints

        throw new NoAvailableVirtualRoomReport(getSingleVirtualRoomPlanTechnologySets(),
                compartment.getTotalEndpointCount()).exception();
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
