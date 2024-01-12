package cz.cesnet.shongo.controller.rest.models.resource;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.api.Capability;
import cz.cesnet.shongo.controller.api.RecordingCapability;
import cz.cesnet.shongo.controller.api.ReservationSummary;
import cz.cesnet.shongo.controller.api.Resource;
import cz.cesnet.shongo.controller.api.ResourceSummary;
import cz.cesnet.shongo.controller.api.RoomProviderCapability;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.request.ReservationListRequest;
import cz.cesnet.shongo.controller.api.request.ResourceListRequest;
import cz.cesnet.shongo.controller.api.rpc.ReservationService;
import cz.cesnet.shongo.controller.api.rpc.ResourceService;
import cz.cesnet.shongo.util.RangeSet;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Period;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Represents utilization of all types of capacities for all resources to which a single user has access.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Slf4j
public class ResourcesUtilization
{

    /**
     * {@link SecurityToken} of user to which the {@link ResourcesUtilization} belongs.
     */
    private final SecurityToken securityToken;

    /**
     * {@link ReservationService} for retrieving reservations.
     */
    private final ReservationService reservationService;

    /**
     * List of {@link ResourceCapacity} to which the user has access.
     */
    private final List<ResourceCapacity> resourceCapacities = new LinkedList<ResourceCapacity>();

    /**
     * Map of {@link ResourceCapacity} by class and by resource-id.
     */
    private final Map<String, Map<Class<? extends ResourceCapacity>, ResourceCapacity>> resourceCapacityMap =
            new HashMap<>();

    /**
     * Map of cached {@link ResourceCapacityUtilization} for {@link ResourceCapacity} and for {@link Interval}.
     */
    private final Map<Interval, Map<ResourceCapacity, ResourceCapacityUtilization>> resourceCapacityUtilizationMap =
            new HashMap<>();

    /**
     * Map of cached {@link ReservationSummary}s for {@link ResourceCapacity}.
     */
    private final Map<ResourceCapacity, RangeSet<ReservationSummary, DateTime>> reservationSetMap =
            new HashMap<>();

    /**
     * {@link Interval} which is already cached in {@link #reservationSetMap}.
     */
    private Interval reservationInterval;

    /**
     * Constructor.
     *
     * @param securityToken sets the {@link #securityToken}
     * @param resources     to be used for fetching {@link #resourceCapacities}
     * @param reservations  sets the {@link #reservationService}
     */
    public ResourcesUtilization(SecurityToken securityToken, ResourceService resources, ReservationService reservations)
    {
        this.securityToken = securityToken;
        this.reservationService = reservations;

        // Fetch ResourceCapacities for all accessible resources
        ResourceListRequest resourceListRequest = new ResourceListRequest(securityToken);
        resourceListRequest.setSort(ResourceListRequest.Sort.NAME);
        resourceListRequest.addCapabilityClass(RoomProviderCapability.class);
        resourceListRequest.addCapabilityClass(RecordingCapability.class);
        for (ResourceSummary resourceSummary : resources.listResources(resourceListRequest)) {
            String resourceId = resourceSummary.getId();
            Resource resource = resources.getResource(securityToken, resourceId);
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

    /**
     * @return {@link #resourceCapacities}
     */
    public Collection<ResourceCapacity> getResourceCapacities()
    {
        return Collections.unmodifiableList(resourceCapacities);
    }

    /**
     * @param resourceId
     * @param capacityClass
     * @return {@link ResourceCapacity} for given {@code resourceId} and {@code capacityClass}
     */
    public ResourceCapacity getResourceCapacity(String resourceId, Class<? extends ResourceCapacity> capacityClass)
    {
        Map<Class<? extends ResourceCapacity>, ResourceCapacity> resourceCapacitiesByClass =
                resourceCapacityMap.get(resourceId);
        if (resourceCapacitiesByClass == null) {
            return null;
        }
        return resourceCapacitiesByClass.get(capacityClass);
    }

    /**
     * @param interval interval to be returned
     * @param period   by which the {@code interval} should be split and for each part should be {@link ResourceCapacityUtilization} computed
     * @return map of {@link ResourceCapacityUtilization} by {@link ResourceCapacity}s and by {@link Interval}s
     */
    public Map<Interval, Map<ResourceCapacity, ResourceCapacityUtilization>> getUtilization(
            Interval interval,
            Period period)
    {
        Map<Interval, Map<ResourceCapacity, ResourceCapacityUtilization>> utilizationsByInterval =
                new LinkedHashMap<>();
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
                utilizations.put(resourceCapacity, utilization);
            }
            utilizationsByInterval.put(utilizationInterval, utilizations);
            start = end;
        }
        return utilizationsByInterval;
    }

