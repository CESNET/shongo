package cz.cesnet.shongo.controller.reservation;

import cz.cesnet.shongo.common.Identifier;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A reservation database for domain controller.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationDatabase
{
    /**
     * Entity manager that is used for loading/saving reservation requests.
     */
    private EntityManager entityManager;

    /**
     * List of all reservation requests in the database by theirs id.
     */
    private Map<Identifier, ReservationRequest> reservationRequestMap = new HashMap<Identifier, ReservationRequest>();

    /**
     * Constructor of reservation database.
     *
     * @param entityManager Sets the {@link #entityManager}
     */
    public ReservationDatabase(EntityManager entityManager)
    {
        this.entityManager = entityManager;

        // Load all reservation requests from the db
        List<ReservationRequest> reservationRequestList = entityManager
                .createQuery("SELECT request FROM ReservationRequest request", ReservationRequest.class)
                .getResultList();
        for (ReservationRequest reservationRequest : reservationRequestList) {
            addReservationRequest(reservationRequest);
        }
    }

    /**
     * Add new reservation request to the database.
     *
     * @param reservationRequest
     */
    public void addReservationRequest(ReservationRequest reservationRequest)
    {
        if (reservationRequest.getIdentifier() == null) {
            throw new IllegalArgumentException("Reservation request must have the identifier filled!");
        }
        if (reservationRequestMap.containsKey(reservationRequest.getIdentifier())) {
            throw new IllegalArgumentException(
                    "Reservation request (" + reservationRequest.getIdentifier() + ") is already in the database!");
        }

        // Save reservation request to database
        entityManager.getTransaction().begin();
        entityManager.persist(reservationRequest);
        entityManager.getTransaction().commit();

        // Add reservation request to list of all requests
        reservationRequestMap.put(reservationRequest.getIdentifier(), reservationRequest);
    }

    /**
     * Update reservation request in the database.
     *
     * @param reservationRequest
     */
    public void updateReservationRequest(ReservationRequest reservationRequest)
    {
        if (reservationRequestMap.containsKey(reservationRequest.getIdentifier()) == false) {
            throw new IllegalArgumentException(
                    "Reservation request (" + reservationRequest.getIdentifier() + ") is not in the database!");
        }

        throw new RuntimeException("TODO: Implement ReservationDatabase.updateReservationRequest");
    }

    /**
     * Delete reservation request in the database
     *
     * @param reservationRequest
     */
    public void removeReservationRequest(ReservationRequest reservationRequest)
    {
        if (reservationRequestMap.containsKey(reservationRequest.getIdentifier()) == false) {
            throw new IllegalArgumentException(
                    "Reservation request (" + reservationRequest.getIdentifier() + ") is not in the database!");
        }

        // Delete reservation request from database
        entityManager.getTransaction().begin();
        entityManager.remove(reservationRequest);
        entityManager.getTransaction().commit();

        // Delete reservation request from the list of all requests
        reservationRequestMap.remove(reservationRequest.getIdentifier());
    }

    /**
     * @return list of all reservation requests in the database.
     */
    public List<ReservationRequest> listReservationRequests()
    {
        return new ArrayList<ReservationRequest>(reservationRequestMap.values());
    }
}
