package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.controller.fault.IdentifierWrongDomainException;
import cz.cesnet.shongo.controller.fault.IdentifierWrongTypeException;
import cz.cesnet.shongo.controller.fault.ReservationRequestNotModifiableException;
import cz.cesnet.shongo.fault.CommonFault;

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
    public static final int IDENTIFIER_WRONG_DOMAIN = 100;

    /**
     * @see IdentifierWrongTypeException
     */
    public static final int IDENTIFIER_WRONG_TYPE = 101;

    /**
     * @see ReservationRequestNotModifiableException
     */
    public static final int RESERVATION_REQUEST_NOT_MODIFIABLE = 200;

    @Override
    protected void fill()
    {
        add(RESERVATION_REQUEST_NOT_MODIFIABLE, ReservationRequestNotModifiableException.class);
        add(IDENTIFIER_WRONG_DOMAIN, IdentifierWrongDomainException.class);
        add(IDENTIFIER_WRONG_TYPE, IdentifierWrongTypeException.class);
        super.fill();
    }
}
