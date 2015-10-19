package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.controller.booking.reservation.Reservation;
import cz.cesnet.shongo.controller.booking.resource.ForeignResourceReservation;

/**
 * Provider of {@link DeallocateReservationTask}.
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class DeallocateReservationTaskProvider
{
    public static DeallocateReservationTask create(Reservation reservation) {
        if (reservation instanceof ForeignResourceReservation) {
            return new DeallocateForeignResourceReservationTask((ForeignResourceReservation) reservation);
        }
        else {
            return new DeallocateReservationTask(reservation);
        }
    }
}
