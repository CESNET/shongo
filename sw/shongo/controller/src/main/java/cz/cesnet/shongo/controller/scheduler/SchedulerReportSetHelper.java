package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.reservation.Reservation;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Helper class for automatic generated {@link SchedulerReportSet}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class SchedulerReportSetHelper
{
    public static SchedulerReportSet.CollidingReservationsReport createCollidingReservations(
            Collection<? extends Reservation> collidingReservations)
    {
        List<String> collidingReservationIds = new LinkedList<String>();
        for (Reservation reservation : collidingReservations) {
            collidingReservationIds.add(ObjectIdentifier.formatId(reservation));
        }
        return new SchedulerReportSet.CollidingReservationsReport(collidingReservationIds);
    }

    public static SchedulerReportSet.ReallocatingReservationsReport createReallocatingReservations(
            Collection<? extends Reservation> collidingReservations)
    {
        List<String> collidingReservationIds = new LinkedList<String>();
        for (Reservation reservation : collidingReservations) {
            collidingReservationIds.add(ObjectIdentifier.formatId(reservation));
        }
        return new SchedulerReportSet.ReallocatingReservationsReport(collidingReservationIds);
    }
}
