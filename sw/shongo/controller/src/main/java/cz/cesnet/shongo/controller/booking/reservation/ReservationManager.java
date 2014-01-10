package cz.cesnet.shongo.controller.booking.reservation;

import cz.cesnet.shongo.AbstractManager;
import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.Temporal;
import cz.cesnet.shongo.controller.ControllerReportSetHelper;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.booking.executable.ExecutableService;
import cz.cesnet.shongo.controller.booking.recording.RecordingCapability;
import cz.cesnet.shongo.controller.booking.recording.RecordingServiceReservation;
import cz.cesnet.shongo.controller.booking.resource.ResourceReservation;
import cz.cesnet.shongo.controller.booking.room.RoomEndpoint;
import cz.cesnet.shongo.controller.booking.room.RoomReservation;
import cz.cesnet.shongo.controller.booking.value.ValueReservation;
import cz.cesnet.shongo.controller.booking.executable.Executable;
import cz.cesnet.shongo.controller.booking.executable.ExecutableManager;
import cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.booking.Allocation;
import cz.cesnet.shongo.controller.booking.request.ReservationRequest;
import cz.cesnet.shongo.controller.booking.resource.Resource;
import cz.cesnet.shongo.controller.booking.room.RoomProviderCapability;
import cz.cesnet.shongo.controller.booking.value.provider.ValueProvider;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import java.util.*;

