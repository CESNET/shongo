package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.AbstractManager;
import cz.cesnet.shongo.controller.allocation.AllocatedCompartment;
import cz.cesnet.shongo.controller.allocation.AllocatedCompartmentManager;
import cz.cesnet.shongo.controller.allocation.AllocatedExternalEndpoint;
import cz.cesnet.shongo.controller.allocation.AllocatedItem;
import cz.cesnet.shongo.controller.common.Person;
import org.joda.time.Interval;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import java.util.*;

/**
 * Manager for {@link CompartmentRequest}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see AbstractManager
 */
public class CompartmentRequestManager extends AbstractManager
{
    /**
     * Constructor.
     *
     * @param entityManager
     */
    public CompartmentRequestManager(EntityManager entityManager)
    {
        super(entityManager);
    }

    /**
     * Create a new compartment request for a reservation request.
     *
     * @param compartment   compartment which will be requested in the compartment request
     * @param requestedSlot date/time slot for which the compartment request will be created
     * @return created compartment request
     */
    public CompartmentRequest create(Compartment compartment, Interval requestedSlot)
    {
        // Create compartment request
        CompartmentRequest compartmentRequest = new CompartmentRequest();
        compartmentRequest.setReservationRequest(compartment.getReservationRequest());
        compartmentRequest.setCompartment(compartment);
        compartmentRequest.setRequestedSlot(requestedSlot);

        // Set of all requested persons
        Set<Person> personSet = new HashSet<Person>();

        // Add requested resources and requested persons by these resources
        for (ResourceSpecification resourceSpecification : compartment.getRequestedResources()) {
            addRequestedResource(compartmentRequest, resourceSpecification, personSet, null);
        }

        // Add directly requested persons
        for (Person person : compartment.getRequestedPersons()) {
            addRequestedPerson(compartmentRequest, person, null, personSet, null);
        }

        // Set proper compartment request state
        compartmentRequest.updateStateByRequestedPersons();

        super.create(compartmentRequest);

        return compartmentRequest;
    }

    /**
     * Add requested resource to compartment request (and also all persons that are requested for the resource).
     *
     * @param compartmentRequest    {@link CompartmentRequest} to which the requested resource should be added
     * @param resourceSpecification {@link ResourceSpecification} of the requested resource
     * @param personSet             set of persons that have already been added to the compartment request
     * @param personRequestMap      map of person request that have already been added to the compartment request
     */
    private void addRequestedResource(CompartmentRequest compartmentRequest,
            ResourceSpecification resourceSpecification, Set<Person> personSet,
            Map<Long, PersonRequest> personRequestMap)
    {
        compartmentRequest.addRequestedResource(resourceSpecification);
        for (Person person : resourceSpecification.getRequestedPersons()) {
            addRequestedPerson(compartmentRequest, person, resourceSpecification, personSet, personRequestMap);
        }
    }

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
    private void addRequestedPerson(CompartmentRequest compartmentRequest, Person person,
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
    }

