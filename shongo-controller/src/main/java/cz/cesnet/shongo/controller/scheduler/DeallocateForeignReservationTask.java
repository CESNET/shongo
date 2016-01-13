package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.ForeignDomainConnectException;
import cz.cesnet.shongo.controller.api.Domain;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.booking.Allocation;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.reservation.AbstractForeignReservation;
import cz.cesnet.shongo.controller.booking.reservation.ForeignRoomReservation;
import cz.cesnet.shongo.controller.booking.reservation.ReservationManager;
import cz.cesnet.shongo.controller.booking.resource.ForeignResourceReservation;
import cz.cesnet.shongo.controller.booking.resource.ResourceManager;
import cz.cesnet.shongo.controller.domains.InterDomainAgent;
import cz.cesnet.shongo.controller.notification.AbstractNotification;
import org.joda.time.Interval;

import javax.persistence.EntityManager;
import java.util.Iterator;
import java.util.List;

/**
 * Represents a {@link DeallocateReservationTask} for deallocating {@link AbstractForeignReservation}.
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

        // Perform foreign deallocate only for the latest reservation request
        if (isDeallocatable()) {
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

    /**
     * Check if {@code reservation} is deallocatable in foreign domain.
     *
     * @return true if can be deallocated in foreign domain.
     */
    protected boolean isDeallocatable()
    {
        Allocation allocation = getReservation().getAllocation();

        if (allocation == null) {
            return true;
        }
        // Is not modified reservation request
        if (allocation.getReservationRequest().getModifiedReservationRequest() == null) {
            return true;
        }
        if (getReservation().equals(allocation.getCurrentReservation())) {
            return true;
        }
        return false;
    }
}
