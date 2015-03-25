package cz.cesnet.shongo.controller.api.request;

import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.api.PeriodicDateTimeSlot;
import cz.cesnet.shongo.controller.api.ReservationRequest;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.Specification;
import org.joda.time.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * {@link AbstractRequest} for checking availability of {@link Specification}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AvailabilityCheckRequest extends AbstractRequest
{
    /**
     * @see ReservationRequestPurpose
     */
    private ReservationRequestPurpose purpose;

    /**
     * Time slots for which the availability should be checked.
     */
    private List<PeriodicDateTimeSlot> slots;

    /**
     * Period for which the availability should be checked.
     */
    private Period period;

    /**
     * Period end for which the availability should be checked.
     */
    private ReadablePartial periodEnd;


    /**
     * Order of periodicity day when {@link cz.cesnet.shongo.controller.api.PeriodicDateTimeSlot.PeriodicityType.MonthPeriodicityType#SPECIFIC_DAY} period is set
     */
    protected Integer periodicityDayOrder;

    /**
     * Day of periodicity day when {@link cz.cesnet.shongo.controller.api.PeriodicDateTimeSlot.PeriodicityType.MonthPeriodicityType#SPECIFIC_DAY} period is set
     */
    protected PeriodicDateTimeSlot.DayOfWeek periodicityDayInMonth;

    /**
     * To be checked if it is available in specified {@link #slots},
     */
    private Specification specification;

    /**
     * To be checked if it is available to be reused in specified {@link #slots},
     */
    private String reservationRequestId;

    /**
     * Identifier of reservation request whose reservations should be ignored.
     */
    private String ignoredReservationRequestId;

    /**
     * Constructor.
     */
    public AvailabilityCheckRequest()
    {
        this.slots = new LinkedList<PeriodicDateTimeSlot>();
    }

    /**
     * Constructor.
     *
     * @param securityToken sets the {@link #securityToken}
     */
    public AvailabilityCheckRequest(SecurityToken securityToken)
    {
        super(securityToken);
        this.slots = new LinkedList<PeriodicDateTimeSlot>();
    }

    /**
     * Constructor.
     *
     * @param securityToken        sets the {@link #securityToken}
     * @param slots                 sets the {@link #slots}
     * @param specification        sets the {@link #specification}
     * @param reservationRequestId sets the {@link #reservationRequestId}
     */
    public AvailabilityCheckRequest(SecurityToken securityToken, List<PeriodicDateTimeSlot> slots,
            Specification specification, String reservationRequestId)
    {
        super(securityToken);
        this.slots = new LinkedList<PeriodicDateTimeSlot>();
        addAllSlots(slots);
        this.specification = specification;
        this.reservationRequestId = reservationRequestId;
    }

    /**
     * Constructor.
     *
     * @param securityToken sets the {@link #securityToken}
     * @param reservationRequest to be initialized from
     *
    public AvailabilityCheckRequest(SecurityToken securityToken, ReservationRequest reservationRequest)
    {
        this(securityToken, slots, reservationRequest.getSpecification(), reservationRequest.getReusedReservationRequestId());
    }/*

    /**
     * @return {@link #purpose}
     */
    public ReservationRequestPurpose getPurpose()
    {
        return purpose;
    }

    /**
     * @param purpose sets the {@link #purpose}
     */
    public void setPurpose(ReservationRequestPurpose purpose)
    {
        this.purpose = purpose;
    }

    public List<PeriodicDateTimeSlot> getSlots() {
        return slots;
    }

    public void setSlots(List<PeriodicDateTimeSlot> slots) {
        this.slots = slots;
    }

    public void addSlot(PeriodicDateTimeSlot slot) {
        this.slots.add(slot);
    }

    public void addSlot(Interval interval) {
        PeriodicDateTimeSlot slot = new PeriodicDateTimeSlot(interval.getStart(), interval.toPeriod(), null);
        this.slots.add(slot);
    }

    public void addAllSlots(List<PeriodicDateTimeSlot> slots) {
        this.slots.addAll(slots);
    }

    /**
     * @return {@link #specification}
     */
    public Specification getSpecification()
    {
        return specification;
    }

    /**
     * @param specification sets the {@link #specification}
     */
    public void setSpecification(Specification specification)
    {
        this.specification = specification;
    }

    /**
     * @return {@link #reservationRequestId}
     */
    public String getReservationRequestId()
    {
        return reservationRequestId;
    }

    /**
     * @param reservationRequestId sets the {@link #reservationRequestId}
     */
    public void setReservationRequestId(String reservationRequestId)
    {
        this.reservationRequestId = reservationRequestId;
    }

    /**
     * @return {@link #ignoredReservationRequestId}
     */
    public String getIgnoredReservationRequestId()
    {
        return ignoredReservationRequestId;
    }

    /**
     * @param ignoredReservationRequestId sets the {@link #ignoredReservationRequestId}
     */
    public void setIgnoredReservationRequestId(String ignoredReservationRequestId)
    {
        this.ignoredReservationRequestId = ignoredReservationRequestId;
    }

    public Period getPeriod() {
        return period;
    }

    public void setPeriod(Period period) {
        this.period = period;
    }

    public ReadablePartial getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(ReadablePartial periodEnd) {
        this.periodEnd = periodEnd;
    }

    private static final String PURPOSE = "purpose";
    private static final String SLOTS = "slots";
    private static final String SPECIFICATION = "specification";
    private static final String RESERVATION_REQUEST = "reservationRequestId";
    private static final String IGNORED_RESERVATION_REQUEST = "ignoredReservationRequestId";
    private static final String PERIODICITY_TYPE = "periodicityType";
    private static final String PERIOD = "period";
    private static final String PERIOD_END = "periodEnd";
    public static final String PERIODICITY_DAY_ORDER = "periodicityDayOrder";
    public static final String PERIODICITY_DAY_IN_MONTH = "periodicityDayInMonth";

    public Integer getPeriodicityDayOrder() {
        return periodicityDayOrder;
    }

    public void setPeriodicityDayOrder(Integer periodicityDayOrder) {
        this.periodicityDayOrder = periodicityDayOrder;
    }

    public PeriodicDateTimeSlot.DayOfWeek getPeriodicityDayInMonth() {
        return periodicityDayInMonth;
    }

    public void setPeriodicityDayInMonth(PeriodicDateTimeSlot.DayOfWeek periodicityDayInMonth) {
        this.periodicityDayInMonth = periodicityDayInMonth;
    }

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(PURPOSE, purpose);
        dataMap.set(SLOTS, slots);
        dataMap.set(SPECIFICATION, specification);
        dataMap.set(RESERVATION_REQUEST, reservationRequestId);
        dataMap.set(IGNORED_RESERVATION_REQUEST, ignoredReservationRequestId);
        dataMap.set(PERIOD, period);
        dataMap.set(PERIOD_END, periodEnd);
        dataMap.set(PERIODICITY_DAY_ORDER, periodicityDayOrder);
        dataMap.set(PERIODICITY_DAY_IN_MONTH, periodicityDayInMonth);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        purpose = dataMap.getEnum(PURPOSE, ReservationRequestPurpose.class);
        slots = dataMap.getList(SLOTS, PeriodicDateTimeSlot.class);
        specification = dataMap.getComplexType(SPECIFICATION, Specification.class);
        reservationRequestId = dataMap.getString(RESERVATION_REQUEST);
        ignoredReservationRequestId = dataMap.getString(IGNORED_RESERVATION_REQUEST);
        period = dataMap.getPeriod(PERIOD);
        periodEnd = dataMap.getReadablePartial(PERIOD_END);
        periodicityDayOrder = dataMap.getInteger(PERIODICITY_DAY_ORDER);
        periodicityDayInMonth = dataMap.getEnum(PERIODICITY_DAY_IN_MONTH, PeriodicDateTimeSlot.DayOfWeek.class);
    }
}
