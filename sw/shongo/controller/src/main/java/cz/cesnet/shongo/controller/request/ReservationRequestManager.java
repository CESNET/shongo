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
     * Create a new reservation in the database.
     *
     * @param reservationRequest
     */
    public void create(ReservationRequest reservationRequest)
    {
        super.create(reservationRequest);
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
        super.delete(reservationRequest);
    }

    /**
     * @param reservationRequestId
     * @return {@link ReservationRequest} with given identifier or null if the request doesn't exist
     */
    public ReservationRequest get(long reservationRequestId)
    {
        try {
            ReservationRequest reservationRequest = entityManager.createQuery(
                    "SELECT request FROM ReservationRequest request WHERE request.id = :id",
                    ReservationRequest.class).setParameter("id", reservationRequestId)
                    .getSingleResult();
            return reservationRequest;
        }
        catch (NoResultException exception) {
            return null;
        }
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
}
