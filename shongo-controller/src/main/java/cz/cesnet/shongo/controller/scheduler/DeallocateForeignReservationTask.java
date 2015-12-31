package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.ForeignDomainConnectException;
import cz.cesnet.shongo.controller.api.Domain;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.booking.Allocation;
import cz.cesnet.shongo.controller.booking.reservation.AbstractForeignReservation;
import cz.cesnet.shongo.controller.booking.reservation.ForeignRoomReservation;
import cz.cesnet.shongo.controller.booking.reservation.ReservationManager;
import cz.cesnet.shongo.controller.booking.resource.ForeignResourceReservation;
import cz.cesnet.shongo.controller.domains.InterDomainAgent;
import cz.cesnet.shongo.controller.notification.AbstractNotification;
import org.joda.time.Interval;

import javax.persistence.EntityManager;
import java.util.List;

/**
 * Represents a {@link DeallocateReservationTask} for deallocating {@link ForeignResourceReservation}.
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class DeallocateForeignReservationTask extends DeallocateReservationTask
{
    public DeallocateForeignReservationTask(AbstractForeignReservation reservation)
    {
        super(reservation);
    }

    @Override
    protected List<AbstractNotification> perform(Interval slot, Scheduler.Result result, EntityManager entityManager,
                                                 ReservationManager reservationManager, AuthorizationManager authorizationManager)
            throws ForeignDomainConnectException
    {
        AbstractForeignReservation reservation = getReservation();
        Allocation allocation = reservation.getAllocation();

        // Perform foreign deallocate only for the latest reservation request
        if (allocation == null || reservation.equals(allocation.getCurrentReservation())) {
            // Check if foreign reservation even exists
            if (reservation instanceof ForeignRoomReservation) {
                ForeignRoomReservation foreignResourceReservation = (ForeignRoomReservation) reservation;
                if (!foreignResourceReservation.getForeignReservationRequestsIds().isEmpty())
                {
                    throw new TodoImplementException("wait for finalization...");
                }
            }
            if (reservation.getForeignReservationRequestId() != null) {
                Domain domain = reservation.getDomain().toApi();
                if (!InterDomainAgent.getInstance().getConnector().deallocateReservation(domain, reservation.getForeignReservationRequestId())) {
                    // Process error
                    throw new TodoImplementException("process returned error");
                }
            }
        }
        return super.perform(slot, result, entityManager, reservationManager, authorizationManager);
    }

    @Override
    protected AbstractForeignReservation getReservation()
    {
        return (AbstractForeignReservation) super.getReservation();
    }
}
