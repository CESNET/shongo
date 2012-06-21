package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.common.AbstractManager;
import org.joda.time.Interval;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import java.util.List;

/**
 * Manager for {@link ReservationRequest}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see AbstractManager
 */
public class ReservationRequestManager extends AbstractManager
{
    /**
     * Constructor.
     *
     * @param entityManager
     */
    public ReservationRequestManager(EntityManager entityManager)
    {
        super(entityManager);
    }

    /**
     * Create new reservation in the database.
     *
     * @param reservationRequest
     */
    public void create(ReservationRequest reservationRequest, String domain)
    {
        Transaction transaction = beginTransaction();

        super.create(reservationRequest);

        entityManager.flush();

        transaction.commit();
    }

    /**
     * Update existing reservation request in the database.
     *
     * @param reservationRequest
     */
    public void update(ReservationRequest reservationRequest)
    {
        Transaction transaction = beginTransaction();

        super.update(reservationRequest);

        ReservationRequestStateManager.setState(entityManager, reservationRequest,
                ReservationRequest.State.NOT_PREPROCESSED);

        transaction.commit();
    }

    /**
     * Delete existing reservation request in the database
     *
     * @param reservationRequest
     */
    public void delete(ReservationRequest reservationRequest)
    {
        Transaction transaction = beginTransaction();

        super.delete(reservationRequest);

        transaction.commit();
    }

    /**
     * @return list all reservation requests in the database.
     */
    public List<ReservationRequest> list()
    {
        List<ReservationRequest> reservationRequestList = entityManager
                .createQuery("SELECT request FROM ReservationRequest request", ReservationRequest.class)
                .getResultList();
        return reservationRequestList;
    }

    /**
     * @return list all reservation requests in the database which aren't preprocessed in given interval.
     */
    public List<ReservationRequest> listNotPreprocessed(Interval interval)
    {
        List<ReservationRequest> reservationRequestList = entityManager
                .createQuery("SELECT request FROM ReservationRequest request WHERE request NOT IN (" +
                        "SELECT state.reservationRequest FROM ReservationRequestPreprocessedState state "
                        + "WHERE state.start <= :from AND state.end >= :to)",
                        ReservationRequest.class).setParameter("from", interval.getStart())
                .setParameter("to", interval.getEnd())
                .getResultList();
        return reservationRequestList;
    }

    /**
     * @param id
     * @return {@link cz.cesnet.shongo.controller.request.ReservationRequest} with given identifier
     *         or null if the request not exists
     */
    public ReservationRequest get(long id)
    {
        try {
            ReservationRequest reservationRequest = entityManager.createQuery(
                    "SELECT request FROM ReservationRequest request WHERE request.id = :id",
                    ReservationRequest.class).setParameter("id", id)
                    .getSingleResult();
            return reservationRequest;
        }
        catch (NoResultException exception) {
            return null;
        }
    }
}
