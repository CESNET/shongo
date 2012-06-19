package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.common.AbstractManager;
import cz.cesnet.shongo.common.Identifier;
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
    public void create(ReservationRequest reservationRequest)
    {
        validateReservationRequest(reservationRequest);

        Transaction transaction = beginTransaction();

        super.create(reservationRequest);

        transaction.commit();
    }

    /**
     * Update existing reservation request in the database.
     *
     * @param reservationRequest
     */
    public void update(ReservationRequest reservationRequest)
    {
        validateReservationRequest(reservationRequest);

        Transaction transaction = beginTransaction();

        super.update(reservationRequest);

        transaction.commit();
    }

    /**
     * Delete existing reservation request in the database
     *
     * @param reservationRequest
     */
    public void delete(ReservationRequest reservationRequest)
    {
        validateReservationRequest(reservationRequest);

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
                        "SELECT request FROM ReservationRequest request LEFT JOIN request.state.records record "
                        + "WHERE record.from <= :from AND record.to >= :to)",
                        ReservationRequest.class).setParameter("from", interval.getStart())
                .setParameter("to", interval.getEnd())
                .getResultList();
        return reservationRequestList;
    }

    /**
     * @param identifier
     * @return {@link cz.cesnet.shongo.controller.request.ReservationRequest} with given identifier or null if the request not exists
     */
    public ReservationRequest get(Identifier identifier)
    {
        try {
            ReservationRequest reservationRequest = entityManager.createQuery(
                    "SELECT request FROM ReservationRequest request WHERE request.identifierAsString = :identifier",
                    ReservationRequest.class).setParameter("identifier", identifier.toString())
                    .getSingleResult();
            return reservationRequest;
        }
        catch (NoResultException exception) {
            return null;
        }
    }

    /**
     * Check domain in all existing reservation requests identifiers
     *
     * @param domain
     */
    public void checkDomain(String domain)
    {
        List<ReservationRequest> reservationRequestList = entityManager
                .createQuery("SELECT request FROM ReservationRequest request", ReservationRequest.class)
                .getResultList();
        for (ReservationRequest reservationRequest : reservationRequestList) {
            if (reservationRequest.getIdentifier().getDomain().equals(domain) == false) {
                throw new IllegalStateException("Reservation request has wrong domain in identifier '" +
                        reservationRequest.getIdentifier().getDomain() + "' (should be '" + domain + "')!");
            }
        }
    }

    /**
     * Validate state of reservation request
     *
     * @param reservationRequest request to be validated
     */
    private void validateReservationRequest(ReservationRequest reservationRequest)
    {
        if (reservationRequest.getIdentifier() == null) {
            throw new IllegalArgumentException("Reservation request must have the identifier filled!");
        }
    }
}
