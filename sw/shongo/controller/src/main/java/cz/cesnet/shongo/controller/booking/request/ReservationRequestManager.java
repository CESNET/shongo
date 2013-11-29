package cz.cesnet.shongo.controller.booking.request;

import cz.cesnet.shongo.AbstractManager;
import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.controller.ControllerReportSetHelper;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.booking.Allocation;
import cz.cesnet.shongo.controller.booking.compartment.CompartmentSpecification;
import cz.cesnet.shongo.controller.booking.participant.EndpointParticipant;
import cz.cesnet.shongo.controller.booking.participant.InvitedPersonParticipant;
import cz.cesnet.shongo.controller.booking.participant.AbstractParticipant;
import cz.cesnet.shongo.controller.booking.participant.PersonParticipant;
import cz.cesnet.shongo.controller.booking.specification.Specification;
import cz.cesnet.shongo.controller.booking.reservation.Reservation;
import cz.cesnet.shongo.controller.booking.reservation.ReservationManager;
import org.joda.time.Interval;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
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

        Allocation allocation = reservationRequest.getAllocation();
        if (!deleteAllocation(allocation, reservationRequest.getUpdatedBy(), authorizationManager)) {
            ControllerReportSetHelper.throwEntityNotDeletableReferencedFault(
                    ReservationRequest.class, reservationRequest.getId());
        }

        // Clear allocation reports for all reservation request versions
        for (AbstractReservationRequest version : listVersions(reservationRequest)) {
            if (version instanceof ReservationRequest) {
                ReservationRequest reservationRequestVersion = (ReservationRequest) version;
                reservationRequestVersion.clearReports();
            }
        }

        // Soft delete the reservation request
        delete(reservationRequest, authorizationManager, false);

        transaction.commit();
    }

    /**
     * Delete existing {@link AbstractReservationRequest} and all it's versions in the database.
     *
     * @param reservationRequest   to be deleted (and all it's versions)
     * @param authorizationManager to be used for deleting ACL records
     * @return list of {@link Reservation}s which are detached from deleted {@code reservationRequest}'s {@link Allocation}
     */
    public List<Reservation> hardDelete(AbstractReservationRequest reservationRequest,
            AuthorizationManager authorizationManager)
    {
        PersistenceTransaction transaction = beginPersistenceTransaction();

        Allocation allocation = reservationRequest.getAllocation();
        if (!deleteAllocation(allocation, reservationRequest.getUpdatedBy(), authorizationManager)) {
            ControllerReportSetHelper.throwEntityNotDeletableReferencedFault(
                    ReservationRequest.class, reservationRequest.getId());
        }

        // Detach reservations from allocation which is going to be deleted
        ReservationManager reservationManager = new ReservationManager(entityManager);
        List<Reservation> detachedReservations = new LinkedList<Reservation>();
        for (Reservation reservation : new LinkedList<Reservation>(allocation.getReservations())) {
            reservation.setAllocation(null);
            reservationManager.update(reservation);
            detachedReservations.add(reservation);
        }

        List<AbstractReservationRequest> versions = listVersions(reservationRequest);
        for (AbstractReservationRequest version : versions) {
            delete(version, authorizationManager, true);
        }

        transaction.commit();

        // Return detached reservations
        return detachedReservations;
    }

    /**
     * @param allocation           for which the {@link Allocation#reservations} should be deleted
     * @param userId               who deleted the allocation
     * @param authorizationManager to be used for deleting ACL records
     */
    private boolean deleteAllocation(Allocation allocation, String userId, AuthorizationManager authorizationManager)
    {
        ReservationManager reservationManager = new ReservationManager(entityManager);

        // Check if reservations can be deleted
        if (reservationManager.isAllocationReused(allocation)) {
            return false;
        }

        // Mark allocation for deletion
        allocation.setState(Allocation.State.DELETED);

        // Delete all child reservation requests
        for (ReservationRequest childReservationRequest : new LinkedList<ReservationRequest>(
                allocation.getChildReservationRequests())) {
            // Remove child reservation request from parent
            childReservationRequest.setParentAllocation(null);

            // Hard delete child reservation request
            List<Reservation> detachedReservations = hardDelete(childReservationRequest, authorizationManager);

            // Attach reservations to allocation marked for deletion which were detached from hard deleted request
            for (Reservation detachedReservation : detachedReservations) {
                allocation.addReservation(detachedReservation);
            }
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
    public void delete(AbstractReservationRequest abstractReservationRequest,
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
     * @throws cz.cesnet.shongo.CommonReportSet.EntityNotExistsException
     *          when the {@link AbstractReservationRequest} doesn't exist
     */
    public AbstractReservationRequest get(Long reservationRequestId) throws CommonReportSet.EntityNotExistsException
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
                    .throwEntityNotExistFault(AbstractReservationRequest.class, reservationRequestId);
        }
    }

    /**
     * @param reservationRequestId of the {@link ReservationRequest}
     * @return {@link ReservationRequest} with given id
     * @throws cz.cesnet.shongo.CommonReportSet.EntityNotExistsException
     *          when the {@link ReservationRequest} doesn't exist
     */
    public ReservationRequest getReservationRequest(Long reservationRequestId)
            throws CommonReportSet.EntityNotExistsException
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
            return ControllerReportSetHelper.throwEntityNotExistFault(ReservationRequest.class, reservationRequestId);
        }
    }

    /**
     * @param reservationRequestSetId of the {@link ReservationRequestSet}
     * @return {@link ReservationRequestSet} with given id
     * @throws cz.cesnet.shongo.CommonReportSet.EntityNotExistsException
     *          when the {@link ReservationRequestSet} doesn't exist
     */
    public ReservationRequestSet getReservationRequestSet(Long reservationRequestSetId)
            throws CommonReportSet.EntityNotExistsException
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
            return ControllerReportSetHelper.throwEntityNotExistFault(
                    ReservationRequestSet.class, reservationRequestSetId);
        }
    }

    /**
     * @param allocationIds
     * @return list of reservation request identifier for given {@code allocationIds}
     */
    public Set<Long> listReservationRequestIdsByAllocations(Set<Long> allocationIds)
    {
        return null;  //To change body of created methods use File | Settings | File Templates.
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
     * @return list of {@link ReservationRequest}s in {@link ReservationRequest.AllocationState#COMPLETE} state and
     *         starting in given {@code interval}
     */
    public List<ReservationRequest> listCompletedReservationRequests(Interval interval)
    {
        List<ReservationRequest> reservationRequests = entityManager.createQuery(
                "SELECT reservationRequest FROM ReservationRequest reservationRequest"
                        + " WHERE reservationRequest.state = :activeState"
                        + " AND reservationRequest.allocationState = :allocationState"
                        + " AND reservationRequest.slotStart < :end"
                        + " AND reservationRequest.slotEnd > :start",
                ReservationRequest.class)
                .setParameter("activeState", AbstractReservationRequest.State.ACTIVE)
                .setParameter("allocationState", ReservationRequest.AllocationState.COMPLETE)
                .setParameter("start", interval.getStart())
                .setParameter("end", interval.getEnd())
                .getResultList();
        return reservationRequests;
    }

    /**
     * @param reservationRequest
     * @return list of {@link AbstractReservationRequest}s which reuse given {@code reservationRequest}
     */
    public List<AbstractReservationRequest> listReservationRequestActiveUsages(
            AbstractReservationRequest reservationRequest)
    {
        List<AbstractReservationRequest> reservationRequests = entityManager.createQuery(
                "SELECT reservationRequest FROM AbstractReservationRequest reservationRequest"
                        + " WHERE reservationRequest.reusedAllocation = :allocation"
                        + " AND reservationRequest.state = :stateActive",
                AbstractReservationRequest.class)
                .setParameter("allocation", reservationRequest.getAllocation())
                .setParameter("stateActive", AbstractReservationRequest.State.ACTIVE)
                .getResultList();
        return reservationRequests;
    }

    /**
     * @param interval
     * @return list of {@link ReservationRequest}s which reuse given {@code allocation}, which are in
     *         {@link ReservationRequest.AllocationState#ALLOCATED} state and starting in given {@code interval}
     */
    public List<ReservationRequest> listAllocationUsages(Allocation allocation, Interval interval)
    {
        List<ReservationRequest> reservationRequests = entityManager.createQuery(
                "SELECT reservationRequest FROM ReservationRequest reservationRequest"
                        + " WHERE reservationRequest.reusedAllocation = :reusableAllocation"
                        + " AND reservationRequest.state = :activeState"
                        + " AND reservationRequest.allocationState = :allocationState"
                        + " AND reservationRequest.slotStart < :end"
                        + " AND reservationRequest.slotEnd > :start",
                ReservationRequest.class)
                .setParameter("reusableAllocation", allocation)
                .setParameter("activeState", AbstractReservationRequest.State.ACTIVE)
                .setParameter("allocationState", ReservationRequest.AllocationState.ALLOCATED)
                .setParameter("start", interval.getStart())
                .setParameter("end", interval.getEnd())
                .getResultList();
        return reservationRequests;
    }

    /**
     * @return list of {@link ReservationRequest}s which should be deleted
     */
    public List<ReservationRequest> getReservationRequestsForDeletion()
    {
        return entityManager.createQuery(
                "SELECT reservationRequest FROM ReservationRequest reservationRequest"
                        + " LEFT JOIN reservationRequest.parentAllocation parentAllocation"
                        + " WHERE parentAllocation.state = :stateDeleted"
                        + " OR parentAllocation.state = :stateWithoutChildRequests",
                ReservationRequest.class)
                .setParameter("stateDeleted", Allocation.State.DELETED)
                .setParameter("stateWithoutChildRequests", Allocation.State.ACTIVE_WITHOUT_CHILD_RESERVATION_REQUESTS)
                .getResultList();
    }

    /**
     * @return list of {@link Allocation}s which should be deleted
     */
    public List<Allocation> getAllocationsForDeletion()
    {
        return entityManager.createQuery(
                "SELECT allocation FROM Allocation allocation"
                        + " WHERE allocation.state = :stateDeleted AND allocation NOT IN ("
                        + " SELECT reservationRequest.allocation FROM AbstractReservationRequest reservationRequest)",
                Allocation.class)
                .setParameter("stateDeleted", Allocation.State.DELETED)
                .getResultList();
    }

    /**
     * @param specification {@link cz.cesnet.shongo.controller.booking.specification.Specification} which is searched
     * @param personId      id for {@link cz.cesnet.shongo.controller.booking.person.AbstractPerson} for which the search is performed
     * @return {@link cz.cesnet.shongo.controller.booking.participant.InvitedPersonParticipant} from given {@link cz.cesnet.shongo.controller.booking.specification.Specification} that references
     *         {@link cz.cesnet.shongo.controller.booking.person.AbstractPerson} with given id if exists, null otherwise
     */
    private InvitedPersonParticipant getInvitedPersonParticipant(Specification specification, Long personId)
            throws IllegalArgumentException
    {
        if (specification instanceof CompartmentSpecification) {
            CompartmentSpecification compartmentSpecification = (CompartmentSpecification) specification;
            for (AbstractParticipant participant : compartmentSpecification.getParticipants()) {
                if (participant instanceof InvitedPersonParticipant) {
                    InvitedPersonParticipant invitedPersonParticipant = (InvitedPersonParticipant) participant;
                    if (invitedPersonParticipant.getPerson().getId().equals(personId)) {
                        return (InvitedPersonParticipant) participant;
                    }
                }
            }
        }
        return null;
    }

    /**
     * @param reservationRequest {@link ReservationRequest} which is searched
     * @param personId           id for {@link cz.cesnet.shongo.controller.booking.person.AbstractPerson} for which the search is performed
     * @return {@link PersonParticipant} from given {@link ReservationRequest} that references
     *         {@link cz.cesnet.shongo.controller.booking.person.AbstractPerson} with given id
     * @throws RuntimeException when {@link PersonParticipant} isn't found
     */
    private InvitedPersonParticipant getInvitedPersonParticipant(ReservationRequest reservationRequest, Long personId)
    {
        Specification specification = reservationRequest.getSpecification();
        InvitedPersonParticipant invitedPersonParticipant = getInvitedPersonParticipant(specification, personId);
        if (invitedPersonParticipant == null) {
            throw new RuntimeException(
                    String.format("Requested person '%d' doesn't exist in specification '%d'!",
                            personId, specification.getId()));
        }
        return invitedPersonParticipant;
    }

    /**
     * Accept the invitation for specified {@link cz.cesnet.shongo.controller.booking.person.AbstractPerson} to participate in the specified {@link ReservationRequest}.
     *
     * @param reservationRequestId id for {@link ReservationRequest}
     * @param personId             id for {@link cz.cesnet.shongo.controller.booking.person.AbstractPerson}
     * @throws RuntimeException when {@link cz.cesnet.shongo.controller.booking.person.AbstractPerson} hasn't selected resource by he will connect to
     *                          the video conference yet
     */
    public void acceptInvitedPersonParticipant(Long reservationRequestId, Long personId)
    {
        ReservationRequest reservationRequest = getReservationRequest(reservationRequestId);
        InvitedPersonParticipant invitedPersonParticipant = getInvitedPersonParticipant(reservationRequest, personId);
        if (invitedPersonParticipant.getEndpointParticipant() == null) {
            throw new RuntimeException(
                    String.format("Cannot accept person '%d' to compartment request '%d' because person hasn't "
                            + "selected the device be which he will connect to the compartment yet!",
                            personId, reservationRequestId));
        }
        invitedPersonParticipant.setInvitationState(InvitedPersonParticipant.InvitationState.ACCEPTED);
        reservationRequest.updateStateBySpecification();
        update(reservationRequest);
    }

    /**
     * Reject the invitation for specified {@link cz.cesnet.shongo.controller.booking.person.AbstractPerson} to participate in the specified {@link ReservationRequest}.
     *
     * @param reservationRequestId id for {@link ReservationRequest}
     * @param personId             id for {@link cz.cesnet.shongo.controller.booking.person.AbstractPerson}
     */
    public void rejectInvitedPersonParticipant(Long reservationRequestId, Long personId)
    {
        ReservationRequest reservationRequest = getReservationRequest(reservationRequestId);
        InvitedPersonParticipant invitedPersonParticipant = getInvitedPersonParticipant(reservationRequest, personId);
        invitedPersonParticipant.setInvitationState(InvitedPersonParticipant.InvitationState.REJECTED);
        reservationRequest.updateStateBySpecification();
        update(reservationRequest);
    }

    /**
     * @param reservationRequestId
     * @param personId
     * @param endpointParticipant
     */
    public void selectEndpointForInvitedPersonParticipant(Long reservationRequestId, Long personId,
            EndpointParticipant endpointParticipant)
    {
        ReservationRequest reservationRequest = getReservationRequest(reservationRequestId);
        InvitedPersonParticipant invitedPersonParticipant = getInvitedPersonParticipant(reservationRequest, personId);

        CompartmentSpecification compartmentSpecification =
                (CompartmentSpecification) reservationRequest.getSpecification();
        if (!compartmentSpecification.containsParticipant(endpointParticipant)) {
            compartmentSpecification.addParticipant(endpointParticipant);
        }
        invitedPersonParticipant.setEndpointParticipant(endpointParticipant);
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
