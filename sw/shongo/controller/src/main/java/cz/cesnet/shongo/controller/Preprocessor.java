package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.controller.request.ReservationRequestManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Preprocessor
{
    private static Logger logger = LoggerFactory.getLogger(Preprocessor.class);

    ReservationRequestManager reservationRequestManager;

    public void run(Epoch epoch)
    {
        logger.debug("Running preprocessor...");

        reservationRequestManager.list();

        // TODO: continue on preprocessor implementation

    }
}
