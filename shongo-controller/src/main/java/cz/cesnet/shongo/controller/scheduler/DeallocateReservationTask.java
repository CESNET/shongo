package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.booking.executable.Executable;
import cz.cesnet.shongo.controller.booking.request.ReservationRequestManager;
import cz.cesnet.shongo.controller.booking.reservation.Reservation;
import cz.cesnet.shongo.controller.booking.reservation.ReservationManager;
import cz.cesnet.shongo.controller.booking.room.RoomEndpoint;
import cz.cesnet.shongo.controller.notification.AbstractNotification;
import cz.cesnet.shongo.controller.notification.ReservationNotification;
import cz.cesnet.shongo.controller.notification.RoomNotification;
import org.joda.time.Interval;

import javax.persistence.EntityManager;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

//TODO
public class DeallocateReservationTask
{
    private final Reservation reservation;

    public DeallocateReservationTask(Reservation reservation)
    {
        this.reservation = reservation;
    }

    /**
     * @return newly allocated {@link Reservation}
     * @throws SchedulerException when the {@link Reservation} cannot be allocated
     */
    protected List<AbstractNotification> deallocate(Interval slot, Scheduler.Result result,
                                                    EntityManager entityManager, ReservationManager reservationManager,
                                                    AuthorizationManager authorizationManager) throws SchedulerException
    {
        // Delete all reservations which should be deleted
        List<AbstractNotification> reservationNotifications = new LinkedList<>();
        ReservationNotification.Deleted reservationNotificationDeleted =
                new ReservationNotification.Deleted(reservation, authorizationManager);
        reservationNotifications.add(reservationNotificationDeleted);

        reservation.setAllocation(null);
        if (reservation.getSlotEnd().isAfter(slot.getStart())) {
            Collection<Reservation> reservationItems = new LinkedList<>();
            ReservationManager.getAllReservations(reservation, reservationItems);
            List<AbstractNotification> notifications = new LinkedList<>();
            for (Reservation reservationItem : reservationItems) {
                Executable executable = reservationItem.getExecutable();
                if (executable instanceof RoomEndpoint) {
                    RoomEndpoint roomEndpoint = (RoomEndpoint) reservationItem.getExecutable();
                    if (roomEndpoint.isParticipantNotificationEnabled()) {
                        notifications.add(new RoomNotification.RoomDeleted(roomEndpoint, entityManager));
                    }
                }
            }
            reservationNotifications.addAll(notifications);
        }
        reservationManager.delete(reservation, slot.getStart(), authorizationManager);
        result.deletedReservations++;

        return reservationNotifications;
    }

    public Reservation getReservation()
    {
        return reservation;
    }
}
