package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.AbstractManager;
import cz.cesnet.shongo.fault.EntityNotFoundException;
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
     * @param reservationRequest to be validated
     */
    private void validate(ReservationRequest reservationRequest) throws IllegalArgumentException
    {
        if (reservationRequest.getType() == null) {
            throw new IllegalArgumentException("Reservation request must have type set!");
        }
    }

    /**
     * Create a new reservation in the database.
     *
     * @param reservationRequest
     */
    public void create(ReservationRequest reservationRequest)
    {
        validate(reservationRequest);

        super.create(reservationRequest);
    }

    /**
     * Update existing reservation request in the database.
     *
     * @param reservationRequest
     */
    public void update(ReservationRequest reservationRequest)
    {
        validate(reservationRequest);

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

        // Delete all compartment requests
        CompartmentRequestManager compartmentRequestManager = new CompartmentRequestManager(entityManager);
        for (CompartmentRequest compartmentRequest : compartmentRequestManager
                .listByReservationRequest(reservationRequest)) {
            compartmentRequestManager.delete(compartmentRequest);
        }

        // Clear reservation request state
        ReservationRequestStateManager.clear(entityManager, reservationRequest);

        super.delete(reservationRequest);

        transaction.commit();
    }

    /**
     * @param reservationRequestId
     * @return {@link ReservationRequest} with given identifier
     * @throws EntityNotFoundException when reservation request doesn't exist
     */
    public ReservationRequest get(Long reservationRequestId) throws EntityNotFoundException
    {
        try {
            ReservationRequest reservationRequest = entityManager.createQuery(
                    "SELECT request FROM ReservationRequest request WHERE request.id = :id",
                    ReservationRequest.class).setParameter("id", reservationRequestId)
                    .getSingleResult();
            return reservationRequest;
        }
        catch (NoResultException exception) {
            throw new EntityNotFoundException(ReservationRequest.class, reservationRequestId);
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
