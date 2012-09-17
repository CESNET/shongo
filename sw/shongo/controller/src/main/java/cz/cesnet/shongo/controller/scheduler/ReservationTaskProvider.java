package cz.cesnet.shongo.controller.scheduler;

/**
 * TODO:
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
