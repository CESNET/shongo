package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.controller.request.Specification;
import cz.cesnet.shongo.controller.reservation.Reservation;

/**
 * Interface which can be implemented by a {@link Specification} and it tells that the {@link Specification} should be
 * allocated to a {@link Reservation} by a {@link ReservationTask} which can be obtained by the
 * {@link #createReservationTask(ReservationTask.Context)} method.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public interface ReservationTaskProvider
{
    /**
     * @return new instance of {@link ReservationTask}.
     */
    public ReservationTask createReservationTask(ReservationTask.Context context);
}
