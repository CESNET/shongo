package cz.cesnet.shongo.controller.api.request;

import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.api.ReservationRequest;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.Specification;
import org.joda.time.Interval;

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
     * Time slot for which the availability should be checked.
     */
    private Interval slot;

    /**
     * To be checked if it is available in specified {@link #slot},
     */
    private Specification specification;

    /**
     * To be checked if it is available to be reused in specified {@link #slot},
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
    }

    /**
     * Constructor.
     *
     * @param securityToken sets the {@link #securityToken}
     */
    public AvailabilityCheckRequest(SecurityToken securityToken)
    {
        super(securityToken);
    }

    /**
     * Constructor.
     *
     * @param securityToken        sets the {@link #securityToken}
     * @param slot                 sets the {@link #slot}
     * @param specification        sets the {@link #specification}
     * @param reservationRequestId sets the {@link #reservationRequestId}
     */
    public AvailabilityCheckRequest(SecurityToken securityToken, Interval slot,
            Specification specification, String reservationRequestId)
    {
        super(securityToken);
        this.slot = slot;
        this.specification = specification;
        this.reservationRequestId = reservationRequestId;
    }

    /**
     * Constructor.
     *
     * @param securityToken sets the {@link #securityToken}
     * @param reservationRequest to be initialized from
     */
    public AvailabilityCheckRequest(SecurityToken securityToken, ReservationRequest reservationRequest)
    {
        this(securityToken, reservationRequest.getSlot(), reservationRequest.getSpecification(),
                reservationRequest.getReusedReservationRequestId());
    }

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

    /**
     * @return {@link #slot}
     */
    public Interval getSlot()
    {
        return slot;
    }

    /**
     * @param slot sets the {@link #slot}
     */
    public void setSlot(Interval slot)
    {
        this.slot = slot;
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

    private static final String PURPOSE = "purpose";
    private static final String SLOT = "slot";
    private static final String SPECIFICATION = "specification";
    private static final String RESERVATION_REQUEST = "reservationRequestId";
    private static final String IGNORED_RESERVATION_REQUEST = "ignoredReservationRequestId";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(PURPOSE, purpose);
        dataMap.set(SLOT, slot);
        dataMap.set(SPECIFICATION, specification);
        dataMap.set(RESERVATION_REQUEST, reservationRequestId);
        dataMap.set(IGNORED_RESERVATION_REQUEST, ignoredReservationRequestId);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        purpose = dataMap.getEnum(PURPOSE, ReservationRequestPurpose.class);
        slot = dataMap.getInterval(SLOT);
        specification = dataMap.getComplexType(SPECIFICATION, Specification.class);
        reservationRequestId = dataMap.getString(RESERVATION_REQUEST);
        ignoredReservationRequestId = dataMap.getString(IGNORED_RESERVATION_REQUEST);
    }
}
