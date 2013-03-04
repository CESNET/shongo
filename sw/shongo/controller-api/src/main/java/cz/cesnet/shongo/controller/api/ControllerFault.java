package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.controller.fault.IdentifierWrongDomainException;
import cz.cesnet.shongo.controller.fault.IdentifierWrongTypeException;
import cz.cesnet.shongo.controller.fault.ReservationRequestNotModifiableException;
import cz.cesnet.shongo.fault.CommonFault;
import cz.cesnet.shongo.fault.Fault;

/**
 * Domain controller faults.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ControllerFault extends CommonFault
{
    /**
     * @see IdentifierWrongDomainException
     */
    public static final int IDENTIFIER_WRONG_FORMAT = 100;

    /**
     * @see IdentifierWrongDomainException
     */
    public static final int IDENTIFIER_WRONG_DOMAIN = 101;

    /**
     * @see IdentifierWrongTypeException
     */
    public static final int IDENTIFIER_WRONG_TYPE = 102;

    /**
     * Reservation request date/time slot duration was specified empty.
     */
    public static final Fault RESERVATION_REQUEST_EMPTY_DURATION = new SimpleFault(
            200, "Date/time slot duration must not be empty.");

    /**
     * @see ReservationRequestNotModifiableException
     */
    public static final int RESERVATION_REQUEST_NOT_MODIFIABLE = 201;

    @Override
    protected void fill()
    {
        add(RESERVATION_REQUEST_NOT_MODIFIABLE, ReservationRequestNotModifiableException.class);
        add(IDENTIFIER_WRONG_DOMAIN, IdentifierWrongDomainException.class);
        add(IDENTIFIER_WRONG_TYPE, IdentifierWrongTypeException.class);
        super.fill();
    }
}
