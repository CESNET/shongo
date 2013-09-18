package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.Converter;
import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.controller.api.rpc.ReservationService;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Period;

import java.util.LinkedList;
import java.util.List;

/**
 * Request for reservation of resources.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationRequest extends AbstractReservationRequest
{
    /**
     * Shongo-id of reservation request for which is this reservation request created.
     */
    private String parentReservationRequestId;

    /**
     * Date/time slot for which the reservation is requested.
     */
    private Interval slot;

    /**
     * {@link AllocationState} of the request.
     */
    private AllocationState allocationState;

    /**
     * {@link AllocationStateReport} for the {@link #allocationState}.
     */
    private AllocationStateReport allocationStateReport;

    /**
     * Allocated {@link Reservation}s.
     */
    private List<String> reservationIds = new LinkedList<String>();

    /**
     * Constructor.
     */
    public ReservationRequest()
    {
    }

    /**
     * @return {@link #parentReservationRequestId}
     */
    public String getParentReservationRequestId()
    {
        return parentReservationRequestId;
    }

    /**
     * @param parentReservationRequestId sets the {@link #parentReservationRequestId}
     */
    public void setParentReservationRequestId(String parentReservationRequestId)
    {
        this.parentReservationRequestId = parentReservationRequestId;
    }

    /**
     * @return {@link #SLOT}
     */
    public Interval getSlot()
    {
        return slot;
    }

    /**
     * @param slot sets the {@link #SLOT}
     */
    public void setSlot(Interval slot)
    {
        this.slot = slot;
    }

    /**
     * @param slot sets the {@link #SLOT}
     */
    public void setSlot(String slot)
    {
        this.slot = Converter.convertStringToInterval(slot);
    }

    /**
     * @param start sets the starting date/time from the {@link #SLOT}
     * @param end   sets the ending date/time from the {@link #SLOT}
     */
    public void setSlot(DateTime start, DateTime end)
    {
        setSlot(new Interval(start, end));
    }

    /**
     * @param dateTime sets the date/time from the {@link #SLOT}
     * @param duration sets the duration from the {@link #SLOT}
     */
    public void setSlot(DateTime dateTime, Period duration)
    {
        setSlot(new Interval(dateTime, duration));
    }

    /**
     * @param startDateTime         sets the starting date/time for the {@link #SLOT}
     * @param endDateTimeOrDuration sets the ending date/time or duration for the {@link #SLOT}
     */
    public void setSlot(String startDateTime, String endDateTimeOrDuration)
    {
        Interval interval;
        try {
            interval = new Interval(DateTime.parse(startDateTime), DateTime.parse(endDateTimeOrDuration));
        }
        catch (IllegalArgumentException exception) {
            interval = new Interval(DateTime.parse(startDateTime), Period.parse(endDateTimeOrDuration));
        }
        setSlot(interval);
    }

    /**
     * @return {@link #allocationState}
     */
    public AllocationState getAllocationState()
    {
        return allocationState;
    }

    /**
     * @param allocationState sets the {@link #allocationState}
     */
    public void setAllocationState(AllocationState allocationState)
    {
        this.allocationState = allocationState;
    }

    /**
     * @return {@link #allocationStateReport}
     */
    public AllocationStateReport getAllocationStateReport()
    {
        return allocationStateReport;
    }

    /**
     * @param allocationStateReport sets the {@link #allocationStateReport}
     */
    public void setAllocationStateReport(AllocationStateReport allocationStateReport)
    {
        this.allocationStateReport = allocationStateReport;
    }

    /**
     * @return {@link #reservationIds}
     */
    public List<String> getReservationIds()
    {
        return reservationIds;
    }

    /**
     * @return last {@link #reservationIds} or null
     */
    public String getLastReservationId()
    {
        if (reservationIds.size() == 0) {
            return null;
        }
        return reservationIds.get(reservationIds.size() - 1);
    }

    /**
     * @param reservationService
     * @param securityToken
     * @return last {@link Reservation}
     */
    public Reservation getLastReservation(ReservationService reservationService, SecurityToken securityToken)
    {
        String reservationId = getLastReservationId();
        if (reservationId != null) {
            return reservationService.getReservation(securityToken, reservationId);
        }
        return null;
    }

    /**
     * @param reservationIds sets the {@link #reservationIds}
     */
    public void setReservationIds(List<String> reservationIds)
    {
        this.reservationIds = reservationIds;
    }

    /**
     * @param reservationId to be added to the {@link #reservationIds}
     */
    public void addReservationId(String reservationId)
    {
        this.reservationIds.add(reservationId);
    }

    public static final String PARENT_RESERVATION_REQUEST_ID = "parentReservationRequestId";
    public static final String SLOT = "slot";
    public static final String ALLOCATION_STATE = "allocationState";
    public static final String ALLOCATION_STATE_REPORT = "allocationStateReport";
    public static final String RESERVATION_IDS = "reservationIds";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(PARENT_RESERVATION_REQUEST_ID, parentReservationRequestId);
        dataMap.set(SLOT, slot);
        dataMap.set(ALLOCATION_STATE, allocationState);
        dataMap.set(ALLOCATION_STATE_REPORT, allocationStateReport);
        dataMap.set(RESERVATION_IDS, reservationIds);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        parentReservationRequestId = dataMap.getString(PARENT_RESERVATION_REQUEST_ID);
        slot = dataMap.getIntervalRequired(SLOT);
        allocationState = dataMap.getEnum(ALLOCATION_STATE, AllocationState.class);
        allocationStateReport = dataMap.getComplexType(ALLOCATION_STATE_REPORT, AllocationStateReport.class);
        reservationIds = dataMap.getList(RESERVATION_IDS, String.class);
    }
}
