package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.AbstractManager;
import cz.cesnet.shongo.controller.common.Person;
import cz.cesnet.shongo.controller.reservation.CompartmentReservation;
import cz.cesnet.shongo.fault.EntityNotFoundException;
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
        } else if (abstractReservationRequest instanceof ReservationRequest) {
            // Keep reservation (is deleted by scheduler)
            ReservationRequest reservationRequest = (ReservationRequest) abstractReservationRequest;
            reservationRequest.setReservation(null);
        }

        super.delete(abstractReservationRequest);

        transaction.commit();
    }

    /**
     * @param reservationRequestId of the {@link AbstractReservationRequest}
     * @return {@link AbstractReservationRequest} with given identifier
     * @throws EntityNotFoundException when the {@link AbstractReservationRequest} doesn't exist
     */
    public AbstractReservationRequest get(Long reservationRequestId) throws EntityNotFoundException
    {
        try {
            AbstractReservationRequest reservationRequest = entityManager.createQuery(
                    "SELECT reservationRequest FROM AbstractReservationRequest reservationRequest"
                            + " WHERE reservationRequest.id = :id",
                    AbstractReservationRequest.class).setParameter("id", reservationRequestId)
                    .getSingleResult();
            return reservationRequest;
        }
        catch (NoResultException exception) {
            throw new EntityNotFoundException(AbstractReservationRequest.class, reservationRequestId);
        }
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
                    "SELECT reservationRequest FROM ReservationRequest reservationRequest"
                            + " WHERE reservationRequest.id = :id",
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
                    "SELECT reservationRequest FROM ReservationRequestSet reservationRequest"
                            + " WHERE reservationRequest.id = :id",
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
    public List<AbstractReservationRequest> list()
    {
        List<AbstractReservationRequest> reservationRequestList = entityManager
                .createQuery("SELECT reservationRequest FROM AbstractReservationRequest reservationRequest"
                        + " WHERE reservationRequest NOT IN ("
                        + "  SELECT reservationRequest FROM ReservationRequest reservationRequest"
                        + "  WHERE reservationRequest.createdBy = :createdBy"
                        + " )",
                        AbstractReservationRequest.class)
                .setParameter("createdBy", ReservationRequest.CreatedBy.CONTROLLER)
                .getResultList();
        return reservationRequestList;
    }

    /**
     * @return list all reservation requests in the database which aren't preprocessed in given interval.
     */
    public List<ReservationRequestSet> listNotPreprocessedReservationRequestSets(Interval interval)
    {
        List<ReservationRequestSet> reservationRequestList = entityManager
                .createQuery("SELECT reservationRequestSet FROM ReservationRequestSet reservationRequestSet"
                        + " WHERE reservationRequestSet NOT IN ("
                        + " SELECT state.reservationRequestSet FROM ReservationRequestSetPreprocessedState state"
                        + " WHERE state.start <= :from AND state.end >= :to)",
                        ReservationRequestSet.class).setParameter("from", interval.getStart())
                .setParameter("to", interval.getEnd())
                .getResultList();
        return reservationRequestList;
    }

    /**
     * @param reservationRequestId of the {@link ReservationRequest}
     * @return {@link ReservationRequest} with given identifier
     * @throws IllegalArgumentException when the {@link ReservationRequest} doesn't exist
     */
    public ReservationRequest getReservationRequestNotNull(Long reservationRequestId) throws IllegalArgumentException
    {
        try {
            return getReservationRequest(reservationRequestId);
        }
        catch (EntityNotFoundException e) {
            throw new IllegalArgumentException("Reservation request '" + reservationRequestId + "' doesn't exist!");
        }
    }

    /**
     * @param reservationRequestSet from which the {@link ReservationRequest}s should be returned
     * @return list of existing {@link ReservationRequest}s for a {@link ReservationRequestSet} taking place
     */
    public List<ReservationRequest> listReservationRequestsBySet(ReservationRequestSet reservationRequestSet)
    {
        return listReservationRequestsBySet(reservationRequestSet.getId());
    }

    /**
     * @param reservationRequestSetId of the {@link ReservationRequestSet} from which the {@link ReservationRequest}s
     *                                should be returned
     * @return list of existing {@link ReservationRequest}s for a {@link ReservationRequestSet}
     */
    public List<ReservationRequest> listReservationRequestsBySet(Long reservationRequestSetId)
    {
        List<ReservationRequest> compartmentRequestList = entityManager.createQuery(
                "SELECT reservationRequest FROM ReservationRequest reservationRequest"
                        + " WHERE reservationRequest.id IN("
                        + " SELECT reservationRequest.id FROM ReservationRequestSet reservationRequestSet"
                        + " LEFT JOIN reservationRequestSet.reservationRequests reservationRequest"
                        + " WHERE reservationRequestSet.id = :id)"
                        + " ORDER BY reservationRequest.requestedSlotStart", ReservationRequest.class)
                .setParameter("id", reservationRequestSetId)
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
        return listReservationRequestsBySet(reservationRequestSet.getId(), interval);
    }

    /**
     * @param reservationRequestSetId of the {@link ReservationRequestSet} from which the {@link ReservationRequest}s
     *                                should be returned
     * @param interval                in which the {@link ReservationRequest}s should tak place
     * @return list of existing {@link ReservationRequest}s for a {@link ReservationRequestSet} taking place in
     *         given {@code interval}
     */
    public List<ReservationRequest> listReservationRequestsBySet(Long reservationRequestSetId, Interval interval)
    {
        List<ReservationRequest> reservationRequests = entityManager.createQuery(
                "SELECT reservationRequest FROM ReservationRequestSet reservationRequestSet"
                        + " LEFT JOIN reservationRequestSet.reservationRequests reservationRequest"
                        + " WHERE reservationRequestSet.id = :id "
                        + " AND reservationRequest.requestedSlotStart BETWEEN :start AND :end",
                ReservationRequest.class)
                .setParameter("id", reservationRequestSetId)
                .setParameter("start", interval.getStart())
                .setParameter("end", interval.getEnd())
                .getResultList();
        return reservationRequests;
    }

    /**
     * @param interval
     * @return list of existing {@link ReservationRequest}s in {@link ReservationRequest.State#COMPLETE} state and
     *         starting in given interval
     */
    public List<ReservationRequest> listCompletedReservationRequests(Interval interval)
    {
        List<ReservationRequest> compartmentRequestList = entityManager.createQuery(
                "SELECT reservationRequest FROM ReservationRequest reservationRequest"
                        + " WHERE reservationRequest.state = :state"
                        + " AND reservationRequest.requestedSlotStart BETWEEN :start AND :end",
                ReservationRequest.class)
                .setParameter("state", ReservationRequest.State.COMPLETE)
                .setParameter("start", interval.getStart())
                .setParameter("end", interval.getEnd())
                .getResultList();

        return compartmentRequestList;
    }

    /**
     * @param specification {@link Specification} which is searched
     * @param personId      identifier for {@link Person} for which the search is performed
     * @return {@link PersonSpecification} from given {@link Specification} that references {@link Person}
     *         with given identifier if exists, null otherwise
     */
    private PersonSpecification getPersonSpecification(Specification specification, Long personId)
            throws IllegalArgumentException
    {
        if (specification instanceof PersonSpecification) {
            PersonSpecification personSpecification = (PersonSpecification) specification;
            if (personSpecification.getPerson().getId().equals(personId)) {
                return personSpecification;
            }
        }
        else if (specification instanceof CompartmentSpecification) {
            CompartmentSpecification compartmentSpecification = (CompartmentSpecification) specification;
            for (Specification childSpecification : compartmentSpecification.getChildSpecifications()) {
                PersonSpecification personSpecification = getPersonSpecification(childSpecification, personId);
                if (personSpecification != null) {
                    return personSpecification;
                }
            }
        }
        return null;
    }

    /**
     * @param reservationRequest {@link ReservationRequest} which is searched
     * @param personId           identifier for {@link Person} for which the search is performed
     * @return {@link PersonSpecification} from given {@link ReservationRequest} that references {@link Person}
     *         with given identifier
     * @throws IllegalArgumentException when {@link PersonSpecification} isn't found
     */
    private PersonSpecification getPersonSpecification(ReservationRequest reservationRequest, Long personId)
            throws IllegalArgumentException
    {
        Specification specification = reservationRequest.getSpecification();
        PersonSpecification personSpecification = getPersonSpecification(specification, personId);
        if (personSpecification == null) {
            throw new IllegalArgumentException(
                    String.format("Requested person '%d' doesn't exist in specification '%d'!",
                            personId, specification.getId()));
        }
        return personSpecification;
    }

    /**
     * Accept the invitation for specified {@link Person} to participate in the specified {@link ReservationRequest}.
     *
     * @param reservationRequestId identifier for {@link ReservationRequest}
     * @param personId             identifier for {@link Person}
     * @throws IllegalStateException when {@link Person} hasn't selected resource by he will connect to
     *                               the video conference yet
     */
    public void acceptPersonRequest(Long reservationRequestId, Long personId) throws IllegalStateException
    {
        ReservationRequest reservationRequest = getReservationRequestNotNull(reservationRequestId);
        PersonSpecification personSpecification = getPersonSpecification(reservationRequest, personId);
        if (personSpecification.getEndpointSpecification() == null) {
            throw new IllegalStateException(
                    String.format("Cannot accept person '%d' to compartment request '%d' because person hasn't "
                            + "selected the device be which he will connect to the compartment yet!",
                            personId, reservationRequestId));
        }
        personSpecification.setInvitationState(PersonSpecification.InvitationState.ACCEPTED);
        reservationRequest.updateStateBySpecifications();
        update(reservationRequest);
    }

    /**
     * Reject the invitation for specified {@link Person} to participate in the specified {@link ReservationRequest}.
     *
     * @param reservationRequestId identifier for {@link ReservationRequest}
     * @param personId             identifier for {@link Person}
     */
    public void rejectPersonRequest(Long reservationRequestId, Long personId)
    {
        ReservationRequest reservationRequest = getReservationRequestNotNull(reservationRequestId);
        PersonSpecification personSpecification = getPersonSpecification(reservationRequest, personId);
        personSpecification.setInvitationState(PersonSpecification.InvitationState.REJECTED);
        reservationRequest.updateStateBySpecifications();
        update(reservationRequest);
    }

    /**
     * @param reservationRequestId
     * @param personId
     * @param endpointSpecification
     */
    public void selectEndpointForPersonSpecification(Long reservationRequestId, Long personId,
            EndpointSpecification endpointSpecification)
    {
        ReservationRequest reservationRequest = getReservationRequestNotNull(reservationRequestId);
        PersonSpecification personSpecification = getPersonSpecification(reservationRequest, personId);

        CompartmentSpecification compartmentSpecification =
                (CompartmentSpecification) reservationRequest.getSpecification();
        if (!compartmentSpecification.containsSpecification(endpointSpecification)) {
            compartmentSpecification.addChildSpecification(endpointSpecification);
        }
        personSpecification.setEndpointSpecification(endpointSpecification);
        update(reservationRequest);
    }
}
