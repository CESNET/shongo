package cz.cesnet.shongo.controller.fault;

import cz.cesnet.shongo.controller.api.ControllerFault;
import cz.cesnet.shongo.fault.FaultException;
import cz.cesnet.shongo.fault.SerializableException;

/**
 * Exception to be thrown when {@link cz.cesnet.shongo.controller.api.ReservationRequest} cannot be modified or deleted.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationRequestNotModifiableException extends FaultException implements SerializableException
{
    /**
     * Shongo-id of {@link cz.cesnet.shongo.controller.api.ReservationRequest}.
     */
    private String reservationRequestId;

    /**
     * Constructor.
     */
    public ReservationRequestNotModifiableException()
    {
    }

    /**
     * Constructor.
     *
     * @param reservationRequestId
     */
    public ReservationRequestNotModifiableException(String reservationRequestId)
    {
        this.reservationRequestId = reservationRequestId;
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

    @Override
    public int getCode()
    {
        return ControllerFault.RESERVATION_REQUEST_NOT_MODIFIABLE;
    }

    @Override
    public String getMessage()
    {
        return ControllerFault.formatMessage("Reservation request '%s' cannot be modified or deleted.",
                reservationRequestId);
    }
}
