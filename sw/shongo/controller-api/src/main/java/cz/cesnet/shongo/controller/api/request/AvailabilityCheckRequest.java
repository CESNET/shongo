package cz.cesnet.shongo.controller.api.request;

import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.Specification;
import org.joda.time.Interval;

/**
 * {@link cz.cesnet.shongo.controller.api.request.AbstractRequest} for listing objects.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AvailabilityCheckRequest extends AbstractRequest
{
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
    private String reusedReservationRequestId;

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
     * @return {@link #reusedReservationRequestId}
     */
    public String getReusedReservationRequestId()
    {
        return reusedReservationRequestId;
    }

    /**
     * @param reusedReservationRequestId sets the {@link #reusedReservationRequestId}
     */
    public void setReusedReservationRequestId(String reusedReservationRequestId)
    {
        this.reusedReservationRequestId = reusedReservationRequestId;
    }

    private static final String SLOT = "slot";
    private static final String SPECIFICATION = "specification";
    private static final String RESERVATION_REQUEST = "reservationRequestId";
    private static final String REUSED_RESERVATION_REQUEST = "reusedReservationRequestId";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(SLOT, slot);
        dataMap.set(SPECIFICATION, specification);
        dataMap.set(RESERVATION_REQUEST, reservationRequestId);
        dataMap.set(REUSED_RESERVATION_REQUEST, reusedReservationRequestId);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        slot = dataMap.getInterval(SLOT);
        specification = dataMap.getComplexType(SPECIFICATION, Specification.class);
        reservationRequestId = dataMap.getString(RESERVATION_REQUEST);
        reusedReservationRequestId = dataMap.getString(REUSED_RESERVATION_REQUEST);
    }
}
