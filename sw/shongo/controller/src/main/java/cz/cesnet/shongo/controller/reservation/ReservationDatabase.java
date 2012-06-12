package cz.cesnet.shongo.controller.reservation;

import cz.cesnet.shongo.common.AbsoluteDateTimeSlot;
import cz.cesnet.shongo.common.Identifier;
import cz.cesnet.shongo.common.Person;
import cz.cesnet.shongo.controller.Domain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class ReservationDatabase
{
    private static Logger logger = LoggerFactory.getLogger(ReservationDatabase.class);

    /**
     * Domain for which the reservation database is used.
     */
    private Domain domain;

    /**
     * Entity manager that is used for accessing database.
     */
    private EntityManager entityManager;

    /**
     * Constructor of reservation database.
     */
    public ReservationDatabase()
    {
    }

    /**
     * Constructor of reservation database.
     *
     * @param domain        sets the {@link #domain}
     * @param entityManager Sets the {@link #entityManager}
     */
    public ReservationDatabase(Domain domain, EntityManager entityManager)
    {
        setDomain(domain);
        setEntityManager(entityManager);
        init();
    }

    /**
     * @param domain sets the {@link #domain}
     */
    public void setDomain(Domain domain)
    {
        this.domain = domain;
    }

    /**
     * @param entityManager sets the {@link #entityManager}
     */
    public void setEntityManager(EntityManager entityManager)
    {
        this.entityManager = entityManager;
    }

    /**
     * Initialize reservation database.
     */
    public void init()
    {
        if (domain == null) {
            throw new IllegalStateException("Reservation database doesn't have the domain set!");
        }
        if (entityManager == null) {
            throw new IllegalStateException("Reservation database doesn't have the entity manager set!");
        }

        logger.debug("Checking reservation database...");

        List<ReservationRequest> reservationRequestList = entityManager
                .createQuery("SELECT request FROM ReservationRequest request", ReservationRequest.class)
                .getResultList();
        for ( ReservationRequest reservationRequest : reservationRequestList) {
            if(reservationRequest.getIdentifier().getDomain().equals(domain.getCodeName()) == false) {
                throw new IllegalStateException("Reservation request has wrong domain in identifier '" +
                        reservationRequest.getIdentifier().getDomain() + "' (should be '" + domain.getCodeName() + "')!");
            }
        }
    }

    /**
     * Destroy reservation database.
     */
    public void destroy()
    {
        logger.debug("Closing reservation database...");
    }

    /**
     * Add new reservation request to the database.
     *
     * @param reservationRequest
     */
    public void addReservationRequest(ReservationRequest reservationRequest)
    {
        // Create new identifier for the reservation request
        if (reservationRequest.getIdentifier() != null) {
            throw new IllegalArgumentException("New reservation request should not have identifier filled ("
                    + reservationRequest.getIdentifier() + ")!");
        }
        reservationRequest.createNewIdentifier(domain.getCodeName());

        // Validate request
        validateReservationRequest(reservationRequest);

        // Check the non-existence
        if (getReservationRequest(reservationRequest.getIdentifier()) != null) {
            throw new IllegalArgumentException(
                    "Reservation request (" + reservationRequest.getIdentifier() + ") is already in the database!");
        }

        // Save reservation request to database
        entityManager.getTransaction().begin();
        entityManager.persist(reservationRequest);
        entityManager.getTransaction().commit();

        // Create compartment requests from reservation request
        synchronizeCompartmentRequests(reservationRequest);
    }

    /**
     * Update reservation request in the database.
     *
     * @param reservationRequest
     */
    public void updateReservationRequest(ReservationRequest reservationRequest)
    {
        reservationRequest.checkPersisted();

        entityManager.getTransaction().begin();

        // Save changes
        entityManager.persist(reservationRequest);

        // Update compartment requests from reservation request
        synchronizeCompartmentRequests(reservationRequest);

        entityManager.getTransaction().commit();
    }

    /**
     * Delete reservation request in the database
     *
     * @param reservationRequest
     */
    public void removeReservationRequest(ReservationRequest reservationRequest)
    {
        reservationRequest.checkPersisted();

        // Delete reservation request from database
        entityManager.getTransaction().begin();
        entityManager.remove(reservationRequest);
        entityManager.getTransaction().commit();
    }

    /**
     * @return list of all reservation requests in the database.
     */
    public List<ReservationRequest> listReservationRequests()
    {
        List<ReservationRequest> reservationRequestList = entityManager
                .createQuery("SELECT request FROM ReservationRequest request", ReservationRequest.class)
                .getResultList();
        return reservationRequestList;
    }

    /**
     * @param identifier
     * @return {@link ReservationRequest} with given identifier or null if the request not exists
     */
    public ReservationRequest getReservationRequest(Identifier identifier)
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
     * @param reservationRequestIdentifier
     * @return list of existing compartment requests for a {@link ReservationRequest} with the given identifier
     */
    public List<CompartmentRequest> listCompartmentRequests(Identifier reservationRequestIdentifier)
    {
        // Get existing compartment requests for compartment
        List<CompartmentRequest> compartmentRequestList = entityManager.createQuery(
                "SELECT request FROM CompartmentRequest request " +
                        ""/*"WHERE request.compartment.reservationRequest.identifierAsString = :identifier"*/,
                CompartmentRequest.class)/*.setParameter("identifier", reservationRequestIdentifier.toString())*/
                .getResultList();
        return compartmentRequestList;
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

        // TODO: Do some further validation
    }

    /**
     * Synchronize (create/modify/delete) compartment requests from a single persisted reservation request.
     */
    private void synchronizeCompartmentRequests(ReservationRequest reservationRequest)
    {
        reservationRequest.checkPersisted();

        // TODO: Do it for a specific interval
        // Get list of date/time slots
        List<AbsoluteDateTimeSlot> slots = reservationRequest.enumerateRequestedSlots(null, null);

        // Start transaction if is no active
        EntityTransaction transaction = null;
        if ( entityManager.getTransaction().isActive() == false ) {
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
                    modifyCompartmentRequest(compartmentRequest, compartment);
                    map.remove(slot);
                }
                // Create new compartment request
                else {
                    CompartmentRequest compartmentRequest = createCompartmentRequest(compartment, slot);
                    entityManager.persist(compartmentRequest);
                }
            }

            // All compartment requests that remains in map must be deleted
            for (CompartmentRequest compartmentRequest : map.values()) {
                removeCompartmentRequest(compartmentRequest);
                entityManager.remove(compartmentRequest);
            }
        }

        // Delete all compartment request that belongs to compartments without reservation request
        List<CompartmentRequest> compartmentRequestList = entityManager.createQuery(
                "SELECT request FROM CompartmentRequest request WHERE request.compartment.reservationRequest IS NULL",
                CompartmentRequest.class).getResultList();
        for ( CompartmentRequest compartmentRequest : compartmentRequestList ) {
            removeCompartmentRequest(compartmentRequest);
            entityManager.remove(compartmentRequest);
        }

        // Delete all compartments without reservation request
        List<Compartment> compartmentList = entityManager.createQuery(
                "SELECT compartment FROM Compartment compartment WHERE compartment.reservationRequest IS NULL",
                Compartment.class).getResultList();
        for ( Compartment compartment : compartmentList) {
            entityManager.remove(compartment);
        }

        // Commit transaction if was started
        if ( transaction != null ) {
            transaction.commit();
        }
    }

    /**
     * Create a new compartment request for a reservation request.
     *
     * @param compartment   compartment which will be requested in the compartment request
     * @param requestedSlot date/time slot for which the compartment request will be created
     * @return created compartment request
     */
    private CompartmentRequest createCompartmentRequest(Compartment compartment, AbsoluteDateTimeSlot requestedSlot)
    {
        // Create compartment request
        CompartmentRequest compartmentRequest = new CompartmentRequest();
        compartmentRequest.setCompartment(compartment);
        compartmentRequest.setRequestedSlot(requestedSlot);

        // Add requested persons by requested resources
        for (ResourceSpecification resourceSpecification : compartment.getRequestedResources()) {
            for (Person person : resourceSpecification.getRequestedPersons()) {
                // TODO: Check whether person isn't already in compartment (may be do the check in reservation request)
                PersonRequest personRequest = createPersonRequest(person, resourceSpecification);
                compartmentRequest.addRequestedPerson(personRequest);
            }
        }

        // Add directly requested persons
        for (Person person : compartment.getRequestedPersons()) {
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
        //throw new RuntimeException("TODO: Implement compartment request check");
    }

    /**
     * Remove compartment request.
     * @param compartmentRequest
     */
    private void removeCompartmentRequest(CompartmentRequest compartmentRequest)
    {
        System.err.println("TODO: Implement remove compartment request\n" + compartmentRequest.toString());
    }

    /**
     * Create a new person request for compartment request.
     *
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
