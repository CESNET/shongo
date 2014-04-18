package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.controller.booking.reservation.Reservation;
import cz.cesnet.shongo.controller.booking.specification.Specification;
import org.joda.time.Interval;

/**
 * Interface which can be implemented by a {@link Specification} and it tells that the {@link Specification} should be
 * allocated to a {@link Reservation} by a {@link ReservationTask} which can be obtained by the
 * {@link #createReservationTask} method.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public interface ReservationTaskProvider
{
    /**
     * @param schedulerContext
     * @param slot
     * @return new instance of {@link ReservationTask}.
     */
    public ReservationTask createReservationTask(SchedulerContext schedulerContext, Interval slot)
            throws SchedulerException;
}
