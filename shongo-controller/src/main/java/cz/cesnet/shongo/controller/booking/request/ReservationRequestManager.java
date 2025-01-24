package cz.cesnet.shongo.controller.booking.request;

import com.fasterxml.jackson.databind.JsonNode;
import cz.cesnet.shongo.AbstractManager;
import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.controller.ControllerReportSetHelper;
import cz.cesnet.shongo.controller.api.TagType;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.booking.Allocation;
import cz.cesnet.shongo.controller.booking.compartment.CompartmentSpecification;
import cz.cesnet.shongo.controller.booking.participant.EndpointParticipant;
import cz.cesnet.shongo.controller.booking.participant.InvitedPersonParticipant;
import cz.cesnet.shongo.controller.booking.participant.AbstractParticipant;
import cz.cesnet.shongo.controller.booking.participant.PersonParticipant;
import cz.cesnet.shongo.controller.booking.request.auxdata.AuxDataFilter;
import cz.cesnet.shongo.controller.booking.request.auxdata.AuxDataMerged;
import cz.cesnet.shongo.controller.booking.request.auxdata.tagdata.TagData;
import cz.cesnet.shongo.controller.booking.specification.Specification;
import cz.cesnet.shongo.controller.booking.reservation.Reservation;
import cz.cesnet.shongo.controller.booking.reservation.ReservationManager;
import cz.cesnet.shongo.controller.scheduler.SchedulerReport;
import cz.cesnet.shongo.controller.util.NativeQuery;
import org.joda.time.Interval;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import java.util.*;
import java.util.stream.Collectors;

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
     * Updates table executable_summary, DO NOT USE directly, for more see {@link Specification#updateSpecificationSummary(EntityManager, boolean)}
     *
     * @param specification
     * @param deleteOnly
     */
    public void updateSpecificationSummary(Specification specification, boolean deleteOnly)
    {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("specification_id", specification.getId().toString());

        String deleteQuery = NativeQuery.getNativeQuery(NativeQuery.SPECIFICATION_SUMMARY_DELETE, parameters);
        entityManager.createNativeQuery(deleteQuery).executeUpdate();

        if (!deleteOnly) {
            String updateQuery = NativeQuery.getNativeQuery(NativeQuery.SPECIFICATION_SUMMARY_INSERT, parameters);
            entityManager.createNativeQuery(updateQuery).executeUpdate();
        }
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

        reservationRequest.getSpecification().updateSpecificationSummary(entityManager, false);
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

        entityManager.flush();
        oldReservationRequest.getSpecification().updateSpecificationSummary(entityManager, false, false);
        newReservationRequest.getSpecification().updateSpecificationSummary(entityManager, false, false);
    }

    /**
     * Mark as deleted existing {@link AbstractReservationRequest} and all it's versions in the database.
     *
     * @param reservationRequest   to be marked as deleted (and all it's versions)
     * @param authorizationManager to be used for deleting ACL entries
     */
    public void softDelete(AbstractReservationRequest reservationRequest, AuthorizationManager authorizationManager)
    {
        PersistenceTransaction transaction = beginPersistenceTransaction();

        Allocation allocation = reservationRequest.getAllocation();
        if (!deleteAllocation(allocation, reservationRequest.getUpdatedBy(), authorizationManager)) {
            ControllerReportSetHelper.throwObjectNotDeletableReferencedFault(
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
        delete(reservationRequest, false);

        reservationRequest.getSpecification().updateSpecificationSummary(entityManager, false);

        transaction.commit();
    }

    /**
     * Delete existing {@link AbstractReservationRequest} and all it's versions in the database.
     *
     * @param reservationRequest   to be deleted (and all it's versions)
     * @param authorizationManager to be used for deleting ACL entries
     * @return list of {@link Reservation}s which are detached from deleted {@code reservationRequest}'s {@link Allocation}
     */
    public List<Reservation> hardDelete(AbstractReservationRequest reservationRequest,
            AuthorizationManager authorizationManager)
    {
        PersistenceTransaction transaction = beginPersistenceTransaction();

        Allocation allocation = reservationRequest.getAllocation();
        if (!deleteAllocation(allocation, reservationRequest.getUpdatedBy(), authorizationManager)) {
            ControllerReportSetHelper.throwObjectNotDeletableReferencedFault(
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

        // Delete acl records
        authorizationManager.deleteAclEntriesForEntity(reservationRequest);

        List<AbstractReservationRequest> versions = listVersions(reservationRequest);
        for (AbstractReservationRequest version : versions) {
            delete(version, true);
        }
        allocation.setReservationRequest(null);

        entityManager.flush();
        // Update specification summary for each version AFTER everything else
        for (AbstractReservationRequest version : versions) {
            version.getSpecification().updateSpecificationSummary(entityManager, true, false);
        }

        transaction.commit();

        // Return detached reservations
        return detachedReservations;
    }

    /**
     * @param allocation           for which the {@link Allocation#reservations} should be deleted
     * @param userId               who deleted the allocation
     * @param authorizationManager to be used for deleting ACL entries
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
     * @param hardDelete                 specifies whether request should be really deleted or only marked as deleted
     */
    public void delete(AbstractReservationRequest abstractReservationRequest, boolean hardDelete)
    {
        // Clear preprocessor state
        PreprocessorStateManager.clear(entityManager, abstractReservationRequest);

        // Hard delete
        if (hardDelete) {
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
     * @throws cz.cesnet.shongo.CommonReportSet.ObjectNotExistsException
     *          when the {@link AbstractReservationRequest} doesn't exist
     */
    public AbstractReservationRequest get(Long reservationRequestId) throws CommonReportSet.ObjectNotExistsException
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
                    .throwObjectNotExistFault(AbstractReservationRequest.class, reservationRequestId);
        }
    }

    /**
     * @param reservationRequestId of the {@link ReservationRequest}
     * @return {@link ReservationRequest} with given id
     * @throws cz.cesnet.shongo.CommonReportSet.ObjectNotExistsException
     *          when the {@link ReservationRequest} doesn't exist
     */
    public ReservationRequest getReservationRequest(Long reservationRequestId)
            throws CommonReportSet.ObjectNotExistsException
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
            return ControllerReportSetHelper.throwObjectNotExistFault(ReservationRequest.class, reservationRequestId);
        }
    }

    /**
     * @param specification of the {@link AbstractReservationRequest}
     * @return {@link AbstractReservationRequest} with given {@code specification} or {@code null}
     */
    public AbstractReservationRequest getBySpecification(Specification specification)
            throws CommonReportSet.ObjectNotExistsException
    {
        try {
            return entityManager.createQuery(
                    "SELECT reservationRequest FROM AbstractReservationRequest reservationRequest"
                            + " WHERE reservationRequest.specification.id = :specificationId",
                    AbstractReservationRequest.class)
                    .setParameter("specificationId", specification.getId())
                    .getSingleResult();
        }
        catch (NoResultException exception) {
            return null;
        }
    }

    /**
     * @param reservationRequestSetId of the {@link ReservationRequestSet}
     * @return {@link ReservationRequestSet} with given id
     * @throws cz.cesnet.shongo.CommonReportSet.ObjectNotExistsException
     *          when the {@link ReservationRequestSet} doesn't exist
     */
    public ReservationRequestSet getReservationRequestSet(Long reservationRequestSetId)
            throws CommonReportSet.ObjectNotExistsException
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
            return ControllerReportSetHelper.throwObjectNotExistFault(
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
     * @return list of {@link ReservationRequest}s in {@link ReservationRequest.AllocationState#COMPLETE} state and
     *         starting in given {@code interval}
     */
    public List<ReservationRequest> listCompletedReservationRequests(Interval interval)
    {
        //TODO: IDP: when to process???
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
        Allocation allocation = reservationRequest.getAllocation();

        List<AbstractReservationRequest> reservationRequests = entityManager.createQuery(
                "SELECT reservationRequest FROM AbstractReservationRequest reservationRequest"
                        + " WHERE reservationRequest.state = :stateActive"
                        + " AND reservationRequest.reusedAllocation = :allocation",
                AbstractReservationRequest.class)
                .setParameter("stateActive", AbstractReservationRequest.State.ACTIVE)
                .setParameter("allocation", allocation)
                .getResultList();
        return reservationRequests;
    }

    /**
     * @param interval
     * @return list of {@link ReservationRequest}s which reuse given {@code allocation}, which are in
     *         {@link ReservationRequest.AllocationState#ALLOCATED} state and starting in given {@code interval}
     */
    public List<ReservationRequest> listAllocationActiveUsages(Allocation allocation, Interval interval)
    {
        List<ReservationRequest> reservationRequests = entityManager.createQuery(
                "SELECT reservationRequest FROM ReservationRequest reservationRequest"
                        + " WHERE reservationRequest.state = :activeState"
                        + " AND reservationRequest.allocationState = :allocationState"
                        + " AND reservationRequest.slotStart < :end"
                        + " AND reservationRequest.slotEnd > :start"
                        + " AND reservationRequest.reusedAllocation = :allocation",
                ReservationRequest.class)
                .setParameter("activeState", AbstractReservationRequest.State.ACTIVE)
                .setParameter("allocationState", ReservationRequest.AllocationState.ALLOCATED)
                .setParameter("start", interval.getStart())
                .setParameter("end", interval.getEnd())
                .setParameter("allocation", allocation)
                .getResultList();
        return reservationRequests;
    }

    /**
     * @return list of {@link ReservationRequest}s which should be deleted
     */
    public List<ReservationRequest> getOrphanReservationRequestsForDeletion()
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

    /**
     * Remove all {@link ReservationRequest#reports} and return them.
     *
     * @param reservationRequest
     * @return {@link ReservationRequest#reports}
     */
    public List<SchedulerReport> detachReports(ReservationRequest reservationRequest)
    {
        List<SchedulerReport> reports = new LinkedList<SchedulerReport>();
        for (SchedulerReport schedulerReport : reservationRequest.getReports()) {
            reports.add(entityManager.merge(schedulerReport));
        }
        reservationRequest.clearReports();
        return reports;
    }

    /**
     * Creates {@link TagData} for given {@link AbstractReservationRequest} and its corresponding
     * {@link cz.cesnet.shongo.controller.booking.resource.Tag}s.
     *
     * @param reservationRequest reservation request for which the {@link TagData} shall be created
     * @param filter             filter for data desired
     * @return specific implementation of {@link TagData} based on {@link TagType}
     * @param <T> TagData implementation for corresponding {@link TagType}
     */
    public <T extends TagData<?>> List<T> getTagData(AbstractReservationRequest reservationRequest, AuxDataFilter filter)
    {
        return getAuxData(reservationRequest, filter)
                .stream()
                .map(TagData::create)
                .map(data -> (T) data)
                .collect(Collectors.toList());
    }

    /**
     * Merge {@link cz.cesnet.shongo.controller.booking.request.auxdata.AuxData} from {@link AbstractReservationRequest}
     * and data from its corresponding {@link cz.cesnet.shongo.controller.booking.resource.Tag}s.
     *
     * @param reservationRequest reservation request for which the data shall be merged
     * @param filter             filter for data desired
     * @return merged data
     */
    private List<AuxDataMerged> getAuxData(AbstractReservationRequest reservationRequest, AuxDataFilter filter)
    {
        String queryString = "SELECT arr.tagName, rt.tag.type, arr.enabled, arr.data, rt.tag.data" +
                " FROM AbstractReservationRequestAuxData arr" +
                " JOIN ResourceSpecification res_spec ON res_spec.id = arr.specification.id" +
                " JOIN ResourceTag rt ON rt.resource.id = res_spec.resource.id" +
                " WHERE rt.tag.name = arr.tagName" +
                " AND arr.id = :id";
        if (filter.getTagName() != null) {
            queryString += " AND rt.tag.name = :tagName";
        }
        if (filter.getTagType() != null) {
            queryString += " AND rt.tag.type = :type";
        }
        if (filter.getEnabled() != null) {
            queryString += " AND arr.enabled = :enabled";
        }

        TypedQuery<Object[]> query = entityManager.createQuery(queryString, Object[].class)
                .setParameter("id", reservationRequest.getId());
        if (filter.getTagName() != null) {
            query.setParameter("tagName", filter.getTagName());
        }
        if (filter.getTagType() != null) {
            query.setParameter("type", filter.getTagType());
        }
        if (filter.getEnabled() != null) {
            query.setParameter("enabled", filter.getEnabled());
        }

        return query
                .getResultList()
                .stream()
                .map(record -> {
                    final String tagName = (String) record[0];
                    final TagType type = (TagType) record[1];
                    final Boolean enabled = (Boolean) record[2];
                    final JsonNode auxData = (JsonNode) record[3];
                    final JsonNode data = (JsonNode) record[4];
                    return new AuxDataMerged(tagName, type, enabled, data, auxData);
                })
                .collect(Collectors.toList());
    }
}