/**
 * Manager for {@link Reservation}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationManager extends AbstractManager
{
    /**
     * @param entityManager sets the {@link #entityManager}
     */
    public ReservationManager(EntityManager entityManager)
    {
        super(entityManager);
    }

    /**
     * @param reservation to be created in the database
     */
    public void create(Reservation reservation)
    {
        super.create(reservation);
    }

    /**
     * @param reservation to be updated in the database
     */
    public void update(Reservation reservation)
    {
        super.update(reservation);
    }

    /**
     * @param reservation to be deleted in the database
     */
    public synchronized void delete(Reservation reservation, AuthorizationManager authorizationManager)
    {
        // Get all reservations
        Collection<Reservation> reservations = new LinkedList<Reservation>();
        getAllReservations(reservation, reservations);
        // Disconnect reservations from parents (this operation is required because value reservations should be
        // deleted in the end and not automatically by cascade)
        for (Reservation reservationItem : reservations) {
            reservationItem.setParentReservation(null);
        }

        // Get ACL entries for deletion
        for (Reservation reservationToDelete : reservations) {
            authorizationManager.deleteAclEntriesForEntity(reservationToDelete);
        }

        // Date/time now for stopping executables
        DateTime dateTimeNow = Temporal.nowRounded();
        // Stop all executables
        stopReservationExecutables(reservation, dateTimeNow);

        // Delete all reservations
        for (Reservation reservationToDelete : reservations) {
            // Delete reservation
            super.delete(reservationToDelete);
        }
    }

    /**
     * Prepare for stopping all executables from given {@code reservation} and all child reservations.
     *
     * @param reservation
     * @param dateTimeNow
     */
    private void stopReservationExecutables(Reservation reservation, DateTime dateTimeNow)
    {
        // Process current reservation
        Executable executable = reservation.getExecutable();
        if (executable != null) {
            if (executable.getSlot().contains(dateTimeNow)) {
                executable.setSlotEnd(dateTimeNow);
                for (ExecutableService service : executable.getServices()) {
                    if (service.getSlotEnd().isAfter(dateTimeNow)) {
                        service.setSlotEnd(dateTimeNow);
                    }
                }
                ExecutableManager executableManager = new ExecutableManager(entityManager);
                executableManager.update(executable);
            }
        }
        // Process all child reservations
        for (Reservation childReservation : reservation.getChildReservations()) {
            stopReservationExecutables(childReservation, dateTimeNow);
        }
    }

    /**
     * @param reservationId of the {@link Reservation}
     * @return {@link Reservation} with given id
     * @throws cz.cesnet.shongo.CommonReportSet.ObjectNotExistsException
     *          when the {@link Reservation} doesn't exist
     */
    public Reservation get(Long reservationId) throws CommonReportSet.ObjectNotExistsException
    {
        try {
            Reservation reservation = entityManager.createQuery(
                    "SELECT reservation FROM Reservation reservation"
                            + " WHERE reservation.id = :id",
                    Reservation.class).setParameter("id", reservationId)
                    .getSingleResult();
            return reservation;
        }
        catch (NoResultException exception) {
            return ControllerReportSetHelper.throwObjectNotExistFault(Reservation.class, reservationId);
        }
    }

    /**
     * @param interval        in which the requested {@link Reservation}s should start
     * @param reservationType type of requested {@link Reservation}s
     * @return list of {@link Reservation}s starting in given {@code interval}
     */
    public <R extends Reservation> List<R> listByInterval(Interval interval, Class<R> reservationType)
    {
        List<R> reservations = entityManager.createQuery(
                "SELECT reservation FROM " + reservationType.getSimpleName() + " reservation"
                        + " WHERE reservation.slotStart BETWEEN :start AND :end",
                reservationType)
                .setParameter("start", interval.getStart())
                .setParameter("end", interval.getEnd())
                .getResultList();
        return reservations;
    }

    /**
     * Get list of {@link ExistingReservation} which reuse the given {@code reusedReservation}
     * in given {@code interval}.
     *
     * @param reusedReservation which must be referenced in the {@link ExistingReservation#reusedReservation}
     * @param interval
     * @return list of {@link ExistingReservation} which reuse the given {@code reusedReservation}
     */
    public List<ExistingReservation> getExistingReservations(Reservation reusedReservation, Interval interval)
    {
        return entityManager.createQuery(
                "SELECT reservation FROM ExistingReservation reservation"
                        + " WHERE reservation.reusedReservation = :reusedReservation"
                        + " AND reservation.slotStart < :end"
                        + " AND reservation.slotEnd > :start",
                ExistingReservation.class)
                .setParameter("reusedReservation", reusedReservation)
                .setParameter("start", interval.getStart())
                .setParameter("end", interval.getEnd())
                .getResultList();
    }

    /**
     * Get list of {@link ResourceReservation} for given {@code resource} in given {@code interval}.
     *
     * @param resource
     * @param interval
     * @return list of {@link ResourceReservation} for given {@code resource}
     */
    public List<ResourceReservation> getResourceReservations(Resource resource, Interval interval)
    {
        return entityManager.createQuery(
                "SELECT reservation FROM ResourceReservation reservation"
                        + " WHERE reservation.resource = :resource"
                        + " AND reservation.slotStart < :end"
                        + " AND reservation.slotEnd > :start",
                ResourceReservation.class)
                .setParameter("resource", resource)
                .setParameter("start", interval.getStart())
                .setParameter("end", interval.getEnd())
                .getResultList();
    }

    /**
     * @param roomProviderCapability
     * @param interval
     * @return list of {@link RoomReservation}s for given {@code roomProviderCapability} in given {@code interval}
     */
    public List<RoomReservation> getRoomReservations(RoomProviderCapability roomProviderCapability, Interval interval)
    {
        return entityManager.createQuery(
                "SELECT reservation FROM RoomReservation reservation"
                        + " WHERE reservation.roomProviderCapability = :roomProviderCapability"
                        + " AND reservation.slotStart < :end"
                        + " AND reservation.slotEnd > :start",
                RoomReservation.class)
                .setParameter("roomProviderCapability", roomProviderCapability)
                .setParameter("start", interval.getStart())
                .setParameter("end", interval.getEnd())
                .getResultList();
    }

    public List<RoomReservation> getRoomReservationsByReusedRoomEndpoint(RoomEndpoint roomEndpoint, Interval interval)
    {
        return entityManager.createQuery(
                "SELECT reservation FROM RoomReservation reservation"
                        + " WHERE reservation.executable IN("
                        + "   SELECT usedRoomEndpoint FROM UsedRoomEndpoint usedRoomEndpoint"
                        + "   WHERE usedRoomEndpoint.reusedRoomEndpoint = :roomEndpoint"
                        + " ) AND reservation.slotStart < :end"
                        + " AND reservation.slotEnd > :start",
                RoomReservation.class)
                .setParameter("roomEndpoint", roomEndpoint)
                .setParameter("start", interval.getStart())
                .setParameter("end", interval.getEnd())
                .getResultList();
    }

    /**
     * @param recordingCapability
     * @param interval
     * @return list of {@link RecordingServiceReservation}s for given {@code recordingCapability} in given {@code interval}
     */
    public List<RecordingServiceReservation> getRecordingServiceReservations(RecordingCapability recordingCapability,
            Interval interval)
    {
        return entityManager.createQuery(
                "SELECT reservation FROM RecordingServiceReservation reservation"
                        + " WHERE reservation.recordingCapability = :recordingCapability"
                        + " AND reservation.slotStart < :end"
                        + " AND reservation.slotEnd > :start",
                RecordingServiceReservation.class)
                .setParameter("recordingCapability", recordingCapability)
                .setParameter("start", interval.getStart())
                .setParameter("end", interval.getEnd())
                .getResultList();
    }

    /**
     * Get list of {@link cz.cesnet.shongo.controller.booking.value.ValueReservation} for given {@code valueProvider} in given {@code interval}.
     *
     * @param valueProvider
     * @param interval
     * @return list of {@link cz.cesnet.shongo.controller.booking.value.ValueReservation} for given {@code valueProvider}
     */
    public List<ValueReservation> getValueReservations(ValueProvider valueProvider, Interval interval)
    {
        return entityManager.createQuery(
                "SELECT reservation FROM ValueReservation reservation"
                        + " WHERE reservation.valueProvider = :valueProvider"
                        + " AND reservation.slotStart < :end"
                        + " AND reservation.slotEnd > :start",
                ValueReservation.class)
                .setParameter("valueProvider", valueProvider)
                .setParameter("start", interval.getStart())
                .setParameter("end", interval.getEnd())
                .getResultList();
    }

    /**
     * @return list of {@link Reservation}s which should be deleted
     */
    public List<Reservation> getReservationsForDeletion()
    {
        return entityManager.createQuery(
                "SELECT reservation FROM Reservation reservation"
                        + " LEFT JOIN reservation.allocation allocation"
                        + " WHERE reservation.parentReservation IS NULL"
                        + " AND (allocation IS NULL OR"
                        + "      allocation.state = :stateDeleted OR"
                        + "      allocation.state = :stateWithoutReservations)",
                Reservation.class)
                .setParameter("stateDeleted", Allocation.State.DELETED)
                .setParameter("stateWithoutReservations", Allocation.State.ACTIVE_WITHOUT_RESERVATIONS)
                .getResultList();
    }

    /**
     * @param allocation to be checked if it is reused by any {@link ReservationRequest}
     * @return true if given {@code allocation} is reused by any {@link ReservationRequest},
     *         false otherwise
     */
    public boolean isAllocationReused(Allocation allocation)
    {
        return getReservationRequestWithReusedAllocation(allocation).size() > 0;
    }

    /**
     * @param allocation for which the {@link AbstractReservationRequest}s should be returned
     * @return collection of {@link AbstractReservationRequest}s which reuse given {@code allocation}
     */
    public Collection<AbstractReservationRequest> getReservationRequestWithReusedAllocation(Allocation allocation)
    {
        Set<AbstractReservationRequest> reservationRequests = new HashSet<AbstractReservationRequest>();

        // Add top reservation requests by reused allocation
        List<AbstractReservationRequest> reservationRequestsWithReusedAllocation = entityManager.createQuery(
                "SELECT reservationRequest FROM AbstractReservationRequest reservationRequest"
                        + " WHERE reservationRequest.reusedAllocation = :allocation",
                AbstractReservationRequest.class)
                .setParameter("allocation", allocation)
                .getResultList();
        for (AbstractReservationRequest reservationRequest : reservationRequestsWithReusedAllocation) {
            if (!reservationRequest.getState().equals(AbstractReservationRequest.State.ACTIVE)) {
                continue;
            }
            if (reservationRequest instanceof ReservationRequest) {
                Allocation parentAllocation = ((ReservationRequest) reservationRequest).getParentAllocation();
                if (parentAllocation != null) {
                    reservationRequest = parentAllocation.getReservationRequest();
                }
            }
            reservationRequests.add(reservationRequest);
        }

        // Get all reservations
        Collection<Reservation> reservations = new LinkedList<Reservation>();
        for (Reservation reservation : allocation.getReservations()) {
            getAllReservations(reservation, reservations);
        }
        if (reservations.size() > 0) {
            // Add top reservation requests by existing reservations
            List<ExistingReservation> existingReservations = entityManager.createQuery(
                    "SELECT reservation FROM ExistingReservation reservation"
                            + " WHERE reservation.reusedReservation IN(:reservations)", ExistingReservation.class)
                    .setParameter("reservations", reservations)
                    .getResultList();
            for (ExistingReservation reservation : existingReservations) {
                AbstractReservationRequest reservationRequest = reservation.getTopReservationRequest();
                if (reservationRequest != null && !reservationRequest.getState().equals(AbstractReservationRequest.State.DELETED)) {
                    reservationRequests.add(reservationRequest);
                }
            }
        }

        return reservationRequests;
    }

    /**
     * Get all reservations recursive.
     *
     * @param reservation  current reservation
     * @param reservations collection of all {@link Reservation}s with children
     */
    private static void getAllReservations(Reservation reservation, Collection<Reservation> reservations)
    {
        reservations.add(reservation);

        for (Reservation childReservation : reservation.getChildReservations()) {
            getAllReservations(childReservation, reservations);
        }
    }
}
