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
     * To be checked if it is available to be reused (to be provided) in specified {@link #slot},
     */
    private String reservationRequestId;

    /**
     * Identifier of reservation request whose reservations should be ignored.
     */
    private String providedReservationRequestId;

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
     * @return {@link #providedReservationRequestId}
     */
    public String getProvidedReservationRequestId()
    {
        return providedReservationRequestId;
    }

    /**
     * @param providedReservationRequestId sets the {@link #providedReservationRequestId}
     */
    public void setProvidedReservationRequestId(String providedReservationRequestId)
    {
        this.providedReservationRequestId = providedReservationRequestId;
    }

    private static final String SLOT = "slot";
    private static final String SPECIFICATION = "specification";
    private static final String RESERVATION_REQUEST = "reservationRequestId";
    private static final String PROVIDED_RESERVATION_REQUEST = "providedReservationRequestId";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(SLOT, slot);
        dataMap.set(SPECIFICATION, specification);
        dataMap.set(RESERVATION_REQUEST, reservationRequestId);
        dataMap.set(PROVIDED_RESERVATION_REQUEST, providedReservationRequestId);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        slot = dataMap.getInterval(SLOT);
        specification = dataMap.getComplexType(SPECIFICATION, Specification.class);
        reservationRequestId = dataMap.getString(RESERVATION_REQUEST);
        providedReservationRequestId = dataMap.getString(PROVIDED_RESERVATION_REQUEST);
    }
}
