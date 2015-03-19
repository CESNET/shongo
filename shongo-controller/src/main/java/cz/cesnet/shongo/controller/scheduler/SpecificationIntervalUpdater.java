package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.controller.api.rpc.ReservationService;
import cz.cesnet.shongo.controller.booking.specification.Specification;
import org.joda.time.DateTime;
import org.joda.time.Interval;

/**
 * Interface which can be implemented by a {@link Specification} to update {@link Interval}
 * for {@link ReservationService#checkPeriodicAvailability} and {@link Scheduler}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public interface SpecificationIntervalUpdater
{
    /**
     * @param interval
     * @return new {@link org.joda.time.Interval}
     */
    public Interval updateInterval(Interval interval, DateTime minimumDateTime);
}