    /**
     * @param resourceCapacity
     * @param interval
     * @return {@link ResourceCapacityUtilization} for given {@code resourceCapacity} and {@code interval}
     */
    public ResourceCapacityUtilization getUtilization(ResourceCapacity resourceCapacity, Interval interval)
    {
        return getUtilization(resourceCapacity, interval, false, interval);
    }

    /**
     * @param resourceCapacity to be added to the {@link #resourceCapacities} and {@link #resourceCapacityMap}
     */
    private void addResourceCapacity(ResourceCapacity resourceCapacity)
    {
        if (resourceCapacities.add(resourceCapacity)) {
            String resourceId = resourceCapacity.getResourceId();
            Class<? extends ResourceCapacity> resourceCapacityClass = resourceCapacity.getClass();

            Map<Class<? extends ResourceCapacity>, ResourceCapacity> resourceCapacitiesByClass =
                    resourceCapacityMap.get(resourceId);
            if (resourceCapacitiesByClass == null) {
                resourceCapacitiesByClass = new HashMap<>();
                resourceCapacityMap.put(resourceId, resourceCapacitiesByClass);
            }
            resourceCapacitiesByClass.put(resourceCapacityClass, resourceCapacity);
        }
    }

    /**
     * Can be used for bulk fetching of {@link ReservationSummary}s by use {@code fetchAll} and {@code fetchInterval}.
     *
     * @param resourceCapacity
     * @param interval
     * @param fetchAll
     * @param fetchInterval
     * @return {@link ResourceCapacityUtilization} for given {@code resourceCapacity} and {@code interval}
     */
    private ResourceCapacityUtilization getUtilization(
            ResourceCapacity resourceCapacity,
            Interval interval,
            boolean fetchAll, Interval fetchInterval)
    {
        // Try to return cached utilization
        Map<ResourceCapacity, ResourceCapacityUtilization> utilizations = resourceCapacityUtilizationMap.get(interval);
        if (utilizations == null) {
            utilizations = new HashMap<>();
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
     * Get {@link RangeSet} for given {@code resourceCapacity}
     * by fetching {@link ReservationSummary}s for all {@link #resourceCapacities}.
     * <p/>
     * The newly fetched {@link ReservationSummary}s will stored in {@link #reservationSetMap}
     * (because all {@link ResourceCapacity}s will be updated).
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
            log.info("Clearing cached reservations...");
            reservationSetMap.clear();
            reservationInterval = interval;
        }

        // Fetch reservations for all resource capacities
        log.info("Loading reservations for {}...", interval);
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
                reservationSet = new RangeSet<>()
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
     * Get {@link RangeSet} for given {@code resourceCapacity}
     * by fetching {@link ReservationSummary}s only for given {@code resourceCapacity}.
     * <p/>
     * The newly fetched {@link ReservationSummary}s won't be stored in {@link #reservationSetMap}
     * (because all {@link ResourceCapacity} won't be updated).
     *
     * @param resourceCapacity
     * @param interval
     * @return {@link RangeSet} of {@link ReservationSummary}
     */
    private RangeSet<ReservationSummary, DateTime> getReservations(
            ResourceCapacity resourceCapacity,
            Interval interval)
    {
        // Try to return cached reservations
        synchronized (this) {
            if (this.reservationInterval != null && this.reservationInterval.contains(interval)) {
                return this.reservationSetMap.get(resourceCapacity);
            }
        }

        // Fetch reservations for single resource capacity
        RangeSet<ReservationSummary, DateTime> reservationSet = new RangeSet<>()
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

    /**
     * @param resourceId
     * @param reservation
     * @return {@link ResourceCapacity} for given {@code resourceId} and {@code reservation}
     */
    private ResourceCapacity getResourceCapacity(String resourceId, ReservationSummary reservation)
    {
        return getResourceCapacity(resourceId, getResourceCapacityClass(reservation.getType()));
    }

    /**
     * @param reservationType
     * @return class of {@link ResourceCapacity} for given {@code reservationType}
     */
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
