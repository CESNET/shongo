package cz.cesnet.shongo.controller.reservation;

import cz.cesnet.shongo.common.AbsoluteDateTimeSlot;
import cz.cesnet.shongo.common.Identifier;
import cz.cesnet.shongo.common.Person;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static Logger logger = LoggerFactory.getLogger(ReservationDatabase.class);

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
     * @param entityManager Sets the {@link #entityManager}
     */
    public ReservationDatabase(EntityManager entityManager)
    {
        this.entityManager = entityManager;

        logger.debug("Loading reservation database...");

        // Load all reservation requests from the db
        List<ReservationRequest> reservationRequestList = entityManager
                .createQuery("SELECT request FROM ReservationRequest request", ReservationRequest.class)
                .getResultList();
        for (ReservationRequest reservationRequest : reservationRequestList) {
            addReservationRequest(reservationRequest);
        }
    }

    /**
     * Destroy reservation database.
     */
    public void destroy()
    {
        logger.debug("Closing reservation database...");
        reservationRequestMap.clear();
    }

    /**
     * Add new reservation request to the database.
     * @param reservationRequest
     */
    public void addReservationRequest(ReservationRequest reservationRequest)
    {
        validateReservationRequest(reservationRequest);

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

        // Create compartment requests from reservation request
        synchronizeCompartmentRequests(reservationRequest);
    }

    /**
     * Update reservation request in the database.
     * @param reservationRequest
     */
    public void updateReservationRequest(ReservationRequest reservationRequest)
    {
        if (reservationRequestMap.containsKey(reservationRequest.getIdentifier()) == false) {
            throw new IllegalArgumentException(
                    "Reservation request (" + reservationRequest.getIdentifier() + ") is not in the database!");
        }
        reservationRequest.checkPersisted();

        throw new RuntimeException("TODO: Implement ReservationDatabase.updateReservationRequest");
    }

    /**
     * Delete reservation request in the database
     * @param reservationRequest
     */
    public void removeReservationRequest(ReservationRequest reservationRequest)
    {
        if (reservationRequestMap.containsKey(reservationRequest.getIdentifier()) == false) {
            throw new IllegalArgumentException(
                    "Reservation request (" + reservationRequest.getIdentifier() + ") is not in the database!");
        }
        reservationRequest.checkPersisted();

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

    /**
     * Validate state of reservation request
     * @param reservationRequest request to be validated
     */
    private void validateReservationRequest(ReservationRequest reservationRequest)
    {
        if (reservationRequest.getIdentifier() == null) {
            throw new IllegalArgumentException("Reservation request must have the identifier filled!");
        }
    }

    /**
     * Synchronize persisted reservation request with corresponding compartment requests.
     */
    private void synchronizeCompartmentRequests(ReservationRequest reservationRequest)
    {
        reservationRequest.checkPersisted();

        // TODO: Do it for specific interval
        // Get list of date/time slots
        List<AbsoluteDateTimeSlot> slots = reservationRequest.enumerateRequestedSlots(null, null);

        // Start transaction
        entityManager.getTransaction().begin();

        // Compartment requests are synchronized per compartment from reservation request
        for ( Compartment compartment : reservationRequest.getRequestedCompartments()) {
            // Get existing compartment requests for compartment
            List<CompartmentRequest> compartmentRequestList = entityManager.createQuery(
                    "SELECT request FROM CompartmentRequest request WHERE request.compartment = :compartment",
                    CompartmentRequest.class).setParameter("compartment", compartment).getResultList();

            // Create map of compartment requests with date/time slot as key
            Map<AbsoluteDateTimeSlot, CompartmentRequest> map = new HashMap<AbsoluteDateTimeSlot, CompartmentRequest>();
            for ( CompartmentRequest compartmentRequest : compartmentRequestList) {
                map.put(compartmentRequest.getRequestedSlot(), compartmentRequest);
            }

            // For each requested slot we must create or modify compartment request.
            // If we find date/time slot in prepared map we modify the corresponding request
            // and remove it from map, otherwise we create a new compartment request.
            for (AbsoluteDateTimeSlot slot : slots) {
                // Modify existing compartment request
                if ( map.containsKey(slot) ) {
                    CompartmentRequest compartmentRequest = map.get(slot);
                    modifyCompartmentRequest(compartmentRequest, compartment);
                }
                // Create new compartment request
                else {
                    CompartmentRequest compartmentRequest = createCompartmentRequest(compartment, slot);
                    entityManager.persist(compartmentRequest);
                }
            }

            // All compartment requests that remains in map must be deletedne
            for ( CompartmentRequest compartmentRequest : map.values()) {
                entityManager.remove(compartmentRequest);
            }
        }

        // Commit transaction
        entityManager.getTransaction().commit();
    }

    /**
     * Create a new compartment request for a reservation request.
     * @param compartment        compartment which will be requested in the compartment request
     * @param requestedSlot      date/time slot for which the compartment request will be created
     * @return created compartment request
     */
    private CompartmentRequest createCompartmentRequest(Compartment compartment, AbsoluteDateTimeSlot requestedSlot)
    {
        // Create compartment request
        CompartmentRequest compartmentRequest = new CompartmentRequest();
        compartmentRequest.setCompartment(compartment);
        compartmentRequest.setRequestedSlot(requestedSlot);

        // Add requested persons by requested resources
        for ( ResourceSpecification resourceSpecification : compartment.getRequestedResources()) {
            for ( Person person : resourceSpecification.getRequestedPersons()) {
                PersonRequest personRequest = createPersonRequest(person, resourceSpecification);
                compartmentRequest.addRequestedPerson(personRequest);
            }
        }

        // Add directly requested persons
        for ( Person person : compartment.getRequestedPersons()) {
            PersonRequest personRequest = createPersonRequest(person, null);
            compartmentRequest.addRequestedPerson(personRequest);
        }

        // Set proper compartment request state
        compartmentRequest.updateState();

        return compartmentRequest;
    }

    /**
     * Modify compartment request.
     * @param compartmentRequest
     * @param compartment
     */
    private void modifyCompartmentRequest(CompartmentRequest compartmentRequest, Compartment compartment)
    {
        throw new RuntimeException("TODO: Implement compartment request check");
    }

    /**
     * Create a new person request for compartment request.
     * @param person
     * @param resourceSpecification
     * @return
     */
    private PersonRequest createPersonRequest(Person person, ResourceSpecification resourceSpecification)
    {
        PersonRequest personRequest = new PersonRequest();
        personRequest.setPerson(person);
        personRequest.setState(PersonRequest.State.NOT_ASKED);
        personRequest.setResourceSpecification(resourceSpecification);
        return personRequest;
    }
}
