package cz.cesnet.shongo.client.web.models;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.api.request.ReservationListRequest;
import cz.cesnet.shongo.controller.api.request.ResourceListRequest;
import cz.cesnet.shongo.controller.api.rpc.ReservationService;
import cz.cesnet.shongo.controller.api.rpc.ResourceService;
import cz.cesnet.shongo.util.RangeSet;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Period;

import java.util.*;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ResourceCapacityUtilization
{
    private final SecurityToken securityToken;

    private final ResourceService resourceService;

    private final ReservationService reservationService;

    private final List<ResourceCapacity> resourceCapacities = new LinkedList<ResourceCapacity>();

    private final Map<String, Map<Class<? extends ResourceCapacity>, ResourceCapacity>> resourceCapacityMap =
            new HashMap<String, Map<Class<? extends ResourceCapacity>, ResourceCapacity>>();

    public ResourceCapacityUtilization(SecurityToken securityToken,
            ResourceService resourceService, ReservationService reservationService)
    {
        this.securityToken = securityToken;
        this.resourceService = resourceService;
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
                    addResourceCapacity(new ResourceRoomCapacity(resourceSummary, roomProviderCapability));
                }
                else if (capability instanceof RecordingCapability) {
                    RecordingCapability recordingCapability = (RecordingCapability) capability;
                    if (recordingCapability.getLicenseCount() != null) {
                        addResourceCapacity(new ResourceRecordingCapacity(resourceSummary, recordingCapability));
                    }
                }
            }
        }
    }

    public synchronized List<ReservationSummary> getReservations(Collection<ResourceCapacity> resources, Interval interval)
    {
        ReservationListRequest reservationListRequest = new ReservationListRequest(securityToken);
        for (ResourceCapacity resourceCapacity : resources) {
            reservationListRequest.addResourceId(resourceCapacity.getResourceId());
        }
        reservationListRequest.addReservationType(ReservationSummary.Type.ROOM);
        reservationListRequest.addReservationType(ReservationSummary.Type.RECORDING_SERVICE);
        reservationListRequest.setInterval(interval);
        ListResponse<ReservationSummary> reservations = reservationService.listReservations(reservationListRequest);
        return reservations.getItems();
    }

    public Map<Interval, Map<ResourceCapacity, Utilization>> getUtilization(Interval interval, Period period)
    {
        Map<ResourceCapacity, RangeSet<ReservationSummary, DateTime>> reservationSets =
                new HashMap<ResourceCapacity, RangeSet<ReservationSummary, DateTime>>();
        for (ReservationSummary reservation : getReservations(getResourceCapacities(), interval)) {
            Interval reservationSlot = reservation.getSlot();
            String reservationResourceId = reservation.getResourceId();
            ResourceCapacity resourceCapacity = getResourceCapacity(reservationResourceId, reservation);
            RangeSet<ReservationSummary, DateTime> reservationSet = reservationSets.get(resourceCapacity);
            if (reservationSet == null) {
                reservationSet = new RangeSet<ReservationSummary, DateTime>() {
                    @Override
                    protected Bucket<DateTime, ReservationSummary> createBucket(DateTime rangeValue)
                    {
                        return new LicenseBucket(rangeValue);
                    }
                };
                reservationSets.put(resourceCapacity, reservationSet);
            }
            reservationSet.add(reservation, reservationSlot.getStart(), reservationSlot.getEnd());
        }

        Map<Interval, Map<ResourceCapacity, Utilization>> resourceCapacityUtilizationMap =
                new LinkedHashMap<Interval, Map<ResourceCapacity, Utilization>>();
        DateTime start = interval.getStart();
        DateTime maxEnd = interval.getEnd();
        while (start.isBefore(maxEnd)) {
            DateTime end = start.plus(period);
            if (end.isAfter(maxEnd)) {
                end = maxEnd;
            }
            Map<ResourceCapacity, Utilization> utilizations = new HashMap<ResourceCapacity, Utilization>();
            for (ResourceCapacity resourceCapacity : reservationSets.keySet()) {
                RangeSet<ReservationSummary, DateTime> reservationSet = reservationSets.get(resourceCapacity);

                List<LicenseBucket> buckets = new LinkedList<LicenseBucket>();
                buckets.addAll(reservationSet.getBuckets(start, end, LicenseBucket.class));
                Collections.sort(buckets, new Comparator<LicenseBucket>()
                {
                    @Override
                    public int compare(LicenseBucket roomBucket1, LicenseBucket roomBucket2)
                    {
                        return -Double.compare(roomBucket1.getLicenseCount(), roomBucket2.getLicenseCount());
                    }
                });
                if (buckets.size() > 0) {
                    LicenseBucket bucket = buckets.get(0);
                    utilizations.put(resourceCapacity, new Utilization(bucket));
                }
            }
            resourceCapacityUtilizationMap.put(new Interval(start, end), utilizations);
            start = end;
        }
        return resourceCapacityUtilizationMap;
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

    public Collection<ResourceCapacity> getResourceCapacities()
    {
        return Collections.unmodifiableList(resourceCapacities);
    }

    private ResourceCapacity getResourceCapacity(String resourceId, Class<? extends ResourceCapacity> resourceCapacityClass)
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
                return ResourceRoomCapacity.class;
            case RECORDING_SERVICE:
                return ResourceRecordingCapacity.class;
            default:
                throw new TodoImplementException(reservationType);
        }
    }

    public static class Utilization
    {
        private LicenseBucket bucket;

        public Utilization(LicenseBucket bucket)
        {
            this.bucket = bucket;
        }
    }

    public static abstract class ResourceCapacity
    {
        protected ResourceSummary resource;

        public ResourceCapacity(ResourceSummary resource)
        {
            this.resource = resource;
        }

        public String getResourceId()
        {
            return resource.getId();
        }

        public String getResourceName()
        {
            return resource.getName();
        }

        public abstract String formatUtilization(Utilization utilization);
    }

    public static abstract class ResourceLicenseCountCapacity extends ResourceCapacity
    {
        protected Integer licenseCount;

        public ResourceLicenseCountCapacity(ResourceSummary resource, Integer licenseCount)
        {
            super(resource);

            this.licenseCount = licenseCount;
        }

        @Override
        public String formatUtilization(Utilization utilization)
        {
            String peak = null;
            int utilizedLicenseCount = 0;
            if (utilization != null) {
                utilizedLicenseCount = utilization.bucket.getLicenseCount();
                peak = utilization.bucket.getRangeValue().toString();
                peak += " (" + utilization.bucket.size() + " reservations)";
            }
            StringBuilder output = new StringBuilder();
            output.append(utilizedLicenseCount);
            output.append("/");
            output.append(licenseCount);
            if (utilizedLicenseCount > 0) {
                if (peak != null) {
                    output.insert(0, "<strong title='Peak: " + peak + "'>");
                }
                else {
                    output.insert(0, "<strong>");
                }
                output.append("</strong>");
            }
            return output.toString();
        }
    }

    public static class ResourceRoomCapacity extends ResourceLicenseCountCapacity
    {
        public ResourceRoomCapacity(ResourceSummary resource, RoomProviderCapability capability)
        {
            super(resource, capability.getLicenseCount());
        }
    }

    public static class ResourceRecordingCapacity extends ResourceLicenseCountCapacity
    {
        public ResourceRecordingCapacity(ResourceSummary resource, RecordingCapability capability)
        {
            super(resource, capability.getLicenseCount());
        }
    }

    public class LicenseBucket extends RangeSet.Bucket<DateTime, ReservationSummary>
    {
        /**
         * Sum of {@link RoomReservation#getLicenseCount()}
         */
        private int licenseCount = 0;

        /**
         * Constructor.
         *
         * @param rangeValue
         */
        public LicenseBucket(DateTime rangeValue)
        {
            super(rangeValue);
        }

        /**
         * @return {@link #licenseCount}
         */
        private int getLicenseCount()
        {
            return licenseCount;
        }

        @Override
        public boolean add(ReservationSummary reservation)
        {
            if (super.add(reservation)) {
                switch (reservation.getType()) {
                    case ROOM:
                        this.licenseCount += reservation.getRoomLicenseCount();
                    case RECORDING_SERVICE:
                        this.licenseCount++;
                        break;
                    default:
                        throw new TodoImplementException(reservation.getType());
                }
                return true;
            }
            else {
                return false;
            }
        }

        @Override
        public boolean remove(Object object)
        {
            if (super.remove(object)) {
                ReservationSummary reservation = (ReservationSummary) object;
                switch (reservation.getType()) {
                    case ROOM:
                        this.licenseCount -= reservation.getRoomLicenseCount();
                    case RECORDING_SERVICE:
                        this.licenseCount--;
                        break;
                    default:
                        throw new TodoImplementException(reservation.getType());
                }
                return true;
            }
            else {
                return false;
            }
        }
    }
}
