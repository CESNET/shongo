package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.common.AbstractManager;
import cz.cesnet.shongo.common.Identifier;
import cz.cesnet.shongo.controller.Scheduler;

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
     * Scheduler which methods are invoked for reservation request events.
     */
    private Scheduler scheduler;

    /**
     * Constructor.
     *
     * @param entityManager
     * @param scheduler
     */
    public ReservationRequestManager(EntityManager entityManager, Scheduler scheduler)
    {
        super(entityManager);
        this.scheduler = scheduler;
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

        scheduler.onNewReservationRequest(reservationRequest);

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

        scheduler.onUpdateReservationRequest(reservationRequest);

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

        scheduler.onDeleteReservationRequest(reservationRequest);

        super.delete(reservationRequest);

        transaction.commit();
    }

    /**
     * @return list of all reservation requests in the database.
     */
    public List<ReservationRequest> list()
    {
        List<ReservationRequest> reservationRequestList = entityManager
                .createQuery("SELECT request FROM ReservationRequest request", ReservationRequest.class)
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
