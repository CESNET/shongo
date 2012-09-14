package cz.cesnet.shongo.controller.reservation;

import cz.cesnet.shongo.AbstractManager;
import cz.cesnet.shongo.controller.Cache;
import cz.cesnet.shongo.controller.allocationaold.AllocatedItem;
import cz.cesnet.shongo.controller.request.ReservationRequest;
import cz.cesnet.shongo.controller.request.ReservationRequestSet;
import cz.cesnet.shongo.fault.TodoImplementException;
import org.joda.time.Interval;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import java.util.List;

/**
 * Manager for {@link Reservation}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationManager extends AbstractManager
{
    /**
     * @param entityManager sets the {@link #entityManager}
     */
    public ReservationManager(EntityManager entityManager)
    {
        super(entityManager);
    }

    /**
     * @param reservation to be created in the database
     */
    public void create(Reservation reservation)
    {
        super.create(reservation);
    }

    /**
     * @param reservation to be updated in the database
     */
    public void update(Reservation reservation)
    {
        super.update(reservation);
    }

    /**
     * @param Reservation to be deleted in the database
     */
    public void delete(Reservation Reservation, Cache cache)
    {
        // Remove all allocated virtual rooms from virtual rooms database
        List<Reservation> childReservations = Reservation.getChildReservations();
        for (Reservation childReservation : childReservations) {
            throw new TodoImplementException();
            //cache.removeAllocatedItem(childReservation);
        }
        super.delete(Reservation);
    }

    /**
     * @param reservationRequest for which the {@link Reservation} should be returned
     * @return {@link Reservation} for the given {@link ReservationRequest} or null if doesn't exists
     */
    public Reservation getByReservationRequest(ReservationRequest reservationRequest)
    {
        return getByReservationRequest(reservationRequest.getId());
    }

    /**
     * @param reservationRequestId of the {@link ReservationRequest} for which the {@link Reservation} should be returned
     * @return {@link Reservation} for the given {@link ReservationRequest} or null if doesn't exists
     */
    public Reservation getByReservationRequest(Long reservationRequestId)
    {
        try {
            Reservation reservation = entityManager.createQuery(
                    "SELECT reservation FROM Reservation reservation WHERE reservation.reservationRequest.id = :id",
                    Reservation.class).setParameter("id", reservationRequestId)
                    .getSingleResult();
            return reservation;
        }
        catch (NoResultException exception) {
            return null;
        }
    }

    /**
     * @param reservationRequestSet from which the {@link Reservation}s should be
     *                              returned.
     * @return list of {@link Reservation}s from given {@code reservationRequestSet}
     */
    public List<Reservation> listByReservationRequestSet(ReservationRequestSet reservationRequestSet)
    {
        return listByReservationRequestSet(reservationRequestSet.getId());
    }

    /**
     * @param reservationRequestSetId for {@link ReservationRequestSet} from which the {@link Reservation}s should be
     *                                returned.
     * @return list of {@link Reservation}s from {@link ReservationRequestSet} with
     *         given {@code reservationRequestId}
     */
    public List<Reservation> listByReservationRequestSet(Long reservationRequestSetId)
    {
        List<Reservation> allocatedCompartments = entityManager.createQuery(
                "SELECT reservation FROM Reservation reservation"
                        + " WHERE reservation.reservationRequest.id IN ("
                        + " SELECT reservationRequest.id FROM ReservationRequestSet reservationRequestSet"
                        + " LEFT JOIN reservationRequestSet.reservationRequests reservationRequest"
                        + " WHERE reservationRequestSet.id = :id)",
                Reservation.class)
                .setParameter("id", reservationRequestSetId)
                .getResultList();
        return allocatedCompartments;
    }

    /**
     * @param interval in which the requested {@link Reservation}s should start
     * @return list of {@link Reservation}s starting in given {@code interval}
     */
    public List<Reservation> listByInterval(Interval interval)
    {
        List<Reservation> allocatedCompartments = entityManager.createQuery(
                "SELECT reservation FROM Reservation reservation "
                        + "WHERE reservation.reservationRequest.requestedSlot.start BETWEEN :start AND :end",
                Reservation.class)
                .setParameter("start", interval.getStart())
                .setParameter("end", interval.getEnd())
                .getResultList();
        return allocatedCompartments;
    }


    /**
     * @param reservation to be marked for deletion
     */
    public void markedForDeletion(Reservation reservation)
    {
        if (true)
            throw new TodoImplementException();
        //reservation.setReservationRequest(null);
        update(reservation);
    }

    /**
     * Delete {@link Reservation}s which were marked by {@link #markedForDeletion(Reservation)}.
     *
     * @param cache from which the {@link Reservation}s are also deleted
     */
    public void deleteAllMarked(Cache cache)
    {
        List<Reservation> reservations = entityManager.createQuery(
                "SELECT reservation FROM Reservation reservation WHERE reservation.reservationRequest IS NULL",
                Reservation.class)
                .getResultList();
        for (Reservation reservation : reservations) {
            delete(reservation, cache);
        }
    }
}
