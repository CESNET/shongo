package cz.cesnet.shongo.controller.allocation;

import cz.cesnet.shongo.AbstractManager;
import cz.cesnet.shongo.controller.Cache;
import org.joda.time.Interval;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import java.util.List;

/**
 * Manager for {@link AllocatedCompartment}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AllocatedCompartmentManager extends AbstractManager
{
    /**
     * @param entityManager sets the {@link #entityManager}
     */
    public AllocatedCompartmentManager(EntityManager entityManager)
    {
        super(entityManager);
    }

    /**
     * @param allocatedCompartment to be created in the database
     */
    public void create(AllocatedCompartment allocatedCompartment)
    {
        super.create(allocatedCompartment);
    }

    /**
     * @param allocatedCompartment to be updated in the database
     */
    public void update(AllocatedCompartment allocatedCompartment)
    {
        super.update(allocatedCompartment);
    }

    /**
     * @param allocatedCompartment to be deleted in the database
     */
    public void delete(AllocatedCompartment allocatedCompartment, Cache cache)
    {
        // Remove all allocated virtual rooms from virtual rooms database
        List<AllocatedItem> allocatedItems = allocatedCompartment.getAllocatedItems();
        for (AllocatedItem allocatedItem : allocatedItems) {
            cache.removeAllocatedItem(allocatedItem);
        }
        super.delete(allocatedCompartment);
    }

    /**
     * @param compartmentRequest
     * @return {@link AllocatedCompartment} for the given {@link CompartmentRequest} or null if doesn't exists
     */
    public AllocatedCompartment getByCompartmentRequest(CompartmentRequest compartmentRequest)
    {
        return getByCompartmentRequest(compartmentRequest.getId());
    }

    /**
     * @param compartmentRequestId for {@link CompartmentRequest}
     * @return {@link AllocatedCompartment} for {@link CompartmentRequest} with given {@code compartmentRequestId}
     *         or null if doesn't exists
     */
    public AllocatedCompartment getByCompartmentRequest(Long compartmentRequestId)
    {
        try {
            AllocatedCompartment allocatedCompartment = entityManager.createQuery(
                    "SELECT alloc FROM AllocatedCompartment alloc WHERE alloc.compartmentRequest.id = :id",
                    AllocatedCompartment.class).setParameter("id", compartmentRequestId)
                    .getSingleResult();
            return allocatedCompartment;
        }
        catch (NoResultException exception) {
            return null;
        }
    }

    /**
     * @param reservationRequestId for {@link ReservationRequest} from which the {@link AllocatedCompartment}s should be
     *                             returned.
     * @return list of {@link AllocatedCompartment}s from {@link ReservationRequest} with
     *         given {@code reservationRequestId}
     */
    public List<AllocatedCompartment> listByReservationRequest(Long reservationRequestId)
    {
        List<AllocatedCompartment> allocatedCompartments = entityManager.createQuery(
                "SELECT allocation FROM AllocatedCompartment allocation"
                        + " WHERE allocation.compartmentRequest.reservationRequest.id = :id",
                AllocatedCompartment.class)
                .setParameter("id", reservationRequestId)
                .getResultList();
        return allocatedCompartments;
    }

    /**
     * @param interval in which the requested {@link AllocatedCompartment}s should start
     * @return list of {@link AllocatedCompartment}s starting in given {@code interval}
     */
    public List<AllocatedCompartment> listByInterval(Interval interval)
    {
        List<AllocatedCompartment> allocatedCompartments = entityManager.createQuery(
                "SELECT allocatedCompartment FROM AllocatedCompartment allocatedCompartment "
                        + "WHERE allocatedCompartment.compartmentRequest.requestedSlot.start BETWEEN :start AND :end",
                AllocatedCompartment.class)
                .setParameter("start", interval.getStart())
                .setParameter("end", interval.getEnd())
                .getResultList();
        return allocatedCompartments;
    }

    /**
     * @param reservationRequest for which the {@link AllocatedCompartment}s should be returned
     * @return list of {@link AllocatedCompartment}s for given {@code reservationRequest}
     */
    public List<AllocatedCompartment> listByReservationRequest(ReservationRequest reservationRequest)
    {
        return listByReservationRequest(reservationRequest.getId());
    }

    /**
     * @param allocatedCompartment to be marked for deletion
     */
    public void markedForDeletion(AllocatedCompartment allocatedCompartment)
    {
        allocatedCompartment.setCompartmentRequest(null);
        update(allocatedCompartment);
    }

    /**
     * Delete {@link AllocatedCompartment}s which were marked by {@link #markedForDeletion(AllocatedCompartment)}.
     *
     * @param cache from which the {@link AllocatedCompartment}s are also deleted
     */
    public void deleteAllMarked(Cache cache)
    {
        List<AllocatedCompartment> allocatedCompartments = entityManager.createQuery(
                "SELECT allocation FROM AllocatedCompartment allocation WHERE allocation.compartmentRequest IS NULL",
                AllocatedCompartment.class)
                .getResultList();
        for (AllocatedCompartment allocatedCompartment : allocatedCompartments) {
            delete(allocatedCompartment, cache);
        }
    }
}
