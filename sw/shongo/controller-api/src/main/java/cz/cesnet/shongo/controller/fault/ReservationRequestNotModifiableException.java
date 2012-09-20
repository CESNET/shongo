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
     *
     * @param reservationRequestIdentifier
     */
    public ReservationRequestNotModifiableException(String reservationRequestIdentifier)
    {
        super(ControllerFault.RESERVATION_REQUEST_NOT_MODIFIABLE,
                "Reservation request '%s' cannot be modified or deleted.", reservationRequestIdentifier);
        this.reservationRequestIdentifier = reservationRequestIdentifier;
    }

    /**
     * Constructor.
     *
     * @param message message containing parsed parameters
     */
    public ReservationRequestNotModifiableException(Message message)
    {
        this(message.getParameter("reservationRequestIdentifier"));
    }

    /**
     * @return {@link #reservationRequestIdentifier}
     */
    public String getReservationRequestIdentifier()
    {
        return reservationRequestIdentifier;
    }
}
