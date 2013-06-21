package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.AbstractManager;
import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.ControllerReportSetHelper;
import cz.cesnet.shongo.controller.api.ReservationRequestType;
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
        Allocation allocation = new Allocation();
        allocation.setReservationRequest(abstractReservationRequest);
        abstractReservationRequest.setAllocation(allocation);
        abstractReservationRequest.validate();
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
        oldReservationRequest.setState(AbstractReservationRequest.State.MODIFIED);
        oldReservationRequest.validate();

        // Set new reservation request to allocation
        Allocation allocation = oldReservationRequest.getAllocation();
        allocation.setReservationRequest(newReservationRequest);

        // Create new reservation request
        newReservationRequest.setModifiedReservationRequest(oldReservationRequest);
        newReservationRequest.setAllocation(allocation);
        newReservationRequest.validate();
        super.create(newReservationRequest);
    }

    /**
     * Mark as deleted existing {@link AbstractReservationRequest} and all it's versions in the database.
     *
     * @param reservationRequest   to be marked as deleted (and all it's versions)
     * @param authorizationManager to be used for deleting ACL records
     */
    public void softDelete(AbstractReservationRequest reservationRequest, AuthorizationManager authorizationManager)
    {
        PersistenceTransaction transaction = beginPersistenceTransaction();

        if (!deleteAllocation(reservationRequest.getAllocation(), authorizationManager)) {
            ControllerReportSetHelper.throwEntityNotDeletableReferencedFault(
                    ReservationRequest.class, reservationRequest.getId());
        }

        delete(reservationRequest, authorizationManager, false);

        transaction.commit();
    }

    /**
     * Delete existing {@link AbstractReservationRequest} and all it's versions in the database.
     *
     * @param reservationRequest   to be deleted (and all it's versions)
     * @param authorizationManager to be used for deleting ACL records
     */
    public void hardDelete(AbstractReservationRequest reservationRequest, AuthorizationManager authorizationManager)
    {
        PersistenceTransaction transaction = beginPersistenceTransaction();

        if (!deleteAllocation(reservationRequest.getAllocation(), authorizationManager)) {
            ControllerReportSetHelper.throwEntityNotDeletableReferencedFault(
                    ReservationRequest.class, reservationRequest.getId());
        }

        List<AbstractReservationRequest> versions = listVersions(reservationRequest);
        for (AbstractReservationRequest version : versions) {
            delete(version, authorizationManager, true);
        }

        transaction.commit();
    }

    /**
     * @param allocation           for which the {@link Allocation#reservations} should be deleted
     * @param authorizationManager to be used for deleting ACL records
     */
    private boolean deleteAllocation(Allocation allocation, AuthorizationManager authorizationManager)
    {
        ReservationManager reservationManager = new ReservationManager(entityManager);

        // Detach reservations from allocation and they will be deleted by scheduler
        for (Reservation reservation : new LinkedList<Reservation>(allocation.getReservations())) {
            // Check if reservation can be deleted
            if (reservationManager.isProvided(reservation)) {
                return false;
            }
            reservation.setAllocation(null);
            reservationManager.update(reservation);
        }

        // Delete all child reservation requests
        for (ReservationRequest reservationRequest : allocation.getChildReservationRequests()) {
            hardDelete(reservationRequest, authorizationManager);
        }

        return true;
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
        // Clear preprocessor state
        PreprocessorStateManager.clear(entityManager, abstractReservationRequest);

        // Hard delete
        if (hardDelete) {
            authorizationManager.deleteAclRecordsForEntity(abstractReservationRequest);

            super.delete(abstractReservationRequest);
        }
        // Soft delete
        else {
            abstractReservationRequest.setState(AbstractReservationRequest.State.DELETED);

            // Clear allocation reports
            if (abstractReservationRequest instanceof ReservationRequest) {
                ReservationRequest reservationRequest = (ReservationRequest) abstractReservationRequest;
                reservationRequest.clearReports();
            }

            super.update(abstractReservationRequest);
        }
    }

    /**
     * @param reservationRequest
     * @return list of all versions for given {@code reservationRequest}
     */
    private List<AbstractReservationRequest> listVersions(AbstractReservationRequest reservationRequest)
    {
        return entityManager.createQuery("SELECT reservationRequest FROM AbstractReservationRequest reservationRequest"
                + " WHERE reservationRequest.allocation = :allocation", AbstractReservationRequest.class)
                .setParameter("allocation", reservationRequest.getAllocation())
                .getResultList();
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
            return ControllerReportSetHelper.throwEntityNotFoundFault(
                    ReservationRequestSet.class, reservationRequestSetId);
        }
    }

    /**
     * @return list all {@link ReservationRequestSet} which aren't preprocessed in given interval.
     */
    public List<ReservationRequestSet> listNotPreprocessedReservationRequestSets(Interval interval)
    {
        List<ReservationRequestSet> reservationRequestList = entityManager
                .createQuery("SELECT reservationRequest FROM ReservationRequestSet reservationRequest"
                        + " WHERE reservationRequest.state = :activeState AND reservationRequest NOT IN ("
                        + " SELECT state.reservationRequest FROM PreprocessedState state"
                        + " WHERE state.start <= :from AND state.end >= :to)",
                        ReservationRequestSet.class)
                .setParameter("activeState", AbstractReservationRequest.State.ACTIVE)
                .setParameter("from", interval.getStart())
                .setParameter("to", interval.getEnd())
                .getResultList();
        return reservationRequestList;
    }

    /**
     * @param interval
     * @return list of existing {@link ReservationRequest}s in {@link cz.cesnet.shongo.controller.request.ReservationRequest.AllocationState#COMPLETE} state and
     *         starting in given interval
     */
    public List<ReservationRequest> listCompletedReservationRequests(Interval interval)
    {
        List<ReservationRequest> compartmentRequestList = entityManager.createQuery(
                "SELECT reservationRequest FROM ReservationRequest reservationRequest"
                        + " WHERE reservationRequest.state = :activeState AND reservationRequest.allocationState = :allocationState"
                        + " AND reservationRequest.slotStart < :end"
                        + " AND reservationRequest.slotEnd > :start",
                ReservationRequest.class)
                .setParameter("activeState", AbstractReservationRequest.State.ACTIVE)
                .setParameter("allocationState", ReservationRequest.AllocationState.COMPLETE)
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
        ReservationRequest reservationRequest = getReservationRequest(reservationRequestId);
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
        ReservationRequest reservationRequest = getReservationRequest(reservationRequestId);
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
        ReservationRequest reservationRequest = getReservationRequest(reservationRequestId);
        PersonSpecification personSpecification = getPersonSpecification(reservationRequest, personId);

        CompartmentSpecification compartmentSpecification =
                (CompartmentSpecification) reservationRequest.getSpecification();
        if (!compartmentSpecification.containsSpecification(endpointSpecification)) {
            compartmentSpecification.addChildSpecification(endpointSpecification);
        }
        personSpecification.setEndpointSpecification(endpointSpecification);
        update(reservationRequest);
    }

    /**
     * @param parentReservationRequest for which the child {@link ReservationRequest}s should be returned
     * @return list of child {@link ReservationRequest}s for a given {@link AbstractReservationRequest}
     */
    public List<ReservationRequest> listChildReservationRequests(AbstractReservationRequest parentReservationRequest)
    {
        List<ReservationRequest> compartmentRequestList = entityManager.createQuery(
                "SELECT reservationRequest FROM ReservationRequest reservationRequest"
                        + "    WHERE reservationRequest.parentAllocation = :allocation"
                        + " ORDER BY reservationRequest.slotStart", ReservationRequest.class)
                .setParameter("allocation", parentReservationRequest.getAllocation())
                .getResultList();
        return compartmentRequestList;
    }

    /**
     * @param parentReservationRequest for which the child {@link ReservationRequest}s should be returned
     * @param interval                 in which the {@link ReservationRequest}s must take place
     * @return list of child {@link ReservationRequest}s for a given {@link AbstractReservationRequest} taking place in
     *         given {@code interval}
     */
    public List<ReservationRequest> listChildReservationRequests(AbstractReservationRequest parentReservationRequest,
            Interval interval)
    {
        List<ReservationRequest> reservationRequests = entityManager.createQuery(
                "SELECT reservationRequest FROM ReservationRequest reservationRequest"
                        + " WHERE reservationRequest.parentAllocation = :allocation"
                        + "   AND reservationRequest.slotStart < :end"
                        + "   AND reservationRequest.slotEnd > :start",
                ReservationRequest.class)
                .setParameter("allocation", parentReservationRequest.getAllocation())
                .setParameter("start", interval.getStart())
                .setParameter("end", interval.getEnd())
                .getResultList();
        return reservationRequests;
    }
}
