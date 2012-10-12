package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.controller.fault.ReservationRequestNotModifiableException;
import cz.cesnet.shongo.fault.CommonFault;

/**
 * Domain controller faults.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ControllerFault extends CommonFault
{
    public static final int RESERVATION_REQUEST_NOT_MODIFIABLE = 200;

    @Override
    protected void fill()
    {
        add(RESERVATION_REQUEST_NOT_MODIFIABLE, ReservationRequestNotModifiableException.class);
        super.fill();
    }
}
