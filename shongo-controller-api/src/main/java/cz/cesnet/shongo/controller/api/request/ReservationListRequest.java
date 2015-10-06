package cz.cesnet.shongo.controller.api.request;

import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.controller.api.Reservation;
import cz.cesnet.shongo.controller.api.ReservationSummary;
import cz.cesnet.shongo.controller.api.SecurityToken;
import org.joda.time.Interval;

import java.util.*;

/**
 * {@link ListRequest} for reservations.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationListRequest extends SortableListRequest<ReservationListRequest.Sort>
{
    /**
     * {@link ReservationSummary.Type}s which should be returned.
     */
    private Set<ReservationSummary.Type> reservationTypes = new HashSet<ReservationSummary.Type>();

    /**
     * Resource-ids of resources which must be allocated by returned {@link Reservation}s.
     */
    private Set<String> resourceIds = new HashSet<String>();

    /**
     * Interval in only which the {@link Reservation}s should be returned.
     */
    private Interval interval;

    /**
     * Constructor.
     */
    public ReservationListRequest()
    {
        super(Sort.class);
    }

    /**
     * Constructor.
     *
     * @param securityToken sets the {@link #securityToken}
     */
    public ReservationListRequest(SecurityToken securityToken)
    {
        super(Sort.class, securityToken);
    }

    /**
     * @return {@link #reservationTypes}
     */
    public Set<ReservationSummary.Type> getReservationTypes()
    {
        return Collections.unmodifiableSet(reservationTypes);
    }

    /**
     * @param reservationType to be added to the {@link #reservationTypes}
     */
    public void addReservationType(ReservationSummary.Type reservationType)
    {
        this.reservationTypes.add(reservationType);
    }

    /**
     * @return {@link #resourceIds}
     */
    public Set<String> getResourceIds()
    {
        return resourceIds;
    }

    /**
     * @param resourceIds sets the {@link #resourceIds}
     */
    public void setResourceIds(Set<String> resourceIds)
    {
        this.resourceIds.clear();
        this.resourceIds.addAll(resourceIds);
    }

    /**
     * @return {@link #interval}
     */
    public Interval getInterval()
    {
        return interval;
    }

    /**
     * @param interval sets the {@link #interval}
     */
    public void setInterval(Interval interval)
    {
        this.interval = interval;
    }

    /**
     * @param resourceId to be added to the {@link #resourceIds}
     */
    public void addResourceId(String resourceId)
    {
        resourceIds.add(resourceId);
    }

    /**
     * Field by which the result should be sorted.
     */
    public static enum Sort
    {
        SLOT
    }

    private static final String RESERVATION_TYPES = "reservationTypes";
    private static final String RESOURCE_IDS = "resourceIds";
    private static final String INTERVAL = "interval";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(RESERVATION_TYPES, reservationTypes);
        dataMap.set(RESOURCE_IDS, resourceIds);
        dataMap.set(INTERVAL, interval);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        reservationTypes = dataMap.getSet(RESERVATION_TYPES, ReservationSummary.Type.class);
        resourceIds = dataMap.getSet(RESOURCE_IDS, String.class);
        interval = dataMap.getInterval(INTERVAL);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ReservationListRequest request = (ReservationListRequest) o;

        if (reservationTypes != null ? !reservationTypes.equals(request.reservationTypes) : request.reservationTypes != null)
            return false;
        if (resourceIds != request.getResourceIds()) {
            if (resourceIds == null || request.getResourceIds() == null) return false;
            if (resourceIds.size() != request.getResourceIds().size()) return false;
            for (String resourceId : resourceIds) {
                if (!request.getResourceIds().contains(resourceId)) return false;
            }
        }
        return !(interval != null ? !interval.equals(request.interval) : request.interval != null);

    }

    @Override
    public int hashCode()
    {
        int result = reservationTypes != null ? reservationTypes.hashCode() : 0;
        result = 31 * result + (resourceIds != null ? resourceIds.hashCode() : 0);
        result = 31 * result + (interval != null ? interval.hashCode() : 0);
        return result;
    }
}
