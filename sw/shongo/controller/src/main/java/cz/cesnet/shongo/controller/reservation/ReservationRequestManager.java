package cz.cesnet.shongo.controller.reservation;

import cz.cesnet.shongo.common.AbsoluteDateTimeSlot;
import cz.cesnet.shongo.common.AbstractManager;
import cz.cesnet.shongo.common.Identifier;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A reservation database for domain controller.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationRequestManager extends AbstractManager
{
    /**
     * @see CompartmentManager
     */
    private CompartmentManager compartmentManager;

    /**
     * @see CompartmentRequestManager
     */
    private CompartmentRequestManager compartmentRequestManager;

    /**
     * Constructor.
     * @param entityManager
     * @param compartmentManager
     * @param compartmentRequestManager
     */
    private ReservationRequestManager(EntityManager entityManager, CompartmentManager compartmentManager,
            CompartmentRequestManager compartmentRequestManager)
    {
        super(entityManager);
        this.compartmentManager = compartmentManager;
        this.compartmentRequestManager = compartmentRequestManager;
    }

    /**
     * @param entityManager
     * @return new instance of {@link ReservationRequestManager}
     */
    public static ReservationRequestManager createInstance(EntityManager entityManager)
    {
        CompartmentRequestManager compartmentRequestManager = CompartmentRequestManager.createInstance(entityManager);
        CompartmentManager compartmentManager = CompartmentManager.createInstance(entityManager,
                compartmentRequestManager);
        return new ReservationRequestManager(entityManager, compartmentManager, compartmentRequestManager);
    }

    /**
     * Create new reservation in the database.
     *
     * @param reservationRequest
     */
    public void create(ReservationRequest reservationRequest)
    {
        validateReservationRequest(reservationRequest);

        super.create(reservationRequest);

        synchronizeCompartmentRequests(reservationRequest);
    }

    /**
     * Update existing reservation request in the database.
     *
     * @param reservationRequest
     */
    public void update(ReservationRequest reservationRequest)
    {
        validateReservationRequest(reservationRequest);

        super.update(reservationRequest);

        synchronizeCompartmentRequests(reservationRequest);
    }

    /**
     * Delete existing reservation request in the database
     *
     * @param reservationRequest
     */
    public void delete(ReservationRequest reservationRequest)
    {
        validateReservationRequest(reservationRequest);

        super.delete(reservationRequest);
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
     * @return {@link cz.cesnet.shongo.controller.reservation.ReservationRequest} with given identifier or null if the request not exists
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

    /**
     * Synchronize (create/modify/delete) compartment requests from a single persisted reservation request.
     */
    private void synchronizeCompartmentRequests(ReservationRequest reservationRequest)
    {
        entityManager.flush();

        reservationRequest.checkPersisted();

        // TODO: Do it for a specific interval
        // Get list of date/time slots
        List<AbsoluteDateTimeSlot> slots = reservationRequest.enumerateRequestedSlots(null, null);

        // Start transaction if is no active
        EntityTransaction transaction = null;
        if (entityManager.getTransaction().isActive() == false) {
            transaction = entityManager.getTransaction();
            transaction.begin();
        }

        // Compartment requests are synchronized per compartment from reservation request
        for (Compartment compartment : reservationRequest.getRequestedCompartments()) {
            // List existing compartment requests for reservation request
            List<CompartmentRequest> compartmentRequestList = entityManager.createQuery(
                    "SELECT request FROM CompartmentRequest request WHERE request.compartment = :compartment",
                    CompartmentRequest.class).setParameter("compartment", compartment).getResultList();

            // Create map of compartment requests with date/time slot as key
            Map<AbsoluteDateTimeSlot, CompartmentRequest> map = new HashMap<AbsoluteDateTimeSlot, CompartmentRequest>();
            for (CompartmentRequest compartmentRequest : compartmentRequestList) {
                map.put(compartmentRequest.getRequestedSlot(), compartmentRequest);
            }

            // For each requested slot we must create or modify compartment request.
            // If we find date/time slot in prepared map we modify the corresponding request
            // and remove it from map, otherwise we create a new compartment request.
            for (AbsoluteDateTimeSlot slot : slots) {
                // Modify existing compartment request
                if (map.containsKey(slot)) {
                    CompartmentRequest compartmentRequest = map.get(slot);
                    compartmentRequestManager.update(compartmentRequest, compartment);
                    map.remove(slot);
                }
                // Create new compartment request
                else {
                    compartmentRequestManager.create(compartment, slot);
                }
            }

            // All compartment requests that remains in map must be deleted
            for (CompartmentRequest compartmentRequest : map.values()) {
                compartmentRequestManager.delete(compartmentRequest);
            }
        }

        compartmentManager.deleteAllWithoutReservationRequest();

        // Commit transaction if was started
        if (transaction != null) {
            transaction.commit();
        }
    }

    /**
     * Check domain in all existing reservation requests identifiers
     * @param domain
     */
    public void checkDomain(String domain)
    {
        List<ReservationRequest> reservationRequestList = entityManager
                .createQuery("SELECT request FROM ReservationRequest request", ReservationRequest.class)
                .getResultList();
        for ( ReservationRequest reservationRequest : reservationRequestList) {
            if(reservationRequest.getIdentifier().getDomain().equals(domain) == false) {
                throw new IllegalStateException("Reservation request has wrong domain in identifier '" +
                        reservationRequest.getIdentifier().getDomain() + "' (should be '" + domain + "')!");
            }
        }
    }
}
