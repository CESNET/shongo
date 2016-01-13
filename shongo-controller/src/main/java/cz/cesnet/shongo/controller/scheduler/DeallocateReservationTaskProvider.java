package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.controller.booking.reservation.AbstractForeignReservation;
import cz.cesnet.shongo.controller.booking.reservation.ForeignRoomReservation;
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
        if (reservation instanceof ForeignRoomReservation) {
            return new DeallocateForeignRoomReservationTask((ForeignRoomReservation) reservation);
        }
        else if (reservation instanceof AbstractForeignReservation) {
            return new DeallocateForeignReservationTask((AbstractForeignReservation) reservation);
        }
        else {
            return new DeallocateReservationTask(reservation);
        }
    }
}
