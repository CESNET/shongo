package cz.cesnet.shongo.controller.reservation;

import cz.cesnet.shongo.AbstractManager;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.Cache;
import cz.cesnet.shongo.controller.ControllerFaultSet;
import cz.cesnet.shongo.controller.authorization.AclRecord;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.executor.Executable;
import cz.cesnet.shongo.controller.executor.ExecutableManager;
import cz.cesnet.shongo.controller.request.ReservationRequest;
import cz.cesnet.shongo.controller.resource.Alias;
import cz.cesnet.shongo.controller.resource.Resource;
import cz.cesnet.shongo.controller.resource.value.ValueProvider;
import cz.cesnet.shongo.controller.util.DatabaseFilter;
import cz.cesnet.shongo.fault.FaultException;
import cz.cesnet.shongo.fault.TodoImplementException;
import org.joda.time.DateTime;
import org.joda.time.DateTimeFieldType;
import org.joda.time.Interval;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
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
     * @return {@link AclRecord}s which should be deleted
     */
    public Collection<AclRecord> delete(Reservation reservation, Authorization authorization, Cache cache)
            throws FaultException
    {
        // Get all reservations and disconnect them from parents
        Collection<Reservation> reservations = new LinkedList<Reservation>();
        getChildReservations(reservation, reservations);

        // Get ACL records for deletion
        Collection<AclRecord> aclRecordsToDelete = new LinkedList<AclRecord>();
        if (authorization != null) {
            for (Reservation reservationToDelete : reservations) {
                aclRecordsToDelete.addAll(authorization.getAclRecords(reservationToDelete));
            }
        }

        // Date/time now for stopping executables
        DateTime dateTimeNow = DateTime.now().withField(DateTimeFieldType.millisOfSecond(), 0);
        // Stop all executables
        stopReservationExecutables(reservation, dateTimeNow);

        // Delete all reservations
        for (Reservation reservationToDelete : reservations) {
            // Remove additional reservation from the cache
            cache.removeReservation(reservationToDelete);

            // Delete additional reservation
            super.delete(reservationToDelete);
        }
        return aclRecordsToDelete;
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
            if (executable.getState().equals(Executable.State.STARTED)) {
                if (executable.getSlotEnd().isAfter(dateTimeNow)) {
                    DateTime newSlotEnd = dateTimeNow;
                    if (newSlotEnd.isBefore(executable.getSlotStart())) {
                        newSlotEnd = executable.getSlotStart();
                    }
                    executable.setSlotEnd(newSlotEnd);
                    ExecutableManager executableManager = new ExecutableManager(entityManager);
                    executableManager.update(executable);
                }
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
     * @throws FaultException when the {@link Reservation} doesn't exist
     */
    public Reservation get(Long reservationId) throws FaultException
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
            return ControllerFaultSet.throwEntityNotFoundFault(Reservation.class, reservationId);
        }
    }

    /**
     * @param reservationRequest for which the {@link Reservation} should be returned
     * @return {@link Reservation} for the given {@link ReservationRequest} or null if doesn't exists
     */
    public Reservation getByReservationRequest(ReservationRequest reservationRequest)
    {
        return getByReservationRequest(reservationRequest.getId());
    }

    /**
     * @param reservationRequestId of the {@link ReservationRequest} for which the {@link Reservation} should be returned
     * @return {@link Reservation} for the given {@link ReservationRequest} or null if doesn't exists
     */
    public Reservation getByReservationRequest(Long reservationRequestId)
    {
        try {
            Reservation reservation = entityManager.createQuery(
                    "SELECT reservation FROM Reservation reservation WHERE reservation.reservationRequest.id = :id",
                    Reservation.class)
                    .setParameter("id", reservationRequestId)
                    .getSingleResult();
            return reservation;
        }
        catch (NoResultException exception) {
            return null;
        }
    }

    /**
     * @param ids                requested identifiers
     * @param reservationClasses set of reservation classes which are allowed
     * @param technologies       requested technologies
     * @return list of {@link Reservation}s
     */
    public List<Reservation> list(Set<Long> ids, Long reservationRequestId,
            Set<Class<? extends Reservation>> reservationClasses, Set<Technology> technologies)
    {
        DatabaseFilter filter = new DatabaseFilter("reservation");
        filter.addIds(ids);
        if (reservationClasses != null && reservationClasses.size() > 0) {

            if (reservationClasses.contains(AliasReservation.class)) {
                // List only reservations of given classes or simple reservations which have alias reservation as child
                filter.addFilter("reservation IN ("
                        + "   SELECT mainReservation FROM Reservation mainReservation"
                        + "   LEFT JOIN mainReservation.childReservations childReservation"
                        + "   WHERE TYPE(mainReservation) IN(:classes)"
                        + "      OR (TYPE(mainReservation) = :mainClass AND TYPE(childReservation) = :aliasClass)"
                        + " )");
                filter.addFilterParameter("classes", reservationClasses);
                filter.addFilterParameter("aliasClass", AliasReservation.class);
                filter.addFilterParameter("mainClass", Reservation.class);
            }
            else {
                // List only reservations of given classes
                filter.addFilter("TYPE(reservation) IN(:classes)");
                filter.addFilterParameter("classes", reservationClasses);
            }
        }
        if (reservationRequestId != null) {
            // List only reservations which are allocated for request with given id
            filter.addFilter("reservation IN ("
                    + "   SELECT reservation FROM Reservation reservation"
                    + "   LEFT JOIN reservation.reservationRequest reservationRequest"
                    + "   LEFT JOIN reservationRequest.reservationRequestSet reservationRequestSet"
                    + "   WHERE reservationRequest.id = :reservationRequestId OR reservationRequestSet.id = :reservationRequestId"
                    + " )");
            filter.addFilterParameter("reservationRequestId", reservationRequestId);
        }

        TypedQuery<Reservation> query = entityManager.createQuery(
                "SELECT reservation FROM Reservation reservation"
                        + " WHERE reservation.parentReservation IS NULL AND " + filter.toQueryWhere(),
                Reservation.class);
        filter.fillQueryParameters(query);
        List<Reservation> reservations = query.getResultList();

        if (technologies != null && technologies.size() > 0) {
            Iterator<Reservation> iterator = reservations.iterator();
            while (iterator.hasNext()) {
                Reservation reservation = iterator.next();
                if (reservation instanceof AliasReservation) {
                    AliasReservation aliasReservation = (AliasReservation) reservation;
                    boolean technologyFound = false;
                    for (Alias alias : aliasReservation.getAliases()) {
                        if (technologies.contains(alias.getTechnology())) {
                            technologyFound = true;
                            break;
                        }
                    }
                    if (!technologyFound) {
                        iterator.remove();
                    }
                }
                else if (reservation.getClass().equals(Reservation.class)) {
                    boolean technologyFound = false;
                    for (Reservation childReservation : reservation.getChildReservations()) {
                        if (childReservation instanceof AliasReservation) {
                            AliasReservation childAliasReservation = (AliasReservation) childReservation;
                            for (Alias alias : childAliasReservation.getAliases()) {
                                if (technologies.contains(alias.getTechnology())) {
                                    technologyFound = true;
                                    break;
                                }
                            }
                        }
                        else {
                            throw new TodoImplementException(childReservation.getClass().getName());
                        }
                    }
                    if (!technologyFound) {
                        iterator.remove();
                    }
                }
                else {
                    throw new TodoImplementException(reservation.getClass().getName());
                }
            }
        }
        return reservations;
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
     * @return list of reused {@link Reservation}.
     */
    public List<Reservation> getReusedReservations()
    {
        List<Reservation> reservations = entityManager.createQuery(
                "SELECT DISTINCT reservation.reservation FROM ExistingReservation reservation", Reservation.class)
                .getResultList();
        return reservations;
    }

    /**
     * Get list of {@link ExistingReservation} which reuse the given {@code reusedReservation}.
     *
     * @param reusedReservation which must be referenced in the {@link ExistingReservation#reservation}
     * @return list of {@link ExistingReservation} which reuse the given {@code reusedReservation}
     */
    public List<ExistingReservation> getExistingReservations(Reservation reusedReservation)
    {
        List<ExistingReservation> reservations = entityManager.createQuery(
                "SELECT reservation FROM ExistingReservation reservation"
                        + " WHERE reservation.reservation = :reusedReservation",
                ExistingReservation.class)
                .setParameter("reusedReservation", reusedReservation)
                .getResultList();
        return reservations;
    }

    /**
     * Get list of {@link ResourceReservation} for given {@code resource} in given {@code slot}.
     *
     * @param resource
     * @param interval
     * @return list of {@link ResourceReservation} for given {@code resource}
     */
    public List<ResourceReservation> getResourceReservations(Resource resource, Interval interval)
    {
        List<ResourceReservation> reservations = entityManager.createQuery(
                "SELECT reservation FROM ResourceReservation reservation"
                        + " WHERE reservation.resource = :resource"
                        + " AND reservation.slotStart < :end"
                        + " AND reservation.slotEnd > :start",
                ResourceReservation.class)
                .setParameter("resource", resource)
                .setParameter("start", interval.getStart())
                .setParameter("end", interval.getEnd())
                .getResultList();
        return reservations;
    }

    /**
     * Get list of {@link ValueReservation} for given {@code valueProvider} in given {@code slot}.
     *
     * @param valueProvider
     * @param interval
     * @return list of {@link ValueReservation} for given {@code valueProvider}
     */
    public List<ValueReservation> getValueReservations(ValueProvider valueProvider, Interval interval)
    {
        List<ValueReservation> reservations = entityManager.createQuery(
                "SELECT reservation FROM ValueReservation reservation"
                        + " WHERE reservation.valueProvider = :valueProvider"
                        + " AND reservation.slotStart < :end"
                        + " AND reservation.slotEnd > :start",
                ValueReservation.class)
                .setParameter("valueProvider", valueProvider)
                .setParameter("start", interval.getStart())
                .setParameter("end", interval.getEnd())
                .getResultList();
        return reservations;
    }

    /**
     * Delete {@link Reservation}s which aren't allocated for any {@link ReservationRequest}.
     *
     * @return list of deleted {@link Reservation}
     */
    public List<Reservation> getReservationsForDeletion()
    {
        List<Reservation> reservations = entityManager.createQuery(
                "SELECT reservation FROM Reservation reservation"
                        + " LEFT JOIN reservation.reservationRequest reservationRequest"
                        + " WHERE reservation.createdBy = :createdBy"
                        + " AND reservation.parentReservation IS NULL"
                        + " AND (reservationRequest IS NULL OR reservationRequest.state != :state)",
                Reservation.class)
                .setParameter("createdBy", Reservation.CreatedBy.CONTROLLER)
                .setParameter("state", ReservationRequest.State.ALLOCATED)
                .getResultList();
        return reservations;
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
                        + " WHERE :reservation MEMBER OF reservationRequest.providedReservations")
                .setParameter("reservation", reservation)
                .getResultList();
        if (reservationRequests.size() > 0) {
            return true;
        }
        return false;
    }
}
