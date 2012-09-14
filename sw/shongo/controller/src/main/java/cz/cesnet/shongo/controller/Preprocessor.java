package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.TransactionHelper;
import cz.cesnet.shongo.controller.api.ControllerFault;
import cz.cesnet.shongo.controller.request.*;
import cz.cesnet.shongo.fault.FaultException;
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

        // Build map of reservation requests by original specification
        Map<Specification, Set<ReservationRequest>> reservationRequestsByOriginalSpecification =
                new HashMap<Specification, Set<ReservationRequest>>();
        for (ReservationRequest reservationRequest : reservationRequests) {
            Specification originalSpecification = reservationRequestSet.getOriginalSpecifications()
                    .get(reservationRequest.getSpecification());
            if (originalSpecification == null) {
                originalSpecification = reservationRequest.getSpecification();
            }
            Set<ReservationRequest> set = reservationRequestsByOriginalSpecification.get(originalSpecification);
            if (set == null) {
                set = new HashSet<ReservationRequest>();
                reservationRequestsByOriginalSpecification.put(originalSpecification, set);
            }
            set.add(reservationRequest);
        }

        // Build set of existing specifications for the set
        Set<Long> specifications = new HashSet<Long>();

        // Reservation requests are synchronized per specification from the set
        for (Specification specification : reservationRequestSet.getSpecifications()) {
            if (specification == null) {
                throw new IllegalStateException("Specification should not be null!");
            }
            // List existing reservation requests for the set in the interval
            Set<ReservationRequest> reservationRequestsForSpecification =
                    reservationRequestsByOriginalSpecification.get(specification);

            // Create map of reservation requests with date/time slot as key
            // and remove reservation request from list of all reservation request
            Map<Interval, ReservationRequest> map = new HashMap<Interval, ReservationRequest>();
            if (reservationRequestsForSpecification != null) {
                for (ReservationRequest reservationRequest : reservationRequestsForSpecification) {
                    map.put(reservationRequest.getRequestedSlot(), reservationRequest);
                    reservationRequests.remove(reservationRequest);
                }
            }

            // For each requested slot we must create or modify reservation request.
            // If we find date/time slot in prepared map we modify the corresponding request
            // and remove it from map, otherwise we create a new reservation request.
            for (Interval slot : slots) {
                // Modify existing reservation request
                if (map.containsKey(slot)) {
                    ReservationRequest reservationRequest = map.get(slot);
                    updateReservationRequest(reservationRequest, reservationRequestSet, specification);

                    // Remove the slot from the map for the corresponding reservation request to not be deleted
                    map.remove(slot);
                }
                // Create new reservation request
                else {
                    ReservationRequest reservationRequest = new ReservationRequest();
                    reservationRequest.setRequestedSlot(slot);
                    updateReservationRequest(reservationRequest, reservationRequestSet, specification);
                    reservationRequestSet.addReservationRequest(reservationRequest);
                }
            }

            // All reservation requests that remains in map must be deleted
            for (ReservationRequest reservationRequest : map.values()) {
                reservationRequestSet.removeReservationRequest(reservationRequest);
                reservationRequestManager.delete(reservationRequest);
            }

            specifications.add(specification.getId());
        }

        // All reservation requests that remains in list of all must be deleted
        for (ReservationRequest reservationRequest : reservationRequests) {
            reservationRequestSet.removeReservationRequest(reservationRequest);
            reservationRequestManager.delete(reservationRequest);

            // If referenced specification isn't in reservation request any more
            Specification specification = reservationRequest.getSpecification();
            if (!specifications.contains(specification)) {
                // Remove the specification too
                entityManager.remove(specification);
            }
        }

        // Update reservation request
        reservationRequestManager.update(reservationRequestSet);

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
     * Update given {@code specification} according to given {@code specificationFrom}.
     *
     * @param specification
     * @param specificationFrom
     * @param reservationRequestSet
     * @return true if some change(s) were made in the {@link Specification}
     *         false otherwise
     */
    private static boolean updateSpecification(Specification specification, Specification specificationFrom,
            ReservationRequestSet reservationRequestSet)
    {
        boolean modified = false;
        modified |= specification.synchronizeFrom(specificationFrom);
        if (specification instanceof CompositeSpecification) {
            CompositeSpecification compositeSpecification = (CompositeSpecification) specification;
            CompositeSpecification compositeSpecificationFrom = (CompositeSpecification) specificationFrom;

            // Get map of original specifications by cloned specifications
            Map<Specification, Specification> originalSpecifications = reservationRequestSet
                    .getOriginalSpecifications();

            // Build set of new specifications
            Set<Specification> newSpecifications = new HashSet<Specification>();
            for (Specification newSpecification : compositeSpecificationFrom.getSpecifications()) {
                newSpecifications.add(newSpecification);
            }

            // Update or delete child specifications
            Set<Specification> deletedSpecifications = new HashSet<Specification>();
            for (Specification childSpecification : compositeSpecification.getSpecifications()) {
                Specification originalSpecification = originalSpecifications.get(childSpecification);
                if (originalSpecification == null) {
                    originalSpecification = childSpecification;
                }
                if (newSpecifications.contains(originalSpecification)) {
                    modified |= updateSpecification(childSpecification, originalSpecification, reservationRequestSet);
                    newSpecifications.remove(originalSpecification);
                }
                else {
                    deletedSpecifications.add(childSpecification);
                }
            }
            for (Specification deletedSpecification : deletedSpecifications) {
                compositeSpecification.removeSpecification(deletedSpecification);
                modified = true;
            }

            // Add new child specifications
            for (Specification newSpecification : newSpecifications) {
                if (newSpecification instanceof StatefulSpecification) {
                    StatefulSpecification newStatefulSpecification = (StatefulSpecification) newSpecification;
                    compositeSpecification.addSpecification(newStatefulSpecification.clone(originalSpecifications));
                }
                else {
                    compositeSpecification.addSpecification(newSpecification);
                }
                modified = true;
            }
        }
        return modified;
    }

    /**
     * Update given {@code reservationRequest} according to given {@code specification}.
     *
     * @param reservationRequest
     * @param specification
     * @return true if some change(s) were made in the {@link ReservationRequest}
     *         false otherwise
     */
    private static boolean updateReservationRequest(ReservationRequest reservationRequest,
            ReservationRequestSet reservationRequestSet, Specification specification)
    {
        // Tracks whether the compartment request was modified
        boolean modified = false;
        modified |= reservationRequest.synchronizeFrom(reservationRequestSet);

        Specification oldSpecification = reservationRequest.getSpecification();
        if (oldSpecification == null || oldSpecification.getClass() != specification.getClass()) {
            // Setup new specification
            if (specification instanceof StatefulSpecification) {
                StatefulSpecification statefulSpecification = (StatefulSpecification) specification;
                reservationRequest.setSpecification(
                        statefulSpecification.clone(reservationRequestSet.getOriginalSpecifications()));
            }
            else {
                reservationRequest.setSpecification(specification);
            }
            modified = true;
        }
        else {
            // Check specification for modifications
            modified |= updateSpecification(oldSpecification, specification, reservationRequestSet);
        }

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

        */

        return modified;
    }

    /**
     * Add requested resource to compartment request (and also all persons that are requested for the resource).
     *
     * @param compartmentRequest    {@link CompartmentRequest} to which the requested resource should be added
     * @param resourceSpecification {@link ResourceSpecification} of the requested resource
     * @param personSet             set of persons that have already been added to the compartment request
     * @param personRequestMap      map of person request that have already been added to the compartment request
     */
    /*private void addRequestedResource(CompartmentRequest compartmentRequest,
            ResourceSpecification resourceSpecification, Set<Person> personSet,
            Map<Long, PersonRequest> personRequestMap)
    {
        compartmentRequest.addRequestedResource(resourceSpecification);
        for (Person person : resourceSpecification.getRequestedPersons()) {
            addRequestedPerson(compartmentRequest, person, resourceSpecification, personSet, personRequestMap);
        }
    }*/

    /**
     * Add requested person to compartment request.
     *
     * @param compartmentRequest    {@link CompartmentRequest} to which the requested person should be added
     * @param person                requested {@link Person} to add
     * @param resourceSpecification {@link ResourceSpecification} for the resource which the person use for connecting
     *                              to the compartment (can be <code>null</code>).
     * @param personSet             set of persons that have already been added to the compartment request
     * @param personRequestMap      map of person request that have already been added to the compartment request
     */
    /*private void addRequestedPerson(CompartmentRequest compartmentRequest, Person person,
            ResourceSpecification resourceSpecification, Set<Person> personSet,
            Map<Long, PersonRequest> personRequestMap)
    {
        // Check whether person isn't already added to compartment request
        if (personSet.contains(person)) {
            throw new IllegalStateException("Person '" + person.toString()
                    + "' has already been added to compartment request!");
        }

        // Create person request in compartment request
        PersonRequest personRequest = new PersonRequest();
        personRequest.setPerson(person);
        personRequest.setState(PersonRequest.State.NOT_ASKED);
        if (resourceSpecification != null) {
            personRequest.setResourceSpecification(resourceSpecification);
        }
        compartmentRequest.addRequestedPerson(personRequest);

        if (personSet != null) {
            personSet.add(person);
        }
        if (personRequestMap != null) {
            personRequestMap.put(person.getId(), personRequest);
        }
    }*/

    /**
     * Remove the person request from the compartment request.
     *
     * @param compartmentRequest {@link CompartmentRequest} from which the requested person should be removed
     * @param personRequest      {@link PersonRequest} which should be removed
     */
    /*private void removeRequestedPerson(CompartmentRequest compartmentRequest, PersonRequest personRequest)
    {
        if (personRequest.getState() == PersonRequest.State.NOT_ASKED) {
            compartmentRequest.removeRequestedPerson(personRequest);
        }
        else {
            throw new IllegalStateException("Cannot remove person request from compartment request "
                    + "which was initiated by removing requested person from compartment, "
                    + "because the person has already been asked "
                    + "whether he will accepts the invitation.");
        }
    }*/

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
