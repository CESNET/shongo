package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.AbstractManager;
import cz.cesnet.shongo.fault.EntityNotFoundException;
import cz.cesnet.shongo.fault.TodoImplementException;
import org.joda.time.Interval;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import java.util.List;

/**
 * Manager for {@link AbstractReservationRequest}.
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
     * @param abstractReservationRequest to be validated
     */
    private void validate(AbstractReservationRequest abstractReservationRequest) throws IllegalArgumentException
    {
        if (abstractReservationRequest.getType() == null) {
            throw new IllegalArgumentException("Reservation request must have type set!");
        }
    }

    /**
     * Create a new {@link AbstractReservationRequest} in the database.
     *
     * @param abstractReservationRequest to be created in the database
     */
    public void create(AbstractReservationRequest abstractReservationRequest)
    {
        validate(abstractReservationRequest);

        super.create(abstractReservationRequest);
    }

    /**
     * Update existing {@link AbstractReservationRequest} in the database.
     *
     * @param abstractReservationRequest to be updated in the database
     */
    public void update(AbstractReservationRequest abstractReservationRequest)
    {
        validate(abstractReservationRequest);

        Transaction transaction = beginTransaction();

        super.update(abstractReservationRequest);

        if (abstractReservationRequest instanceof ReservationRequestSet) {
            ReservationRequestSet reservationRequestSet = (ReservationRequestSet) abstractReservationRequest;
            ReservationRequestSetStateManager.setState(entityManager, reservationRequestSet,
                    ReservationRequestSet.State.NOT_PREPROCESSED);
        }

        transaction.commit();
    }

    /**
     * Delete existing {@link AbstractReservationRequest} in the database.
     *
     * @param abstractReservationRequest to be deleted from the database
     */
    public void delete(AbstractReservationRequest abstractReservationRequest)
    {
        Transaction transaction = beginTransaction();

        if (abstractReservationRequest instanceof ReservationRequestSet) {
            // Delete all reservation requests from set
            ReservationRequestSet reservationRequestSet = (ReservationRequestSet) abstractReservationRequest;
            for (ReservationRequest reservationRequest : reservationRequestSet.getReservationRequests()) {
                delete(reservationRequest);
            }
            // Clear state
            ReservationRequestSetStateManager.clear(entityManager, reservationRequestSet);
        }
        else if (abstractReservationRequest instanceof ReservationRequest)
        {
            throw new TodoImplementException();
            /*AllocatedCompartmentManager allocatedCompartmentManager = new AllocatedCompartmentManager(entityManager);
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
            }*/
        }

        super.delete(abstractReservationRequest);

        transaction.commit();
    }

    /**
     * @param reservationRequestId of the {@link ReservationRequest}
     * @return {@link ReservationRequest} with given identifier
     * @throws EntityNotFoundException when the {@link ReservationRequest} doesn't exist
     */
    public ReservationRequest getReservationRequest(Long reservationRequestId) throws EntityNotFoundException
    {
        try {
            ReservationRequest reservationRequest = entityManager.createQuery(
                    "SELECT request FROM ReservationRequest request WHERE request.id = :id",
                    ReservationRequest.class).setParameter("id", reservationRequestId)
                    .getSingleResult();
            return reservationRequest;
        }
        catch (NoResultException exception) {
            throw new EntityNotFoundException(ReservationRequest.class, reservationRequestId);
        }
    }

    /**
     * @param reservationRequestSetId of the {@link ReservationRequestSet}
     * @return {@link ReservationRequestSet} with given identifier
     * @throws EntityNotFoundException when the {@link ReservationRequestSet} doesn't exist
     */
    public ReservationRequestSet getReservationRequestSet(Long reservationRequestSetId) throws EntityNotFoundException
    {
        try {
            ReservationRequestSet reservationRequestSet = entityManager.createQuery(
                    "SELECT request FROM ReservationRequestSet request WHERE request.id = :id",
                    ReservationRequestSet.class).setParameter("id", reservationRequestSetId)
                    .getSingleResult();
            return reservationRequestSet;
        }
        catch (NoResultException exception) {
            throw new EntityNotFoundException(ReservationRequestSet.class, reservationRequestSetId);
        }
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
    public List<ReservationRequestSet> listNotPreprocessedReservationRequestSets(Interval interval)
    {
        List<ReservationRequestSet> reservationRequestList = entityManager
                .createQuery("SELECT request FROM ReservationRequestSet request WHERE request NOT IN (" +
                        "SELECT state.reservationRequestSet FROM ReservationRequestSetPreprocessedState state "
                        + "WHERE state.start <= :from AND state.end >= :to)",
                        ReservationRequestSet.class).setParameter("from", interval.getStart())
                .setParameter("to", interval.getEnd())
                .getResultList();
        return reservationRequestList;
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
     * @param reservationRequestSet from which the {@link ReservationRequest}s should be returned
     * @param interval              in which the {@link ReservationRequest}s should tak place
     * @return list of existing {@link ReservationRequest}s for a {@link ReservationRequestSet} taking place in
     *         given {@code interval}
     */
    public List<ReservationRequest> listReservationRequestsBySet(ReservationRequestSet reservationRequestSet,
            Interval interval)
    {
        return listByReservationRequest(reservationRequestSet.getId(), interval);
    }

    /**
     * @param reservationRequestSetId of the {@link ReservationRequestSet} from which the {@link ReservationRequest}s
     *                                should be returned
     * @param interval                in which the {@link ReservationRequest}s should tak place
     * @return list of existing {@link ReservationRequest}s for a {@link ReservationRequestSet} taking place in
     *         given {@code interval}
     */
    public List<ReservationRequest> listByReservationRequest(Long reservationRequestSetId, Interval interval)
    {
        // Get existing reservation requests for the set
        List<ReservationRequest> compartmentRequestList = entityManager.createQuery(
                "SELECT request FROM ReservationRequestSet set "
                        + "LEFT JOIN set.reservationRequest request"
                        + "WHERE set.id = :id "
                        + "AND request.requestedSlot.start BETWEEN :start AND :end", ReservationRequest.class)
                .setParameter("id", reservationRequestSetId)
                .setParameter("start", interval.getStart())
                .setParameter("end", interval.getEnd())
                .getResultList();
        return compartmentRequestList;
    }

    /**
     * @param specification which should be specified in the {@link ReservationRequest}
     * @param interval      in which the {@link ReservationRequest} should take place
     * @return list of existing {@link ReservationRequest}s from a given {@link Specification} taking place
     *         in given interval
     */
    public List<ReservationRequest> listReservationRequestsBySpecification(Specification specification,
            Interval interval)
    {
        return listReservationRequestsBySpecification(specification.getId(), interval);
    }

    /**
     * @param specificationId of the {@link Specification} which should be specified in the {@link ReservationRequest}
     * @param interval        in which the {@link ReservationRequest} should take place
     * @return list of existing {@link ReservationRequest}s from a given {@link Specification} taking place
     *         in given interval
     */
    public List<ReservationRequest> listReservationRequestsBySpecification(Long specificationId, Interval interval)
    {
        List<ReservationRequest> compartmentRequestList = entityManager.createQuery(
                "SELECT request FROM ReservationRequest request "
                        + "WHERE request.requestedSpecification.id = :id "
                        + "AND request.requestedSlot.start BETWEEN :start AND :end", ReservationRequest.class)
                .setParameter("id", specificationId)
                .setParameter("start", interval.getStart())
                .setParameter("end", interval.getEnd())
                .getResultList();
        return compartmentRequestList;
    }

    /**
     * @param interval
     * @return list of existing complete compartments request starting in given interval
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
