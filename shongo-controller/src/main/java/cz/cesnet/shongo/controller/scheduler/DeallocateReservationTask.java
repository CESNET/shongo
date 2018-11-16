package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.controller.ForeignDomainConnectException;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.booking.executable.Executable;
import cz.cesnet.shongo.controller.booking.reservation.Reservation;
import cz.cesnet.shongo.controller.booking.reservation.ReservationManager;
import cz.cesnet.shongo.controller.booking.room.RoomEndpoint;
import cz.cesnet.shongo.controller.notification.AbstractNotification;
import org.joda.time.Interval;

import javax.persistence.EntityManager;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Represents a {@link Scheduler} task for deallocating {@link Reservation}.
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class DeallocateReservationTask
{
    /**
     * {@link Reservation} to deallocate
     */
    private final Reservation reservation;

    /**
     * Constructor
     */
    public DeallocateReservationTask(Reservation reservation)
    {
        this.reservation = reservation;
    }

    /**
     * Perform the {@link DeallocateReservationTask}.
     *
     * @return generated {@link AbstractNotification}
     */
    protected List<AbstractNotification> perform(Interval slot, Scheduler.Result result,
                                                    EntityManager entityManager, ReservationManager reservationManager,
                                                    AuthorizationManager authorizationManager) throws ForeignDomainConnectException
    {
        Reservation reservation = this.reservation;

        List<AbstractNotification> reservationNotifications = new LinkedList<>();
/*        ReservationNotification.Deleted reservationNotificationDeleted =
                new ReservationNotification.Deleted(reservation, authorizationManager);
        reservationNotifications.add(reservationNotificationDeleted);*/

        reservation.setAllocation(null);
        if (reservation.getSlotEnd() != null && reservation.getSlotEnd().isAfter(slot.getStart())) {
            Collection<Reservation> reservationItems = new LinkedList<>();
            ReservationManager.getAllReservations(reservation, reservationItems);
            List<AbstractNotification> notifications = new LinkedList<>();
            for (Reservation reservationItem : reservationItems) {
                Executable executable = reservationItem.getExecutable();
                if (executable instanceof RoomEndpoint) {
                    RoomEndpoint roomEndpoint = (RoomEndpoint) reservationItem.getExecutable();
                    if (roomEndpoint.isParticipantNotificationEnabled()) {
                        //notifications.add(new RoomNotification.RoomDeleted(roomEndpoint, entityManager));
                    }
                }
            }
            reservationNotifications.addAll(notifications);
        }
        reservationManager.delete(reservation, slot.getStart(), authorizationManager);
        result.deletedReservations++;

        return reservationNotifications;
    }

    /**
     * @return reservation for this {@link DeallocateReservationTask}
     */
    protected Reservation getReservation()
    {
        return reservation;
    }
}
