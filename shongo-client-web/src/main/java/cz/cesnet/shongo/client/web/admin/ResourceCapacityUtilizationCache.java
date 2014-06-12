package cz.cesnet.shongo.client.web.admin;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.request.ReservationListRequest;
import cz.cesnet.shongo.controller.api.request.ResourceListRequest;
import cz.cesnet.shongo.controller.api.rpc.ReservationService;
import cz.cesnet.shongo.controller.api.rpc.ResourceService;
import cz.cesnet.shongo.util.RangeSet;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ResourceCapacityUtilizationCache
{
    private static Logger logger = LoggerFactory.getLogger(ResourceCapacityUtilizationCache.class);

    private static final Period EXPIRATION = Period.minutes(10);

    private final SecurityToken securityToken;

    private final ReservationService reservationService;

    private final List<ResourceCapacity> resourceCapacities = new LinkedList<ResourceCapacity>();

    private final Map<String, Map<Class<? extends ResourceCapacity>, ResourceCapacity>> resourceCapacityMap =
            new HashMap<String, Map<Class<? extends ResourceCapacity>, ResourceCapacity>>();

    private DateTime expirationDateTime;

    private Map<Interval, Map<ResourceCapacity, ResourceCapacityUtilization>> resourceCapacityUtilizationMap;

    private Map<ResourceCapacity, RangeSet<ReservationSummary, DateTime>> reservationSetMap;

    private Interval reservationInterval;

    public ResourceCapacityUtilizationCache(SecurityToken securityToken, ResourceService resourceService,
            ReservationService reservationService)
    {
        this.securityToken = securityToken;
        this.reservationService = reservationService;

        ResourceListRequest resourceListRequest = new ResourceListRequest(securityToken);
        resourceListRequest.setSort(ResourceListRequest.Sort.NAME);
        resourceListRequest.addCapabilityClass(RoomProviderCapability.class);
        resourceListRequest.addCapabilityClass(RecordingCapability.class);
        for (ResourceSummary resourceSummary : resourceService.listResources(resourceListRequest)) {
            String resourceId = resourceSummary.getId();
            Resource resource = resourceService.getResource(securityToken, resourceId);
            for (Capability capability : resource.getCapabilities()) {
                if (capability instanceof RoomProviderCapability) {
                    RoomProviderCapability roomProviderCapability = (RoomProviderCapability) capability;
                    addResourceCapacity(new ResourceCapacity.Room(resourceSummary, roomProviderCapability));
                }
                else if (capability instanceof RecordingCapability) {
                    RecordingCapability recordingCapability = (RecordingCapability) capability;
                    if (recordingCapability.getLicenseCount() != null) {
                        addResourceCapacity(new ResourceCapacity.Recording(resourceSummary, recordingCapability));
                    }
                }
            }
        }
    }

    public synchronized void clear()
    {
        this.expirationDateTime = DateTime.now().plus(EXPIRATION);
        this.resourceCapacityUtilizationMap = null;
        this.reservationSetMap = null;
        this.reservationInterval = null;
    }

    public synchronized void clearExpired(DateTime dateTime)
    {
        if (this.expirationDateTime != null && this.expirationDateTime.isAfterNow()) {
            clear();
        }
    }

    public Map<Interval, Map<ResourceCapacity, ResourceCapacityUtilization>> getUtilization(Interval interval,
            Period period)
    {
        initMaps();
        Map<Interval, Map<ResourceCapacity, ResourceCapacityUtilization>> utilizationsByInterval =
                new LinkedHashMap<Interval, Map<ResourceCapacity, ResourceCapacityUtilization>>();
        DateTime start = interval.getStart();
        DateTime maxEnd = interval.getEnd();
        while (start.isBefore(maxEnd)) {
            DateTime end = start.plus(period);
            if (end.isAfter(maxEnd)) {
                end = maxEnd;
            }
            Interval utilizationInterval = new Interval(start, end);
            Map<ResourceCapacity, ResourceCapacityUtilization> utilizations =
                    new HashMap<ResourceCapacity, ResourceCapacityUtilization>();
            for (ResourceCapacity resourceCapacity : resourceCapacities) {
                ResourceCapacityUtilization utilization =
                        getUtilization(resourceCapacity, utilizationInterval, true, interval);
                if (utilization != null) {
                    utilizations.put(resourceCapacity, utilization);
                }
            }
            utilizationsByInterval.put(utilizationInterval, utilizations);
            start = end;
        }
        return utilizationsByInterval;
    }

    public ResourceCapacityUtilization getUtilization(ResourceCapacity resourceCapacity, Interval interval)
    {
        initMaps();
        return getUtilization(resourceCapacity, interval, false, interval);
    }

    private synchronized void initMaps()
    {
        // Initialize maps which can expire
        boolean expired = (this.expirationDateTime != null && this.expirationDateTime.isBeforeNow());
        if (this.resourceCapacityUtilizationMap == null || expired) {
            this.resourceCapacityUtilizationMap =
                    new HashMap<Interval, Map<ResourceCapacity, ResourceCapacityUtilization>>();
        }
        if (this.reservationSetMap == null || expired) {
            this.reservationSetMap = new HashMap<ResourceCapacity, RangeSet<ReservationSummary, DateTime>>();
            this.reservationInterval = null;
        }
        if (expired) {
            this.expirationDateTime = DateTime.now().plus(EXPIRATION);
        }
    }

    private void addResourceCapacity(ResourceCapacity resourceCapacity)
    {
        if (resourceCapacities.add(resourceCapacity)) {
            String resourceId = resourceCapacity.getResourceId();
            Class<? extends ResourceCapacity> resourceCapacityClass = resourceCapacity.getClass();

            Map<Class<? extends ResourceCapacity>, ResourceCapacity> resourceCapacitiesByClass =
                    resourceCapacityMap.get(resourceId);
            if (resourceCapacitiesByClass == null) {
                resourceCapacitiesByClass = new HashMap<Class<? extends ResourceCapacity>, ResourceCapacity>();
                resourceCapacityMap.put(resourceId, resourceCapacitiesByClass);
            }
            resourceCapacitiesByClass.put(resourceCapacityClass, resourceCapacity);
        }
    }

    private ResourceCapacityUtilization getUtilization(ResourceCapacity resourceCapacity, Interval interval,
            boolean fetchAll, Interval fetchInterval)
    {
        // Try to return cached utilization
        Map<ResourceCapacity, ResourceCapacityUtilization> utilizations = resourceCapacityUtilizationMap.get(interval);
        if (utilizations == null) {
            utilizations = new HashMap<ResourceCapacity, ResourceCapacityUtilization>();
            resourceCapacityUtilizationMap.put(interval, utilizations);
        }
        if (utilizations.containsKey(resourceCapacity)) {
            return utilizations.get(resourceCapacity);
        }

        // Fetch reservations
        RangeSet<ReservationSummary, DateTime> reservations;
        if (fetchAll) {
            reservations = getReservationsWithFetchAll(resourceCapacity, fetchInterval);
        }
        else {
            reservations = getReservations(resourceCapacity, fetchInterval);
        }

        // Prepare new utilization
        ResourceCapacityUtilization utilization = null;
        if (reservations != null) {
            Collection<ResourceCapacityBucket> buckets =
                    reservations.getBuckets(interval.getStart(), interval.getEnd(), ResourceCapacityBucket.class);
            if (buckets.size() > 0) {
                utilization = new ResourceCapacityUtilization(buckets);
            }
        }

        // Store the utilization to cache and return it
        utilizations.put(resourceCapacity, utilization);
        return utilization;
    }

    /**
     * Get {@link RangeSet} for given {@code resourceCapacity}.
     *
     * @param resourceCapacity
     * @param interval
     * @return {@link RangeSet} of {@link ReservationSummary}
     */
    private synchronized RangeSet<ReservationSummary, DateTime> getReservationsWithFetchAll(
            ResourceCapacity resourceCapacity, Interval interval)
    {
        // Try to return cached reservations
        if (this.reservationInterval != null && this.reservationInterval.contains(interval)) {
            return this.reservationSetMap.get(resourceCapacity);
        }
        DateTime start = interval.getStart();
        DateTime end = interval.getEnd();

        // Expand reservation cache at start
        if (reservationInterval != null &&
                reservationInterval.isAfter(start) && reservationInterval.contains(end)) {
            interval = new Interval(start, reservationInterval.getStart());
            reservationInterval = new Interval(start, reservationInterval.getEnd());
        }
        // Expand reservation cache at end
        else if (reservationInterval != null &&
                reservationInterval.isBefore(end) && reservationInterval.contains(start)) {
            interval = new Interval(reservationInterval.getEnd(), end);
            reservationInterval = new Interval(reservationInterval.getStart(), end);
        }
        // Load the whole reservation cache
        else {
            logger.info("Clearing cached reservations...");
            reservationSetMap.clear();
            reservationInterval = interval;
        }

        // Fetch reservations for all resource capacities
        logger.info("Loading reservations for {}...", interval);
        ReservationListRequest reservationListRequest = new ReservationListRequest(securityToken);
        for (ResourceCapacity currentResourceCapacity : resourceCapacities) {
            reservationListRequest.addResourceId(currentResourceCapacity.getResourceId());
        }
        reservationListRequest.addReservationType(ReservationSummary.Type.ROOM);
        reservationListRequest.addReservationType(ReservationSummary.Type.RECORDING_SERVICE);
        reservationListRequest.setInterval(interval);
        for (ReservationSummary reservation : reservationService.listReservations(reservationListRequest)) {
            Interval reservationSlot = reservation.getSlot();
            String reservationResourceId = reservation.getResourceId();
            ResourceCapacity reservationResourceCapacity = getResourceCapacity(reservationResourceId, reservation);
            RangeSet<ReservationSummary, DateTime> reservationSet = reservationSetMap.get(reservationResourceCapacity);
            if (reservationSet == null) {
                reservationSet = new RangeSet<ReservationSummary, DateTime>()
                {
                    @Override
                    protected Bucket<DateTime, ReservationSummary> createBucket(DateTime rangeValue)
                    {
                        return new ResourceCapacityBucket(rangeValue);
                    }
                };
                reservationSetMap.put(reservationResourceCapacity, reservationSet);
            }
            reservationSet.add(reservation, reservationSlot.getStart(), reservationSlot.getEnd());
        }
        return reservationSetMap.get(resourceCapacity);
    }

    /**
     * Get {@link RangeSet} for given {@code resourceCapacity}.
     * <p/>
     * When {@link RangeSet} doesn't exists in {@link #reservationSetMap}, the newly created isn't stored there
     * (because only reservations for single {@code resourceCapacity} are fetched).
     *
     * @param resourceCapacity
     * @param interval
     * @return {@link RangeSet} of {@link ReservationSummary}
     */
    private RangeSet<ReservationSummary, DateTime> getReservations(ResourceCapacity resourceCapacity,
            Interval interval)
    {
        // Try to return cached reservations
        synchronized (this) {
            if (this.reservationInterval != null && this.reservationInterval.contains(interval)) {
                return this.reservationSetMap.get(resourceCapacity);
            }
        }

        // Fetch reservations for single resource capacity
        RangeSet<ReservationSummary, DateTime> reservationSet = new RangeSet<ReservationSummary, DateTime>()
        {
            @Override
            protected Bucket<DateTime, ReservationSummary> createBucket(DateTime rangeValue)
            {
                return new ResourceCapacityBucket(rangeValue);
            }
        };
        ReservationListRequest reservationListRequest = new ReservationListRequest(securityToken);
        reservationListRequest.addResourceId(resourceCapacity.getResourceId());
        reservationListRequest.addReservationType(resourceCapacity.getReservationType());
        reservationListRequest.setInterval(interval);
        for (ReservationSummary reservation : reservationService.listReservations(reservationListRequest)) {
            Interval reservationSlot = reservation.getSlot();
            reservationSet.add(reservation, reservationSlot.getStart(), reservationSlot.getEnd());
        }
        return reservationSet;
    }

    public Collection<ResourceCapacity> getResourceCapacities()
    {
        return Collections.unmodifiableList(resourceCapacities);
    }

    public ResourceCapacity getResourceCapacity(String resourceId,
            Class<? extends ResourceCapacity> resourceCapacityClass)
    {
        Map<Class<? extends ResourceCapacity>, ResourceCapacity> resourceCapacitiesByClass =
                resourceCapacityMap.get(resourceId);
        if (resourceCapacitiesByClass == null) {
            return null;
        }
        return resourceCapacitiesByClass.get(resourceCapacityClass);
    }

    private ResourceCapacity getResourceCapacity(String resourceId, ReservationSummary reservation)
    {
        return getResourceCapacity(resourceId, getResourceCapacityClass(reservation.getType()));
    }

    private Class<? extends ResourceCapacity> getResourceCapacityClass(ReservationSummary.Type reservationType)
    {
        switch (reservationType) {
            case ROOM:
                return ResourceCapacity.Room.class;
            case RECORDING_SERVICE:
                return ResourceCapacity.Recording.class;
            default:
                throw new TodoImplementException(reservationType);
        }
    }
}
