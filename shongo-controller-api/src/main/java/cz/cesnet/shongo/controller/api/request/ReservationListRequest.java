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
}
