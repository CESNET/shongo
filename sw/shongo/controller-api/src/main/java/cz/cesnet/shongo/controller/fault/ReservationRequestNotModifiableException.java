package cz.cesnet.shongo.controller.fault;

import cz.cesnet.shongo.controller.api.ControllerFault;
import cz.cesnet.shongo.fault.CommonFault;
import cz.cesnet.shongo.fault.FaultException;

/**
 * Exception to be thrown when {@link cz.cesnet.shongo.controller.api.ReservationRequest} cannot be modified or deleted.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationRequestNotModifiableException extends FaultException
{
    /**
     * Identifier of {@link cz.cesnet.shongo.controller.api.ReservationRequest}.
     */
    private String reservationRequestIdentifier;

    /**
     * Constructor.
     */
    public ReservationRequestNotModifiableException()
    {
    }

    /**
     * Constructor.
     *
     * @param reservationRequestIdentifier
     */
    public ReservationRequestNotModifiableException(String reservationRequestIdentifier)
    {
        this.reservationRequestIdentifier = reservationRequestIdentifier;
    }

    /**
     * @return {@link #reservationRequestIdentifier}
     */
    public String getReservationRequestIdentifier()
    {
        return reservationRequestIdentifier;
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
                reservationRequestIdentifier);
    }
}
