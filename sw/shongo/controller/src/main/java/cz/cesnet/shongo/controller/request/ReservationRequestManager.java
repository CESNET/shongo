package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.AbstractManager;
import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.ControllerReportSetHelper;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.reservation.ReservationManager;
import cz.cesnet.shongo.controller.util.DatabaseFilter;
import org.joda.time.Interval;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import java.util.LinkedList;
import java.util.List;
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
     * Create a new {@link AbstractReservationRequest} in the database.
     *
     * @param abstractReservationRequest to be created in the database
     */
    public void create(AbstractReservationRequest abstractReservationRequest)
    {
        if (abstractReservationRequest instanceof ReservationRequest) {
            ReservationRequest reservationRequest = (ReservationRequest) abstractReservationRequest;
            if (reservationRequest.getState() == null) {
                reservationRequest.updateStateBySpecification();
            }
        }
        super.create(abstractReservationRequest);
    }

    /**
     * Update existing {@link AbstractReservationRequest} in the database.
     *
     * @param reservationRequest     to be updated in the database
     * @param clearPreprocessedState specifies whether state for {@link ReservationRequestSet} should be set to
     *                               {@link PreprocessorState#NOT_PREPROCESSED}
     */
    public void update(AbstractReservationRequest reservationRequest, boolean clearPreprocessedState)
    {
        PersistenceTransaction transaction = beginPersistenceTransaction();

        super.update(reservationRequest);

        if (clearPreprocessedState && reservationRequest instanceof ReservationRequestSet) {
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
     * @param oldReservationRequest old reservation request which is being modified
     * @param newReservationRequest new reservation request by which the {@code oldReservationRequest} is modified
     */
    public void modify(AbstractReservationRequest oldReservationRequest,
            AbstractReservationRequest newReservationRequest)
    {
        // Set old reservation request as modified
        oldReservationRequest.setType(AbstractReservationRequest.Type.MODIFIED);

        // Create new reservation request
        create(newReservationRequest);

        // Update existing modified reservation requests
        entityManager.createQuery("UPDATE ModifiedReservationRequest"
                + " SET primaryKey.latestReservationRequest = :newReservationRequest"
                + " WHERE primaryKey.latestReservationRequest = :oldReservationRequest")
                .setParameter("oldReservationRequest", oldReservationRequest)
                .setParameter("newReservationRequest", newReservationRequest)
                .executeUpdate();

        // Create new modified reservation request
        ModifiedReservationRequest modifiedReservationRequest = new ModifiedReservationRequest();
        modifiedReservationRequest.setOldReservationRequest(oldReservationRequest);
        modifiedReservationRequest.setNewReservationRequest(newReservationRequest);
        modifiedReservationRequest.setLatestReservationRequest(newReservationRequest);
        entityManager.persist(modifiedReservationRequest);
    }

    public void softDelete(AbstractReservationRequest reservationRequest, AuthorizationManager authorizationManager)
    {
        PersistenceTransaction transaction = beginPersistenceTransaction();

        Long reservationRequestId = reservationRequest.getId();
        List<AbstractReservationRequest> versions = listVersions(reservationRequestId);
        for (AbstractReservationRequest version : versions) {
            delete(version, authorizationManager, false);
        }

        transaction.commit();
    }

    public void hardDelete(AbstractReservationRequest reservationRequest, AuthorizationManager authorizationManager)
    {
        PersistenceTransaction transaction = beginPersistenceTransaction();

        Long reservationRequestId = reservationRequest.getId();
        List<AbstractReservationRequest> versions = listVersions(reservationRequestId);
        for (AbstractReservationRequest version : versions) {
            delete(version, authorizationManager, true);
        }

        transaction.commit();
    }

    /**
     * Delete existing {@link AbstractReservationRequest} in the database.
     *
     * @param abstractReservationRequest to be deleted from the database
     * @param authorizationManager       to be used for deleting ACL records
     * @param hardDelete                 specifies whether request should be really deleted or only marked as deleted
     */
    private void delete(AbstractReservationRequest abstractReservationRequest,
            AuthorizationManager authorizationManager, boolean hardDelete)
    {
        if (abstractReservationRequest instanceof ReservationRequest) {
            ReservationRequest reservationRequest = (ReservationRequest) abstractReservationRequest;

            // Keep reservation (it is deleted by scheduler)
            Reservation reservation = reservationRequest.getReservation();
            if (reservation != null) {
                // Check if reservation can be deleted
                ReservationManager reservationManager = new ReservationManager(entityManager);
                if (reservationManager.isProvided(reservation)) {
                    ControllerReportSetHelper.throwEntityNotDeletableReferencedFault(
                            ReservationRequest.class, reservationRequest.getId());
                }
                reservation.setReservationRequest(null);
                reservationManager.update(reservation);
            }
        }
        else if (abstractReservationRequest instanceof ReservationRequestSet) {
            ReservationRequestSet reservationRequestSet = (ReservationRequestSet) abstractReservationRequest;

            // Delete all reservation requests from set
            for (ReservationRequest reservationRequest : reservationRequestSet.getReservationRequests()) {
                delete(reservationRequest, authorizationManager, true);
            }

            // Clear state
            PreprocessorStateManager.clear(entityManager, reservationRequestSet);
        }

        if (hardDelete) {
            authorizationManager.deleteAclRecordsForEntity(abstractReservationRequest);

            super.delete(abstractReservationRequest);
        }
        else {
            abstractReservationRequest.setType(AbstractReservationRequest.Type.DELETED);
            super.update(abstractReservationRequest);
        }
    }

    /**
     * @param reservationRequestId
     * @return list of all versions for {@link AbstractReservationRequest} with given {@code reservationRequestId}
     */
    private List<AbstractReservationRequest> listVersions(Long reservationRequestId)
    {
        try {
            Long latestReservationRequestId = entityManager.createQuery(
                "SELECT modifiedReservationRequest.primaryKey.latestReservationRequest.id"
                        + "     FROM ModifiedReservationRequest modifiedReservationRequest"
                        + "    WHERE modifiedReservationRequest.primaryKey.oldReservationRequest.id = :id"
                        + "       OR modifiedReservationRequest.primaryKey.newReservationRequest.id = :id"
                        + " GROUP BY modifiedReservationRequest.primaryKey.latestReservationRequest.id", Long.class)
                .setParameter("id", reservationRequestId)
                .getSingleResult();
            // Multiple version exists, so find all of them
            return entityManager.createQuery(
                    "SELECT reservationRequest FROM AbstractReservationRequest reservationRequest"
                            + " WHERE reservationRequest.id = :id "
                            + "    OR reservationRequest.id IN("
                            + "       SELECT modified.primaryKey.oldReservationRequest"
                            + "          FROM ModifiedReservationRequest modified"
                            + "        WHERE modified.primaryKey.latestReservationRequest.id = :id"
                            + "    )",
                    AbstractReservationRequest.class)
                    .setParameter("id", latestReservationRequestId)
                    .getResultList();
        } catch (NoResultException exception) {
            // Only single version exists, so return only reservation request with given identifier
            List<AbstractReservationRequest> reservationRequests = new LinkedList<AbstractReservationRequest>();
            reservationRequests.add(get(reservationRequestId));
            return reservationRequests;
        }
    }

    /**
     * @param reservationRequest to be checked whether it has already been modified
     * @return true whether given {@code reservationRequest} is already modified,
     *         false otherwise
     */
    public boolean isModified(AbstractReservationRequest reservationRequest)
    {
        return entityManager.createQuery(
                "SELECT modifiedReservationRequest FROM ModifiedReservationRequest modifiedReservationRequest"
                        + " WHERE modifiedReservationRequest.primaryKey.oldReservationRequest = :reservationRequest")
                .setParameter("reservationRequest", reservationRequest)
                .getResultList().size() > 0;
    }

    /**
     * @param reservationRequestId of the {@link AbstractReservationRequest}
     * @return {@link AbstractReservationRequest} with given id
     * @throws CommonReportSet.EntityNotFoundException
     *          when the {@link AbstractReservationRequest} doesn't exist
     */
    public AbstractReservationRequest get(Long reservationRequestId) throws CommonReportSet.EntityNotFoundException
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
            return ControllerReportSetHelper
                    .throwEntityNotFoundFault(AbstractReservationRequest.class, reservationRequestId);
        }
    }

    /**
     * @param reservationRequestId of the {@link ReservationRequest}
     * @return {@link ReservationRequest} with given id
     * @throws CommonReportSet.EntityNotFoundException
     *          when the {@link ReservationRequest} doesn't exist
     */
    public ReservationRequest getReservationRequest(Long reservationRequestId)
            throws CommonReportSet.EntityNotFoundException
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
            return ControllerReportSetHelper.throwEntityNotFoundFault(ReservationRequest.class, reservationRequestId);
        }
    }

    /**
     * @param reservationRequestSetId of the {@link ReservationRequestSet}
     * @return {@link ReservationRequestSet} with given id
     * @throws CommonReportSet.EntityNotFoundException
     *          when the {@link ReservationRequestSet} doesn't exist
     */
    public ReservationRequestSet getReservationRequestSet(Long reservationRequestSetId)
            throws CommonReportSet.EntityNotFoundException
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
            return ControllerReportSetHelper
                    .throwEntityNotFoundFault(ReservationRequestSet.class, reservationRequestSetId);
        }
    }

    /**
     * @param ids                    requested identifiers
     * @param userId                 requested user
     * @param technologies           requested technologies
     * @param specificationClasses   set of classes for specifications which are allowed
     * @param providedReservationIds identifier of reservation which must be provided
     * @return list all reservation requests for given {@code owner} and {@code technologies} in the database.
     */
    public List<AbstractReservationRequest> list(Set<Long> ids, String userId, Set<Technology> technologies,
            Set<Class<? extends Specification>> specificationClasses, Set<Long> providedReservationIds)
    {
        DatabaseFilter filter = new DatabaseFilter("request");
        filter.addFilter("TYPE(request) != ReservationRequest OR request.reservationRequestSet IS NULL");
        filter.addFilter("request.type = :createdType", "createdType", AbstractReservationRequest.Type.CREATED);
        filter.addIds(ids);
        filter.addUserId(userId);
        if (technologies != null && technologies.size() > 0) {
            // List only reservation requests which specifies given technologies
            filter.addFilter("request IN ("
                    + "  SELECT reservationRequest"
                    + "  FROM AbstractReservationRequest reservationRequest"
                    + "  LEFT JOIN reservationRequest.specification specification"
                    + "  LEFT JOIN specification.technologies technology"
                    + "  WHERE technology IN(:technologies)"
                    + ")");
            filter.addFilterParameter("technologies", technologies);
        }
        if (specificationClasses != null && specificationClasses.size() > 0) {
            // List only reservation requests which specifies specification of given classes
            filter.addFilter("request IN ("
                    + "  SELECT reservationRequest"
                    + "  FROM AbstractReservationRequest reservationRequest"
                    + "  LEFT JOIN reservationRequest.specification reservationRequestSpecification"
                    + "  WHERE TYPE(reservationRequestSpecification) IN(:classes)"
                    + ")");
            filter.addFilterParameter("classes", specificationClasses);
        }
        if (providedReservationIds != null) {
            // List only reservation requests which got provided given reservation
            filter.addFilter("request IN ("
                    + "  SELECT reservationRequest"
                    + "  FROM AbstractReservationRequest reservationRequest"
                    + "  LEFT JOIN reservationRequest.providedReservations providedReservation"
                    + "  WHERE providedReservation.id IN (:providedReservationId)"
                    + ")");
            filter.addFilterParameter("providedReservationId", providedReservationIds);
        }
        TypedQuery<AbstractReservationRequest> query = entityManager.createQuery("SELECT request"
                + " FROM AbstractReservationRequest request"
                + " WHERE " + filter.toQueryWhere(),
                AbstractReservationRequest.class);
        filter.fillQueryParameters(query);
        List<AbstractReservationRequest> reservationRequestList = query.getResultList();
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
     * @return {@link ReservationRequest} with given id
     * @throws CommonReportSet.EntityNotFoundException
     *          when the {@link ReservationRequest} doesn't exist
     */
    public ReservationRequest getReservationRequestNotNull(Long reservationRequestId)
            throws CommonReportSet.EntityNotFoundException
    {
        return getReservationRequest(reservationRequestId);
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
     * @param personId      id for {@link cz.cesnet.shongo.controller.common.Person} for which the search is performed
     * @return {@link PersonSpecification} from given {@link Specification} that references {@link cz.cesnet.shongo.controller.common.Person}
     *         with given id if exists, null otherwise
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
     * @param personId           id for {@link cz.cesnet.shongo.controller.common.Person} for which the search is performed
     * @return {@link PersonSpecification} from given {@link ReservationRequest} that references {@link cz.cesnet.shongo.controller.common.Person}
     *         with given id
     * @throws RuntimeException when {@link PersonSpecification} isn't found
     */
    private PersonSpecification getPersonSpecification(ReservationRequest reservationRequest, Long personId)
    {
        Specification specification = reservationRequest.getSpecification();
        PersonSpecification personSpecification = getPersonSpecification(specification, personId);
        if (personSpecification == null) {
            throw new RuntimeException(
                    String.format("Requested person '%d' doesn't exist in specification '%d'!",
                            personId, specification.getId()));
        }
        return personSpecification;
    }

    /**
     * Accept the invitation for specified {@link cz.cesnet.shongo.controller.common.Person} to participate in the specified {@link ReservationRequest}.
     *
     * @param reservationRequestId id for {@link ReservationRequest}
     * @param personId             id for {@link cz.cesnet.shongo.controller.common.Person}
     * @throws RuntimeException when {@link cz.cesnet.shongo.controller.common.Person} hasn't selected resource by he will connect to
     *                          the video conference yet
     */
    public void acceptPersonRequest(Long reservationRequestId, Long personId)
    {
        ReservationRequest reservationRequest = getReservationRequestNotNull(reservationRequestId);
        PersonSpecification personSpecification = getPersonSpecification(reservationRequest, personId);
        if (personSpecification.getEndpointSpecification() == null) {
            throw new RuntimeException(
                    String.format("Cannot accept person '%d' to compartment request '%d' because person hasn't "
                            + "selected the device be which he will connect to the compartment yet!",
                            personId, reservationRequestId));
        }
        personSpecification.setInvitationState(PersonSpecification.InvitationState.ACCEPTED);
        reservationRequest.updateStateBySpecification();
        update(reservationRequest);
    }

    /**
     * Reject the invitation for specified {@link cz.cesnet.shongo.controller.common.Person} to participate in the specified {@link ReservationRequest}.
     *
     * @param reservationRequestId id for {@link ReservationRequest}
     * @param personId             id for {@link cz.cesnet.shongo.controller.common.Person}
     */
    public void rejectPersonRequest(Long reservationRequestId, Long personId)
    {
        ReservationRequest reservationRequest = getReservationRequestNotNull(reservationRequestId);
        PersonSpecification personSpecification = getPersonSpecification(reservationRequest, personId);
        personSpecification.setInvitationState(PersonSpecification.InvitationState.REJECTED);
        reservationRequest.updateStateBySpecification();
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
