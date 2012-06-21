package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.common.AbstractManager;
import cz.cesnet.shongo.common.Person;
import org.joda.time.Interval;

import javax.persistence.EntityManager;
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
     * @see PersonRequestManager
     */
    private PersonRequestManager personRequestManager;

    /**
     * Constructor.
     *
     * @param entityManager
     */
    public CompartmentRequestManager(EntityManager entityManager)
    {
        this(entityManager, new PersonRequestManager(entityManager));
    }

    /**
     * Constructor.
     *
     * @param entityManager
     * @param personRequestManager
     */
    public CompartmentRequestManager(EntityManager entityManager, PersonRequestManager personRequestManager)
    {
        super(entityManager);
        this.personRequestManager = personRequestManager;
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
        compartmentRequest.updateState();

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
     * Modify compartment request according to compartment.
     *
     * @param compartmentRequest
     * @param compartment
     */
    public void update(CompartmentRequest compartmentRequest, Compartment compartment)
    {
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

        // Check all requested resource from the compartment
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
                                // TODO: If old resource spec. doesn't has any requested persons consider to remove it
                            }
                            else if (personRequest.getState() == PersonRequest.State.ASKED) {
                                throw new IllegalStateException("Cannot change requested person resource "
                                        + "in compartment request because the person has already been asked "
                                        + "whether he will accepts the invitation.");
                            }
                            else if (personRequest.getState() == PersonRequest.State.REJECTED) {
                                // Do nothing he selected other resource
                            }
                            else if (personRequest.getState() == PersonRequest.State.REJECTED) {
                                // Do nothing he don't want to collaborate
                            }
                        }
                    }
                    else {
                        // Requested person doesn't exist so add new requested person
                        addRequestedPerson(compartmentRequest, person, resourceSpecification, personSet,
                                personRequestMap);
                    }
                }
            }
            else {
                // Resource specification doesn't exists so add it
                addRequestedResource(compartmentRequest, resourceSpecification, personSet, personRequestMap);
            }

            // Remove resource specification from map
            resourceSpecificationMap.remove(resourceSpecification.getId());
        }

        // Check all directly requested persons from the compartment
        for (Person person : compartment.getRequestedPersons()) {
            if (!personSet.contains(person)) {
                // Requested person doesn't exist so add new requested person
                addRequestedPerson(compartmentRequest, person, null, personSet, personRequestMap);
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
        }

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
        }

        super.update(compartmentRequest);
    }

    /**
     * Remove compartment request.
     *
     * @param compartmentRequest
     */
    public void delete(CompartmentRequest compartmentRequest)
    {
        super.delete(compartmentRequest);
    }

    /**
     * @param reservationRequest
     * @return list of existing compartment requests for a {@link ReservationRequest}
     */
    public List<CompartmentRequest> list(ReservationRequest reservationRequest)
    {
        // Get existing compartment requests for compartment
        List<CompartmentRequest> compartmentRequestList = entityManager.createQuery(
                "SELECT request FROM CompartmentRequest request "
                        + "WHERE request.reservationRequest = :reservationRequest "
                        + "ORDER BY request.requestedSlot.start", CompartmentRequest.class)
                .setParameter("reservationRequest", reservationRequest)
                .getResultList();
        return compartmentRequestList;
    }

    /**
     * @param reservationRequest
     * @param interval
     * @return list of existing compartment requests for a {@link ReservationRequest}
     */
    public List<CompartmentRequest> list(ReservationRequest reservationRequest, Interval interval)
    {
        // Get existing compartment requests for compartment
        List<CompartmentRequest> compartmentRequestList = entityManager.createQuery(
                "SELECT request FROM CompartmentRequest request "
                        + "WHERE request.reservationRequest = :reservationRequest "
                        + "AND request.requestedSlot.start BETWEEN :start AND :end", CompartmentRequest.class)
                .setParameter("reservationRequest", reservationRequest)
                .setParameter("start", interval.getStart())
                .setParameter("end", interval.getEnd())
                .getResultList();
        return compartmentRequestList;
    }

    /**
     * @param compartment
     * @return list of existing compartments request from a given compartment
     */
    public List<CompartmentRequest> list(Compartment compartment, Interval interval)
    {
        List<CompartmentRequest> compartmentRequestList = entityManager.createQuery(
                "SELECT request FROM CompartmentRequest request "
                        + "WHERE request.compartment = :compartment "
                        + "AND request.requestedSlot.start BETWEEN :start AND :end", CompartmentRequest.class)
                .setParameter("compartment", compartment)
                .setParameter("start", interval.getStart())
                .setParameter("end", interval.getEnd())
                .getResultList();
        return compartmentRequestList;
    }
}
