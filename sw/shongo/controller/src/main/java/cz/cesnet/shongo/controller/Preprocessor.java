package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.TransactionHelper;
import cz.cesnet.shongo.controller.api.ControllerFault;
import cz.cesnet.shongo.controller.request.*;
import cz.cesnet.shongo.fault.FaultException;
import cz.cesnet.shongo.fault.TodoImplementException;
import cz.cesnet.shongo.util.TemporalHelper;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import java.util.*;

/**
 * Represents a component for a domain controller that is started before scheduler.
 * The component process "not-preprocessed" reservation requests and enumerate them
 * to compartment requests that are scheduled by a scheduler.
 * <p/>
 * Without preprocessor, the scheduler doesn't have any input compartment requests.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Preprocessor extends Component
{
    private static Logger logger = LoggerFactory.getLogger(Preprocessor.class);

    /**
     * Run preprocessor for a given interval.
     *
     * @param interval
     */
    public void run(Interval interval, EntityManager entityManager) throws FaultException
    {
        logger.info("Running preprocessor for interval '{}'...", TemporalHelper.formatInterval(interval));

        TransactionHelper.Transaction transaction = TransactionHelper.beginTransaction(entityManager);

        try {
            // List all not preprocessed reservation request sets
            ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
            List<ReservationRequestSet> reservationRequestSets =
                    reservationRequestManager.listNotPreprocessedReservationRequestSets(interval);

            // Process all reservation request sets
            for (ReservationRequestSet reservationRequestSet : reservationRequestSets) {
                processReservationRequestSet(reservationRequestSet, interval, entityManager);
            }

            transaction.commit();
        }
        catch (Exception exception) {
            transaction.rollback();
            throw new FaultException(exception, ControllerFault.PREPROCESSOR_FAILED);
        }
    }

    /**
     * Run preprocessor only for single {@link ReservationRequestSet} with given identifier for a given interval.
     *
     * @param reservationRequestSetId
     * @param interval
     * @param entityManager
     */
    public void run(long reservationRequestSetId, Interval interval, EntityManager entityManager) throws FaultException
    {
        logger.info("Running preprocessor for a single reservation request set '{}' for interval '{}'...",
                reservationRequestSetId, TemporalHelper.formatInterval(interval));

        TransactionHelper.Transaction transaction = TransactionHelper.beginTransaction(entityManager);

        try {
            // Get reservation request set by identifier
            ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
            ReservationRequestSet reservationRequestSet =
                    reservationRequestManager.getReservationRequestSet(reservationRequestSetId);

            if (reservationRequestSet == null) {
                throw new IllegalArgumentException(String.format("Reservation request set '%s' doesn't exist!",
                        reservationRequestSetId));
            }
            ReservationRequestSetStateManager reservationRequestStateManager =
                    new ReservationRequestSetStateManager(entityManager, reservationRequestSet);
            if (reservationRequestStateManager.getState(interval) != ReservationRequestSet.State.NOT_PREPROCESSED) {
                throw new IllegalStateException(String.format(
                        "Reservation request set '%s' is already preprocessed in %s!",
                        reservationRequestSetId, interval));
            }
            processReservationRequestSet(reservationRequestSet, interval, entityManager);

            transaction.commit();
        }
        catch (Exception exception) {
            transaction.rollback();
            throw new FaultException(exception, ControllerFault.PREPROCESSOR_FAILED);
        }
    }

    /**
     * Synchronize (create/modify/delete) compartment requests from a single persisted reservation request.
     */
    private void processReservationRequestSet(ReservationRequestSet reservationRequestSet, Interval interval,
            EntityManager entityManager)
    {
        reservationRequestSet.checkPersisted();

        logger.info("Pre-processing reservation request '{}'...", reservationRequestSet.getId());

        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);

        // Get list of date/time slots
        List<Interval> slots = reservationRequestSet.enumerateRequestedSlots(interval);

        // List all reservation requests for the set
        List<ReservationRequest> reservationRequests =
                reservationRequestManager.listReservationRequestsBySet(reservationRequestSet, interval);

        // Build set of existing specifications for the set
        Set<Long> specifications = new HashSet<Long>();

        // Reservation requests are synchronized per specification from the set
        for (Specification specification : reservationRequestSet.getRequestedSpecifications()) {
            // List existing reservation requests for the set in the interval
            List<ReservationRequest> reservationRequestsForSpecification =
                    reservationRequestManager.listReservationRequestsBySpecification(specification, interval);

            // Create map of reservation requests with date/time slot as key
            // and remove reservation request from list of all reservation request
            Map<Interval, ReservationRequest> map = new HashMap<Interval, ReservationRequest>();
            for (ReservationRequest reservationRequest : reservationRequestsForSpecification) {
                map.put(reservationRequest.getRequestedSlot(), reservationRequest);
                reservationRequests.remove(reservationRequest);
            }

            // For each requested slot we must create or modify reservation request.
            // If we find date/time slot in prepared map we modify the corresponding request
            // and remove it from map, otherwise we create a new reservation request.
            for (Interval slot : slots) {
                // Modify existing reservation request
                if (map.containsKey(slot)) {
                    ReservationRequest reservationRequest = map.get(slot);
                    if (true) {
                        throw new TodoImplementException("Update reservation request");
                    }
                    reservationRequest.synchronizeFrom(reservationRequestSet);
                    map.remove(slot);
                }
                // Create new reservation request
                else {
                    ReservationRequest reservationRequest = createReservationRequest(specification, slot,
                            reservationRequestManager);
                    reservationRequest.synchronizeFrom(reservationRequestSet);
                }
            }

            // All compartment requests that remains in map must be deleted
            for (ReservationRequest reservationRequest : map.values()) {
                reservationRequestManager.delete(reservationRequest);
            }

            specifications.add(specification.getId());
        }

        // All reservation requests that remains in list of all must be deleted
        for (ReservationRequest reservationRequest : reservationRequests) {
            reservationRequestManager.delete(reservationRequest);

            // If referenced specification isn't in reservation request any more
            Specification specification = reservationRequest.getRequestedSpecification();
            if (!specifications.contains(specification)) {
                // Remove the specification too
                entityManager.remove(specification);
            }
        }

        // When the reservation request hasn't got any future requested slot, the preprocessed state
        // is until "infinite".
        if (!reservationRequestSet.hasRequestedSlotAfter(interval.getEnd())) {
            interval = new Interval(interval.getStart(), ReservationRequestSetStateManager.MAXIMUM_INTERVAL_END);
        }

        // Set state preprocessed state for the interval to reservation request
        ReservationRequestSetStateManager.setState(entityManager, reservationRequestSet,
                ReservationRequestSet.State.PREPROCESSED, interval);
    }

    /**
     * Create a new {@link ReservationRequest} for a reservation request.
     *
     * @param specification   compartment which will be requested in the compartment request
     * @param requestedSlot date/time slot for which the compartment request will be created
     * @return created compartment request
     */
    public static ReservationRequest createReservationRequest(Specification specification, Interval requestedSlot,
            ReservationRequestManager reservationRequestManager)
    {
        // Create compartment request
        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setRequestedSpecification(specification);
        reservationRequest.setRequestedSlot(requestedSlot);

        // TOdO: set params

        // Set of all requested persons
        /*Set<Person> personSet = new HashSet<Person>();

        // Add requested resources and requested persons by these resources
        for (ResourceSpecification resourceSpecification : compartment.getRequestedResources()) {
            addRequestedResource(compartmentRequest, resourceSpecification, personSet, null);
        }

        // Add directly requested persons
        for (Person person : compartment.getRequestedPersons()) {
            addRequestedPerson(compartmentRequest, person, null, personSet, null);
        }*/

        // Set proper state
        reservationRequest.updateStateBySpecifications();

        reservationRequestManager.create(reservationRequest);

        return reservationRequest;
    }

    /**
     * Modify compartment request according to compartment.
     *
     * @param reservationRequest
     * @param specification
     * @return true if some change(s) were made in the compartment request,
     *         false otherwise
     */
    public static boolean updateReservationRequest(ReservationRequest reservationRequest, Specification specification,
            ReservationRequestManager reservationRequestManager)
    {
        // Tracks whether the compartment request was modified
        boolean modified = false;

        // Build set of requested persons that is used to not allow same persons to be requested for a single
        // compartment request
        /*Set<Person> personSet = new HashSet<Person>();
        for (PersonRequest personRequest : reservationRequest.getRequestedPersons()) {
            Person person = personRequest.getPerson();
            if (personSet.contains(person)) {
                throw new IllegalArgumentException("Compartment request contains two same requested persons ('"
                        + person.toString() + "')!");
            }
            personSet.add(person);
        }

        // Build map of person requests that is used for merging new/deleted persons from compartment to existing
        // person requests in compartment request and for removing person requests that are no more requested
        Map<Long, PersonRequest> personRequestMap = new HashMap<Long, PersonRequest>();
        for (PersonRequest personRequest : reservationRequest.getRequestedPersons()) {
            Person person = personRequest.getPerson();
            if (personRequestMap.containsKey(person.getId())) {
                throw new IllegalArgumentException("Compartment request has two same person requests ('"
                        + personRequestMap.get(person).toString() + "' and '" + personRequest + "')!");
            }
            personRequestMap.put(person.getId(), personRequest);
        }

        // Build map of existing requested resources that is used for detecting whether resource specification exists
        // in the compartment request and to remove resource specifications that are no more requested
        Map<Long, ResourceSpecification> resourceSpecificationMap = new HashMap<Long, ResourceSpecification>();
        for (ResourceSpecification resourceSpecification : reservationRequest.getRequestedResources()) {
            if (resourceSpecificationMap.containsKey(resourceSpecification.getId())) {
                throw new IllegalArgumentException("Compartment request has two same requested resources ('"
                        + resourceSpecification.toString() + "')!");
            }
            resourceSpecificationMap.put(resourceSpecification.getId(), resourceSpecification);
        }

        // Check all requested resources from the compartment
        for (ResourceSpecification resourceSpecification : compartment.getRequestedResources()) {
            if (resourceSpecificationMap.containsKey(resourceSpecification.getId())) {
                // Resource specification exists so check it's requested persons
                for (Person person : resourceSpecification.getRequestedPersons()) {
                    if (personSet.contains(person)) {
                        // Requested person exists so check if it is assigned to proper resource
                        PersonRequest personRequest = null;
                        // Find person request in linear time
                        // TODO: Find person request by building map before the main for loop to improve performance
                        for (PersonRequest possiblePersonRequest : reservationRequest.getRequestedPersons()) {
                            if (possiblePersonRequest.getPerson().equals(person)) {
                                personRequest = possiblePersonRequest;
                                break;
                            }
                        }
                        if (personRequest == null) {
                            throw new IllegalStateException("Person request was not found but should be "
                                    + "because person was found in person set!");
                        }
                        // TODO: Update person (it could be modified in compartment but stills it equaled)
                        if (personRequest.getResourceSpecification() == resourceSpecification) {
                            // Existing person request references the right resource specification, thus do nothing
                        }
                        else {
                            // Existing person request references wrong resource specification
                            if (personRequest.getState() == PersonRequest.State.NOT_ASKED) {
                                personRequest.setResourceSpecification(resourceSpecification);
                                modified = true;
                                // TODO: If old resource spec. doesn't has any requested persons consider to remove it
                            }
                            else if (personRequest.getState() == PersonRequest.State.ASKED) {
                                modified = true;
                                throw new IllegalStateException("Cannot change requested person resource "
                                        + "in compartment request because the person has already been asked "
                                        + "whether he will accepts the invitation.");
                            }
                            else if (personRequest.getState() == PersonRequest.State.ACCEPTED) {
                                // Do nothing he selected other resource
                            }
                            else if (personRequest.getState() == PersonRequest.State.REJECTED) {
                                // Do nothing he don't want to collaborate
                            }
                            else {
                                throw new IllegalStateException(
                                        "Unknown state " + personRequest.getState().toString() + "!");
                            }
                        }
                    }
                    else {
                        // Requested person doesn't exist so add new requested person
                        addRequestedPerson(reservationRequest, person, resourceSpecification, personSet,
                                personRequestMap);
                        modified = true;
                    }
                }

                // Resource specification is modified
                modified = true;
            }
            else {
                // Resource specification doesn't exists so add it
                addRequestedResource(reservationRequest, resourceSpecification, personSet, personRequestMap);
                modified = true;
            }

            // Remove resource specification from map
            resourceSpecificationMap.remove(resourceSpecification.getId());
        }

        // Check all directly requested persons from the compartment
        for (Person person : compartment.getRequestedPersons()) {
            if (!personSet.contains(person)) {
                // Requested person doesn't exist so add new requested person
                addRequestedPerson(reservationRequest, person, null, personSet, personRequestMap);
                modified = true;
                personRequestMap.remove(person.getId());
            }

            // Remove the requested person from the map for the person not be deleted
            personRequestMap.remove(person.getId());
        }

        // Remove all person requests from map that are requested from compartment resources to not be deleted
        for (ResourceSpecification resourceSpecification : compartment.getRequestedResources()) {
            for (Person person : resourceSpecification.getRequestedPersons()) {
                personRequestMap.remove(person.getId());
            }
        }

        // All person requests that remains in map should be deleted because they aren't specified
        // in the compartment any more
        for (PersonRequest personRequest : personRequestMap.values()) {
            removeRequestedPerson(reservationRequest, personRequest);
            modified = true;
        }

        // Remove some resources from the map to not be deleted
        for (PersonRequest personRequest : reservationRequest.getRequestedPersons()) {
            // Requested person could selected resources that wasn't
            // specified in compartment so we must remove them from map to not be deleted
            ResourceSpecification resourceSpecification = personRequest.getResourceSpecification();
            if (resourceSpecification != null && resourceSpecificationMap.containsKey(resourceSpecification.getId())) {
                resourceSpecificationMap.remove(resourceSpecification.getId());
            }
        }

        // All resources that remains in map should be deleted because they aren't specified
        // in the compartment any more or they aren't specified by any not deleted person requests
        for (ResourceSpecification resourceSpecification : resourceSpecificationMap.values()) {
            reservationRequest.removeRequestedResource(resourceSpecification);
            modified = true;
        }

        // If the compartment request was modified, we must remove the allocated state from it
        // and set the new state based on requested persons
        if (modified) {
            reservationRequest.clearState();
            reservationRequest.updateStateByRequestedPersons();
        }

        reservationRequestManager.update(reservationRequest);*/

        return modified;
    }

    /**
     * Run preprocessor on given {@code entityManager} and interval.
     *
     * @param entityManager
     * @param interval
     */
    public static void createAndRun(Interval interval, EntityManager entityManager) throws FaultException
    {
        Preprocessor preprocessor = new Preprocessor();
        preprocessor.init();
        preprocessor.run(interval, entityManager);
        preprocessor.destroy();
    }

    /**
     * Run preprocessor on given {@code entityManager}, for a single reservation request and given interval.
     *
     * @param entityManager
     * @param reservationRequestId
     * @param interval
     */
    public static void createAndRun(long reservationRequestId, Interval interval, EntityManager entityManager)
            throws FaultException
    {
        Preprocessor preprocessor = new Preprocessor();
        preprocessor.init();
        preprocessor.run(reservationRequestId, interval, entityManager);
        preprocessor.destroy();
    }
}
