package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.AbstractManager;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.common.Person;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.reservation.ReservationManager;
import cz.cesnet.shongo.fault.EntityNotFoundException;
import cz.cesnet.shongo.fault.EntityToDeleteIsReferencedException;
import cz.cesnet.shongo.fault.FaultException;
import org.joda.time.Interval;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    }

    /**
     * Create a new {@link AbstractReservationRequest} in the database.
     *
     * @param abstractReservationRequest to be created in the database
     */
    public void create(AbstractReservationRequest abstractReservationRequest)
    {
        validate(abstractReservationRequest);

        if (abstractReservationRequest instanceof ReservationRequest) {
            ReservationRequest reservationRequest = (ReservationRequest) abstractReservationRequest;
            if (reservationRequest.getState() == null) {
                reservationRequest.updateStateBySpecifications();
            }
        }

        super.create(abstractReservationRequest);
    }

    /**
     * Update existing {@link AbstractReservationRequest} in the database.
     *
     * @param reservationRequest     to be updated in the database
     * @param clearPreprocessedState specifies whether state for {@link ReservationRequestSet} and
     *                               {@link PermanentReservationRequest} should be set to
     *                               {@link PreprocessorState#NOT_PREPROCESSED}
     */
    public void update(AbstractReservationRequest reservationRequest, boolean clearPreprocessedState)
    {
        validate(reservationRequest);

        Transaction transaction = beginTransaction();

        super.update(reservationRequest);

        if (clearPreprocessedState &&
                (reservationRequest instanceof ReservationRequestSet
                         || reservationRequest instanceof PermanentReservationRequest)) {
            PreprocessorStateManager.setState(entityManager, reservationRequest, PreprocessorState.NOT_PREPROCESSED);
        }

        transaction.commit();
    }

    /**
     * Update existing {@link AbstractReservationRequest} in the database and set
     * {@link PreprocessorState#NOT_PREPROCESSED} state.
     *
     * @param reservationRequest to be updated in the database
     */
    public void update(AbstractReservationRequest reservationRequest)
    {
        update(reservationRequest, true);
    }

    /**
     * Delete existing {@link AbstractReservationRequest} in the database.
     *
     * @param abstractReservationRequest to be deleted from the database
     * @throws FaultException when the deletion failed
     */
    public void delete(AbstractReservationRequest abstractReservationRequest) throws FaultException
    {
        Transaction transaction = beginTransaction();

        if (abstractReservationRequest instanceof ReservationRequestSet) {
            // Delete all reservation requests from set
            ReservationRequestSet reservationRequestSet = (ReservationRequestSet) abstractReservationRequest;
            for (ReservationRequest reservationRequest : reservationRequestSet.getReservationRequests()) {
                delete(reservationRequest);
            }
            // Clear state
            PreprocessorStateManager.clear(entityManager, reservationRequestSet);
        }
        else if (abstractReservationRequest instanceof PermanentReservationRequest) {
            // Clear state
            PreprocessorStateManager.clear(entityManager, abstractReservationRequest);
        }
        else if (abstractReservationRequest instanceof ReservationRequest) {
            // Keep reservation (is deleted by scheduler)
            ReservationRequest reservationRequest = (ReservationRequest) abstractReservationRequest;
            Reservation reservation = reservationRequest.getReservation();
            if (reservation != null) {
                reservationRequest.setReservation(null);
                update(reservationRequest);
                // Check if reservation can be deleted
                ReservationManager reservationManager = new ReservationManager(entityManager);
                if (!reservationManager.isProvided(reservation)) {
                    throw new EntityToDeleteIsReferencedException(ReservationRequest.class, reservationRequest.getId());
                }
            }
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
     * @param reservationId for {@link Reservation} which is allocated {@link AbstractReservationRequest} which should
     *                      be returned
     * @return {@link AbstractReservationRequest} for which is allocated {@link Reservation} with
     *         given {@code reservationId}
     */
    public AbstractReservationRequest getByReservation(Long reservationId)
    {
        AbstractReservationRequest reservationRequest = entityManager.createQuery(
                "SELECT reservationRequest FROM AbstractReservationRequest reservationRequest"
                        + " WHERE reservationRequest IN("
                        + "   SELECT reservationRequestSet FROM ReservationRequestSet reservationRequestSet"
                        + "   LEFT JOIN reservationRequestSet.reservationRequests reservationRequest"
                        + "   WHERE reservationRequest.reservation.id = :id"
                        + ") OR reservationRequest IN("
                        + "   SELECT reservationRequest FROM ReservationRequest reservationRequest"
                        + "   WHERE reservationRequest.reservation.id = :id AND reservationRequest NOT IN("
                        + "       SELECT reservationRequest FROM ReservationRequestSet reservationRequestSet"
                        + "       LEFT JOIN reservationRequestSet.reservationRequests reservationRequest)"
                        + ") OR reservationRequest.id IN("
                        + "   SELECT reservationRequest FROM PermanentReservationRequest reservationRequest"
                        + "   LEFT JOIN reservationRequest.resourceReservations reservation"
                        + "   WHERE reservation.id = :id"
                        + ")", AbstractReservationRequest.class)
                .setParameter("id", reservationId)
                .getSingleResult();
        return reservationRequest;
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
     * @param userId       requested owner
     * @param technologies requested technologies
     * @return list all reservation requests for given {@code owner} and {@code technologies} in the database.
     */
    public List<AbstractReservationRequest> list(Long userId, Set<Technology> technologies)
    {
        Map<String, Object> parameters = new HashMap<String, Object>();
        StringBuilder whereClause = new StringBuilder();
        // List only reservation requests created by any user (skip requests created by the preprocessor
        whereClause.append("(TYPE(request) != ReservationRequest"
                + " OR request.createdBy = :createdBy)");
        // List only reservation requests which are owned by the given user
        if (userId != null) {
            whereClause.append(" AND request.userId = :userId");
            parameters.put("userId", userId);
        }
        // List only reservation requests which specifies given technologies in virtual room or compartment
        if (technologies != null && technologies.size() > 0) {
            whereClause.append(" AND ("
                    + "  request IN ("
                    + "    SELECT reservationRequest"
                    + "    FROM AbstractReservationRequest reservationRequest, RoomSpecification specification"
                    + "    LEFT JOIN reservationRequest.specifications reservationRequestSpecification"
                    + "    LEFT JOIN specification.technologies technology"
                    + "    WHERE (reservationRequest.specification = specification OR"
                    + "           reservationRequestSpecification = specification) AND technology IN(:technologies)"
                    + "  ) OR request IN ("
                    + "    SELECT reservationRequest"
                    + "    FROM AbstractReservationRequest reservationRequest, CompartmentSpecification specification"
                    + "    LEFT JOIN reservationRequest.specifications reservationRequestSpecification"
                    + "    LEFT JOIN specification.technologies technology"
                    + "    WHERE (reservationRequest.specification = specification OR"
                    + "           reservationRequestSpecification = specification) AND technology IN(:technologies)"
                    + "  )"
                    + ")");
            parameters.put("technologies", technologies);
        }

        TypedQuery<AbstractReservationRequest> query = entityManager.createQuery("SELECT request"
                + " FROM AbstractReservationRequest request"
                + " WHERE " + whereClause.toString(),
                AbstractReservationRequest.class);
        query.setParameter("createdBy", ReservationRequest.CreatedBy.USER);
        for (String parameterName : parameters.keySet()) {
            query.setParameter(parameterName, parameters.get(parameterName));
        }
        List<AbstractReservationRequest> reservationRequestList = query.getResultList();
        return reservationRequestList;
    }

    /**
     * @return list all {@link PermanentReservationRequest} which aren't preprocessed in given interval.
     */
    public List<PermanentReservationRequest> listNotPreprocessedPermanentReservationRequests(Interval interval)
    {
        List<PermanentReservationRequest> reservationRequestList = entityManager
                .createQuery("SELECT reservationRequest FROM PermanentReservationRequest reservationRequest"
                        + " WHERE reservationRequest NOT IN ("
                        + " SELECT state.reservationRequest FROM PreprocessedState state"
                        + " WHERE state.start <= :from AND state.end >= :to)",
                        PermanentReservationRequest.class)
                .setParameter("from", interval.getStart())
                .setParameter("to", interval.getEnd())
                .getResultList();
        return reservationRequestList;
    }

    /**
     * @return list all {@link ReservationRequestSet} which aren't preprocessed in given interval.
     */
    public List<ReservationRequestSet> listNotPreprocessedReservationRequestSets(Interval interval)
    {
        List<ReservationRequestSet> reservationRequestList = entityManager
                .createQuery("SELECT reservationRequest FROM ReservationRequestSet reservationRequest"
                        + " WHERE reservationRequest NOT IN ("
                        + " SELECT state.reservationRequest FROM PreprocessedState state"
                        + " WHERE state.start <= :from AND state.end >= :to)",
                        ReservationRequestSet.class)
                .setParameter("from", interval.getStart())
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
                        + " ORDER BY reservationRequest.slotStart", ReservationRequest.class)
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
                        + " AND reservationRequest.slotStart < :end"
                        + " AND reservationRequest.slotEnd > :start",
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
                        + " AND reservationRequest.slotStart < :end"
                        + " AND reservationRequest.slotEnd > :start",
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
