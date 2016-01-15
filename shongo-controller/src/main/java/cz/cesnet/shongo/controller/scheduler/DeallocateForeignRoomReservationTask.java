package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.controller.ControllerReportSetHelper;
import cz.cesnet.shongo.controller.ForeignDomainConnectException;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.alias.AliasReservation;
import cz.cesnet.shongo.controller.booking.domain.Domain;
import cz.cesnet.shongo.controller.booking.reservation.AbstractForeignReservation;
import cz.cesnet.shongo.controller.booking.reservation.ForeignRoomReservation;
import cz.cesnet.shongo.controller.booking.reservation.ReservationManager;
import cz.cesnet.shongo.controller.booking.resource.ResourceManager;
import cz.cesnet.shongo.controller.notification.AbstractNotification;
import org.joda.time.Interval;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 *
 * Represents a {@link DeallocateReservationTask} for deallocating {@link ForeignRoomReservation}.
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class DeallocateForeignRoomReservationTask extends DeallocateForeignReservationTask
{
    public DeallocateForeignRoomReservationTask(ForeignRoomReservation reservation)
    {
        super(reservation);
    }

    @Override
    protected List<AbstractNotification> perform(Interval slot, Scheduler.Result result, EntityManager entityManager, ReservationManager reservationManager, AuthorizationManager authorizationManager) throws ForeignDomainConnectException
    {
        // Check if is ready for single reservation deallocation and prepare
        if (isDeallocatable() && notDeallocatableReady()) {
            ForeignRoomReservation reservation = getReservation();
            Iterator<String> iterator = reservation.getForeignReservationRequestsIds().iterator();
            while (iterator.hasNext()) {
                String foreignReservationRequestId = iterator.next();
                if (reservation.getForeignReservationRequestId() == null) {
                    Domain domain = getDomain(foreignReservationRequestId, entityManager);
                    reservation.setDomain(domain);
                    reservation.setForeignReservationRequestId(foreignReservationRequestId);
                }
                if (!foreignReservationRequestId.equals(getReservation().getForeignReservationRequestId())) {
                    createReservationForDeletion(foreignReservationRequestId, entityManager);
                }
                iterator.remove();
            }
        }
        return super.perform(slot, result, entityManager, reservationManager, authorizationManager);
    }

    @Override
    protected ForeignRoomReservation getReservation()
    {
        return (ForeignRoomReservation) super.getReservation();
    }

    protected boolean notDeallocatableReady()
    {
        if (getReservation().getForeignReservationRequestId() != null) {
            return false;
        }
        if (getReservation().getForeignReservationRequestsIds().isEmpty()) {
            return false;
        }
        return true;
    }

    private void createReservationForDeletion(String foreignReservationRequestId, EntityManager entityManager)
    {
        ReservationManager reservationManager = new ReservationManager(entityManager);

        Domain domain = getDomain(foreignReservationRequestId, entityManager);

        ForeignRoomReservation reservation = new ForeignRoomReservation();
        reservation.setUserId(Authorization.ROOT_USER_ID);
        reservation.setComplete(true);
        reservation.setDomain(domain);
        reservation.setForeignReservationRequestId(foreignReservationRequestId);

        reservationManager.create(reservation);
    }

    private Domain getDomain(String foreignReservationRequestId, EntityManager entityManager)
    {
        ResourceManager resourceManager = new ResourceManager(entityManager);
        String domainName = ObjectIdentifier.parseForeignDomain(foreignReservationRequestId);
        return resourceManager.getDomainByName(domainName);
    }
}
