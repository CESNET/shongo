package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.controller.request.CompartmentRequestManager;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a component of a domain controller that is responsible for scheduling resources for compartment requests.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Scheduler extends Component
{
    private static Logger logger = LoggerFactory.getLogger(Scheduler.class);

    @Override
    public void init()
    {
        super.init();
    }

    @Override
    public void destroy()
    {
        super.destroy();
    }

    /**
     * Run scheduler for a given interval.
     *
     * @param interval
     */
    public void run(Interval interval)
    {
        checkInitialized();

        logger.debug("Running scheduler...");

        CompartmentRequestManager compartmentRequestManager = new CompartmentRequestManager(getEntityManager());

        throw new RuntimeException("TODO: Implement");
    }
}