    /**
     * Remove the person request from the compartment request.
     *
     * @param compartmentRequest {@link CompartmentRequest} from which the requested person should be removed
     * @param personRequest      {@link PersonRequest} which should be removed
     */
    private void removeRequestedPerson(CompartmentRequest compartmentRequest, PersonRequest personRequest)
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
    }

    /**
     * @param compartmentRequest to be updated in database
     */
    public void update(CompartmentRequest compartmentRequest)
    {
        super.update(compartmentRequest);
    }

    /**
     * Modify compartment request according to compartment.
     *
     * @param compartmentRequest
     * @param compartment
     * @return true if some change(s) were made in the compartment request,
     *         false otherwise
     */
    public boolean update(CompartmentRequest compartmentRequest, Compartment compartment)
    {
        // Tracks whether the compartment request was modified
        boolean modified = false;

        // Build set of requested persons that is used to not allow same persons to be requested for a single
        // compartment request
        Set<Person> personSet = new HashSet<Person>();
        for (PersonRequest personRequest : compartmentRequest.getRequestedPersons()) {
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
        for (PersonRequest personRequest : compartmentRequest.getRequestedPersons()) {
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
        for (ResourceSpecification resourceSpecification : compartmentRequest.getRequestedResources()) {
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
                        for (PersonRequest possiblePersonRequest : compartmentRequest.getRequestedPersons()) {
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
                        addRequestedPerson(compartmentRequest, person, resourceSpecification, personSet,
                                personRequestMap);
                        modified = true;
                    }
                }

                // Resource specification is modified
                modified = true;
            }
            else {
                // Resource specification doesn't exists so add it
                addRequestedResource(compartmentRequest, resourceSpecification, personSet, personRequestMap);
                modified = true;
            }

            // Remove resource specification from map
            resourceSpecificationMap.remove(resourceSpecification.getId());
        }

        // Check all directly requested persons from the compartment
        for (Person person : compartment.getRequestedPersons()) {
            if (!personSet.contains(person)) {
                // Requested person doesn't exist so add new requested person
                addRequestedPerson(compartmentRequest, person, null, personSet, personRequestMap);
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
            removeRequestedPerson(compartmentRequest, personRequest);
            modified = true;
        }

        // Remove some resources from the map to not be deleted
        for (PersonRequest personRequest : compartmentRequest.getRequestedPersons()) {
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
            compartmentRequest.removeRequestedResource(resourceSpecification);
            modified = true;
        }

        // If the compartment request was modified, we must remove the allocated state from it
        // and set the new state based on requested persons
        if (modified) {
            compartmentRequest.clearState();
            compartmentRequest.updateStateByRequestedPersons();
        }

        update(compartmentRequest);

        return modified;
    }

    /**
     * Remove compartment request.
     *
     * @param compartmentRequest
     */
    public void delete(CompartmentRequest compartmentRequest)
    {
        AllocatedCompartmentManager allocatedCompartmentManager = new AllocatedCompartmentManager(entityManager);
        AllocatedCompartment allocatedCompartment =
                allocatedCompartmentManager.getByCompartmentRequest(compartmentRequest.getId());
        if (allocatedCompartment != null) {
            for (AllocatedItem allocatedItem : allocatedCompartment.getAllocatedItems()) {
                if (allocatedItem instanceof AllocatedExternalEndpoint) {
                    AllocatedExternalEndpoint allocatedExternalEndpoint = (AllocatedExternalEndpoint) allocatedItem;
                    allocatedExternalEndpoint.setExternalEndpointSpecification(null);
                }
            }
            allocatedCompartmentManager.markedForDeletion(allocatedCompartment);
        }
        super.delete(compartmentRequest);
    }

    /**
     * @param compartmentRequestId identifier for {@link CompartmentRequest}
     * @return {@link CompartmentRequest} with given identifier or null if the request doesn't exist
     */
    public CompartmentRequest get(Long compartmentRequestId)
    {
        try {
            CompartmentRequest compartmentRequest = entityManager.createQuery(
                    "SELECT request FROM CompartmentRequest request WHERE request.id = :id",
                    CompartmentRequest.class).setParameter("id", compartmentRequestId)
                    .getSingleResult();
            return compartmentRequest;
        }
        catch (NoResultException exception) {
            return null;
        }
    }


    /**
     * @param compartmentRequestId identifier for {@link CompartmentRequest}
     * @return {@link CompartmentRequest} with given identifier
     * @throws IllegalArgumentException when the compartment doesn't exist
     */
    public CompartmentRequest getNotNull(Long compartmentRequestId) throws IllegalArgumentException
    {
        CompartmentRequest compartmentRequest = get(compartmentRequestId);
        if (compartmentRequest == null) {
            throw new IllegalArgumentException("Compartment request '" + compartmentRequestId + "' doesn't exist!");
        }
        return compartmentRequest;
    }

    /**
     * @param reservationRequest
     * @return list of existing compartment requests for a {@link ReservationRequest}
     */
    public List<CompartmentRequest> listByReservationRequest(ReservationRequest reservationRequest)
    {
        return listByReservationRequest(reservationRequest.getId());
    }

    /**
     * @param reservationRequestId
     * @return list of existing compartment requests for a {@link ReservationRequest}
     */
    public List<CompartmentRequest> listByReservationRequest(Long reservationRequestId)
    {
        // Get existing compartment requests for compartment
        List<CompartmentRequest> compartmentRequestList = entityManager.createQuery(
                "SELECT request FROM CompartmentRequest request "
                        + "WHERE request.reservationRequest.id = :id "
                        + "ORDER BY request.requestedSlot.start", CompartmentRequest.class)
                .setParameter("id", reservationRequestId)
                .getResultList();
        return compartmentRequestList;
    }

    /**
     * @param reservationRequest
     * @param interval
     * @return list of existing compartment requests for a {@link ReservationRequest} taking place in given interval
     */
    public List<CompartmentRequest> listByReservationRequest(ReservationRequest reservationRequest, Interval interval)
    {
        return listByReservationRequest(reservationRequest.getId(), interval);
    }

    /**
     * @param reservationRequestId
     * @param interval
     * @return list of existing compartment requests for a {@link ReservationRequest} taking place in given interval
     */
    public List<CompartmentRequest> listByReservationRequest(Long reservationRequestId, Interval interval)
    {
        // Get existing compartment requests for compartment
        List<CompartmentRequest> compartmentRequestList = entityManager.createQuery(
                "SELECT request FROM CompartmentRequest request "
                        + "WHERE request.reservationRequest.id = :id "
                        + "AND request.requestedSlot.start BETWEEN :start AND :end", CompartmentRequest.class)
                .setParameter("id", reservationRequestId)
                .setParameter("start", interval.getStart())
                .setParameter("end", interval.getEnd())
                .getResultList();
        return compartmentRequestList;
    }

    /**
     * @param compartment
     * @param interval
     * @return list of existing compartments request from a given compartment taking place in given interval
     */
    public List<CompartmentRequest> listByCompartment(Compartment compartment, Interval interval)
    {
        return listByCompartment(compartment.getId(), interval);
    }

    /**
     * @param compartmentId
     * @param interval
     * @return list of existing compartments request from a given compartment taking place in given interval
     */
    public List<CompartmentRequest> listByCompartment(Long compartmentId, Interval interval)
    {
        List<CompartmentRequest> compartmentRequestList = entityManager.createQuery(
                "SELECT request FROM CompartmentRequest request "
                        + "WHERE request.compartment.id = :id "
                        + "AND request.requestedSlot.start BETWEEN :start AND :end", CompartmentRequest.class)
                .setParameter("id", compartmentId)
                .setParameter("start", interval.getStart())
                .setParameter("end", interval.getEnd())
                .getResultList();
        return compartmentRequestList;
    }

    /**
     * @param interval
     * @return list of existing complete compartments request taking place in given interval
     */
    public List<CompartmentRequest> listCompleted(Interval interval)
    {
        List<CompartmentRequest> compartmentRequestList = entityManager.createQuery(
                "SELECT request FROM CompartmentRequest request "
                        + "WHERE request.state = :state "
                        + "AND request.requestedSlot.start BETWEEN :start AND :end", CompartmentRequest.class)
                .setParameter("state", CompartmentRequest.State.COMPLETE)
                .setParameter("start", interval.getStart())
                .setParameter("end", interval.getEnd())
                .getResultList();
        return compartmentRequestList;
    }

    /**
     * @param compartmentRequestId
     * @param personId
     * @param resourceSpecification
     */
    public void selectResourceForPersonRequest(Long compartmentRequestId, Long personId,
            ResourceSpecification resourceSpecification)
    {
        CompartmentRequest compartmentRequest = getNotNull(compartmentRequestId);
        PersonRequest personRequest = getPersonRequest(compartmentRequest, personId);
        if (!compartmentRequest.containsRequestedResource(resourceSpecification)) {
            compartmentRequest.addRequestedResource(resourceSpecification);
        }
        personRequest.setResourceSpecification(resourceSpecification);
        update(compartmentRequest);
    }

    /**
     * Accept the invitation for specified person to participate in the specified compartment.
     *
     * @param compartmentRequestId identifier for {@link CompartmentRequest}
     * @param personId             identifier for {@link cz.cesnet.shongo.controller.common.Person}
     * @throws IllegalStateException when person hasn't selected resource by he will connect to the compartment yet
     */
    public void acceptPersonRequest(Long compartmentRequestId, Long personId) throws IllegalStateException
    {
        CompartmentRequest compartmentRequest = getNotNull(compartmentRequestId);
        PersonRequest personRequest = getPersonRequest(compartmentRequest, personId);
        if (personRequest.getResourceSpecification() == null) {
            throw new IllegalStateException("Cannot accept person '" + personId + "' to compartment request '"
                    + compartmentRequestId + "' because person hasn't selected the device be which he will connect "
                    + "to the compartment yet!");
        }
        personRequest.setState(PersonRequest.State.ACCEPTED);
        compartmentRequest.updateStateByRequestedPersons();
        update(compartmentRequest);
    }

    /**
     * Reject the invitation for specified person to participate in the specified compartment.
     *
     * @param compartmentRequestId identifier for {@link CompartmentRequest}
     * @param personId             identifier for {@link cz.cesnet.shongo.controller.common.Person}
     */
    public void rejectPersonRequest(Long compartmentRequestId, Long personId)
    {
        CompartmentRequest compartmentRequest = getNotNull(compartmentRequestId);
        PersonRequest personRequest = getPersonRequest(compartmentRequest, personId);
        personRequest.setState(PersonRequest.State.REJECTED);
        compartmentRequest.updateStateByRequestedPersons();
        update(compartmentRequest);
    }

    /**
     * @param compartmentRequest {@link CompartmentRequest} which is searched
     * @param personId           identifier for {@link cz.cesnet.shongo.controller.common.Person} for which the search is performed
     * @return {@link PersonRequest} from given {@link CompartmentRequest} that references person with given identifier
     * @throws IllegalArgumentException when {@link PersonRequest} is not found
     */
    private PersonRequest getPersonRequest(CompartmentRequest compartmentRequest, Long personId)
            throws IllegalArgumentException
    {
        for (PersonRequest personRequest : compartmentRequest.getRequestedPersons()) {
            if (personRequest.getPerson().getId().equals(personId)) {
                return personRequest;
            }
        }
        throw new IllegalArgumentException("Requested person '" + personId + "' doesn't exist in compartment request '"
                + compartmentRequest.getId() + "'!");
    }
}
