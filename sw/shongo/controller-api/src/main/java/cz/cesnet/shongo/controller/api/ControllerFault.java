package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.fault.CommonFault;
import cz.cesnet.shongo.fault.Fault;

/**
 * Domain controller faults.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ControllerFault extends CommonFault
{
    public static final Fault PREPROCESSOR_FAILED = new SimpleFault(100, "");
    public static final Fault SCHEDULER_FAILED = new SimpleFault(101, "Scheduler failed");

    public static final int RESERVATION_REQUEST_NOT_MODIFIABLE = 200;

    @Override
    protected void fill()
    {
        super.fill();
    }
}
