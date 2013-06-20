package cz.cesnet.shongo.controller.reservation;

import cz.cesnet.shongo.*;
import cz.cesnet.shongo.controller.ControllerReportSetHelper;
import cz.cesnet.shongo.controller.ReservationRequestType;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.executor.Executable;
import cz.cesnet.shongo.controller.executor.ExecutableManager;
import cz.cesnet.shongo.controller.request.ReservationRequest;
import cz.cesnet.shongo.controller.resource.Resource;
import cz.cesnet.shongo.controller.resource.RoomProviderCapability;
import cz.cesnet.shongo.controller.resource.value.ValueProvider;
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
     * @param reservation reservation and all child reservations (recursive) to have date/time slot end updated
     * @param slotEnd     new date/time slot end
     */
    public void updateReservationSlotEnd(Reservation reservation, DateTime slotEnd)
    {
        reservation.setSlotEnd(slotEnd);

        // Update executable
        Executable executable = reservation.getExecutable();
        if (executable != null) {
            updateExecutableSlotEnd(executable, slotEnd);
        }

        // Update child reservations
        for (Reservation childReservation : reservation.getChildReservations()) {
            updateReservationSlotEnd(childReservation, slotEnd);
        }
    }

    /**
     * @param executable executable and all child executables (recursive) to have date/time slot end updated
     * @param slotEnd    new date/time slot end
     */
    private void updateExecutableSlotEnd(Executable executable, DateTime slotEnd)
    {
        executable.setSlotEnd(slotEnd);

        // Update child executables
        for (Executable childExecutable : executable.getChildExecutables()) {
            updateExecutableSlotEnd(childExecutable, slotEnd);
        }
    }

    /**
     * Get all reservations recursive and remove the child reservations from it's parents.
     * <p/>
     * This operation is required because value reservations should be deleted in the end
     * and not automatically by cascade.
     *
     * @param reservation  current reservation
     * @param reservations collection of all {@link Reservation}s with children
     */
    private void getChildReservations(Reservation reservation, Collection<Reservation> reservations)
    {
        reservations.add(reservation);

        List<Reservation> childReservations = reservation.getChildReservations();
        while (childReservations.size() > 0) {
            Reservation childReservation = childReservations.get(0);
            getChildReservations(childReservation, reservations);
            childReservation.setParentReservation(null);
        }
    }

    /**
     * @param reservation to be deleted in the database
     */
    public synchronized void delete(Reservation reservation, AuthorizationManager authorizationManager)
    {
        // Get all reservations and disconnect them from parents
        Collection<Reservation> reservations = new LinkedList<Reservation>();
        getChildReservations(reservation, reservations);

        // Get ACL records for deletion
        for (Reservation reservationToDelete : reservations) {
            authorizationManager.deleteAclRecordsForEntity(reservationToDelete);
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
                updateExecutableSlotEnd(executable, dateTimeNow);
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
     * @throws CommonReportSet.EntityNotFoundException
     *          when the {@link Reservation} doesn't exist
     */
    public Reservation get(Long reservationId) throws CommonReportSet.EntityNotFoundException
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
            return ControllerReportSetHelper.throwEntityNotFoundFault(Reservation.class, reservationId);
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
     * Get list of reused {@link Reservation}s. Reused {@link Reservation} is a {@link Reservation} which is referenced
     * by at least one {@link ExistingReservation} in the {@link ExistingReservation#reservation} attribute.
     *
     * @param referencedDateTime ignore all reservations which ends before the specified date/time
     * @return list of reused {@link Reservation}.
     */
    public List<Reservation> getReusedReservations(DateTime referencedDateTime)
    {
        List<Reservation> reservations = entityManager.createQuery(
                "SELECT DISTINCT reusedReservation FROM ExistingReservation reservation"
                        + " LEFT JOIN reservation.reservation reusedReservation"
                        + " WHERE reusedReservation.slotEnd > dateTime", Reservation.class)
                .setParameter("dateTime", referencedDateTime)
                .getResultList();
        return reservations;
    }

    /**
     * Get list of {@link ExistingReservation} which reuse the given {@code reusedReservation}
     * in given {@code interval}.
     *
     * @param reusedReservation which must be referenced in the {@link ExistingReservation#reservation}
     * @param interval
     * @return list of {@link ExistingReservation} which reuse the given {@code reusedReservation}
     */
    public List<ExistingReservation> getExistingReservations(Reservation reusedReservation, Interval interval)
    {
        return entityManager.createQuery(
                "SELECT reservation FROM ExistingReservation reservation"
                        + " WHERE reservation.reservation = :reusedReservation"
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
     * Get list of {@link RoomReservation} for given {@code roomProviderCapability} in given {@code interval}.
     *
     * @param roomProviderCapability
     * @param interval
     * @return list of {@link RoomReservation} for given {@code resource}
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

    /**
     * Get list of {@link ValueReservation} for given {@code valueProvider} in given {@code interval}.
     *
     * @param valueProvider
     * @param interval
     * @return list of {@link ValueReservation} for given {@code valueProvider}
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
     * Delete {@link Reservation}s which aren't allocated for any {@link ReservationRequest}.
     *
     * @return list of deleted {@link Reservation}
     */
    public List<Reservation> getReservationsForDeletion()
    {
        return entityManager.createQuery(
                "SELECT reservation FROM Reservation reservation"
                        + " LEFT JOIN reservation.allocation allocation"
                        + " WHERE reservation.createdBy = :createdBy"
                        + " AND reservation.parentReservation IS NULL"
                        + " AND (allocation IS NULL)",
                Reservation.class)
                .setParameter("createdBy", Reservation.CreatedBy.CONTROLLER)
                .getResultList();
    }

    /**
     * @param reservation to be checked if it is provided to any {@link ReservationRequest} or {@link Reservation}
     * @return true if given {@code reservation} is provided to any other {@link ReservationRequest},
     *         false otherwise
     */
    public boolean isProvided(Reservation reservation)
    {
        // Checks whether reservation isn't referenced in existing reservations
        List reservations = entityManager.createQuery(
                "SELECT reservation.id FROM ExistingReservation reservation"
                        + " WHERE reservation.reservation = :reservation")
                .setParameter("reservation", reservation)
                .getResultList();
        if (reservations.size() > 0) {
            return true;
        }
        // Checks whether reservation isn't referenced in existing reservation requests
        List reservationRequests = entityManager.createQuery(
                "SELECT reservationRequest.id FROM AbstractReservationRequest reservationRequest"
                        + " WHERE reservationRequest.type = :createdType"
                        + "   AND :reservation MEMBER OF reservationRequest.providedReservations")
                .setParameter("reservation", reservation)
                .setParameter("createdType", ReservationRequestType.CREATED)
                .getResultList();
        if (reservationRequests.size() > 0) {
            return true;
        }
        return false;
    }
}
