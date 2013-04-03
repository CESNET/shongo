package cz.cesnet.shongo.controller.executor;

import cz.cesnet.shongo.AbstractManager;
import cz.cesnet.shongo.controller.ControllerFaultSet;
import cz.cesnet.shongo.controller.authorization.AclRecord;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.util.DatabaseFilter;
import cz.cesnet.shongo.fault.FaultException;
import org.joda.time.DateTime;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import java.util.*;

/**
 * Manager for {@link Executable}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ExecutableManager extends AbstractManager
{
    /**
     * @param entityManager sets the {@link #entityManager}
     */
    public ExecutableManager(EntityManager entityManager)
    {
        super(entityManager);
    }

    /**
     * @param executable to be created in the database
     */
    public void create(Executable executable)
    {
        super.create(executable);
    }

    /**
     * @param executable to be updated in the database
     */
    public void update(Executable executable)
    {
        super.update(executable);
    }

    /**
     * @param executable to be deleted in the database
     * @return {@link AclRecord}s which should be deleted
     */
    public Collection<AclRecord> delete(Executable executable, Authorization authorization) throws FaultException
    {
        Collection<AclRecord> aclRecordsToDelete = new LinkedList<AclRecord>();
        if (authorization != null) {
            aclRecordsToDelete.addAll(authorization.getAclRecords(executable));
        }
        super.delete(executable);
        return aclRecordsToDelete;
    }

    /**
     * @param executableId of the {@link Executable}
     * @return {@link Executable} with given id
     * @throws FaultException when the {@link Executable} doesn't exist
     */
    public Executable get(Long executableId)
            throws FaultException
    {
        try {
            Executable executable = entityManager.createQuery(
                    "SELECT executable FROM Executable executable"
                            + " WHERE executable.id = :id AND executable.state != :notAllocated",
                    Executable.class)
                    .setParameter("id", executableId)
                    .setParameter("notAllocated", Executable.State.NOT_ALLOCATED)
                    .getSingleResult();
            return executable;
        }
        catch (NoResultException exception) {
            return ControllerFaultSet.throwEntityNotFoundFault(Executable.class, executableId);
        }
    }

    /**
     * @param ids requested identifiers
     * @return list of all allocated {@link Executable}s
     */
    public List<Executable> list(Set<Long> ids)
    {
        DatabaseFilter filter = new DatabaseFilter("executable");
        filter.addIds(ids);
        TypedQuery<Executable> query = entityManager.createQuery("SELECT executable FROM Executable executable"
                + " WHERE executable.state != :notAllocated"
                + " AND executable NOT IN("
                + "    SELECT childExecutable FROM Executable executable "
                + "   INNER JOIN executable.childExecutables childExecutable"
                + " )"
                + " AND " + filter.toQueryWhere(),
                Executable.class);
        query.setParameter("notAllocated", Executable.State.NOT_ALLOCATED);
        filter.fillQueryParameters(query);
        List<Executable> executables = query.getResultList();
        return executables;
    }

    /**
     * @param states in which the {@link Executable}s must be
     * @return list of {@link Executable}s which are in one of given {@code states}
     */
    public List<Executable> list(Collection<Executable.State> states)
    {
        List<Executable> executables = entityManager.createQuery(
                "SELECT executable FROM Executable executable"
                        + " WHERE executable NOT IN("
                        + "   SELECT childExecutable FROM Executable executable "
                        + "   INNER JOIN executable.childExecutables childExecutable"
                        + " ) AND executable.state IN(:states)",
                Executable.class)
                .setParameter("states", states)
                .getResultList();
        return executables;
    }

    /**
     * @param states   in which the {@link Executable}s must be
     * @param dateTime in which the {@link Executable}s must take place
     * @return list of {@link Executable}s which are in one of given {@code states}
     *         and take place at given {@code dateTime}
     */
    public List<Executable> listTakingPlace(Collection<Executable.State> states, DateTime dateTime)
    {
        List<Executable> executables = entityManager.createQuery(
                "SELECT executable FROM Executable executable"
                        + " WHERE executable.state IN(:states)"
                        + "   AND executable.slotStart <= :dateTime AND executable.slotEnd >= :dateTime",
                Executable.class)
                .setParameter("states", states)
                .setParameter("dateTime", dateTime)
                .getResultList();
        return executables;
    }

    /**
     * @param states   in which the {@link Executable}s must be
     * @param dateTime in which the {@link Executable}s must not take place
     * @return list of {@link Executable}s which are in one of given {@code states}
     *         and don't take place at given {@code dateTime}
     */
    public List<Executable> listNotTakingPlace(Collection<Executable.State> states, DateTime dateTime)
    {
        List<Executable> executables = entityManager.createQuery(
                "SELECT executable FROM Executable executable"
                        + " WHERE executable.state IN(:states)"
                        + "   AND (executable.slotStart > :dateTime OR executable.slotEnd <= :dateTime)",
                Executable.class)
                .setParameter("states", states)
                .setParameter("dateTime", dateTime)
                .getResultList();
        return executables;
    }

    /**
     * Delete all {@link Executable}s which are not placed inside another {@link Executable} and not referenced by
     * any {@link Reservation} and which should be automatically
     * deleted ({@link Executable.State#NOT_ALLOCATED} or {@link Executable.State#NOT_STARTED}).
     *
     * @param authorization
     * @return {@link AclRecord}s which should be deleted
     */
    public Collection<AclRecord> deleteAllNotReferenced(Authorization authorization) throws FaultException
    {
        List<Executable> executables = entityManager
                .createQuery("SELECT executable FROM Executable executable"
                        + " WHERE executable NOT IN("
                        + "   SELECT childExecutable FROM Executable executable "
                        + "   INNER JOIN executable.childExecutables childExecutable "
                        + " ) AND ("
                        + "       executable.state = :toDelete "
                        + "   OR ("
                        + "       executable.state = :notStarted "
                        + "       AND executable NOT IN (SELECT reservation.executable FROM Reservation reservation))"
                        + " )",
                        Executable.class)
                .setParameter("notStarted", Executable.State.NOT_STARTED)
                .setParameter("toDelete", Executable.State.TO_DELETE)
                .getResultList();
        Collection<AclRecord> aclRecordsToDelete = new LinkedList<AclRecord>();
        for (Executable executable : executables) {
            aclRecordsToDelete.addAll(delete(executable, authorization));
        }
        return aclRecordsToDelete;
    }

    /**
     * @param deviceResourceId
     * @param roomId
     * @param referenceDateTime
     * @return {@link RoomEndpoint} in given {@code deviceResourceId} with given {@code roomId}
     *         and taking place in given {@code referenceDateTime}
     */
    public RoomEndpoint getRoomEndpoint(Long deviceResourceId, String roomId, DateTime referenceDateTime)
    {
        ResourceRoomEndpoint resourceRoomEndpoint;
        try {
            resourceRoomEndpoint = entityManager.createQuery(
                    "SELECT room FROM ResourceRoomEndpoint room"
                            + " WHERE room.roomProviderCapability.resource.id = :resourceId"
                            + " AND room.roomId = :roomId"
                            + " AND room.slotStart <= :dateTime AND room.slotEnd > :dateTime",
                    ResourceRoomEndpoint.class)
                    .setParameter("resourceId", deviceResourceId)
                    .setParameter("roomId", roomId)
                    .setParameter("dateTime", referenceDateTime)
                    .getSingleResult();
        }
        catch (NoResultException exception) {
            return null;
        }
        List<UsedRoomEndpoint> usedRoomEndpoints = entityManager.createQuery(
                "SELECT room FROM UsedRoomEndpoint room"
                        + " WHERE room.roomEndpoint = :room"
                        + " AND room.slotStart <= :dateTime AND room.slotEnd > :dateTime", UsedRoomEndpoint.class)
                .setParameter("room", resourceRoomEndpoint)
                .setParameter("dateTime", referenceDateTime)
                .getResultList();
        if (usedRoomEndpoints.size() == 0) {
            return resourceRoomEndpoint;
        }
        if (usedRoomEndpoints.size() == 1) {
            return usedRoomEndpoints.get(0);
        }
        throw new RuntimeException("Found multiple " + UsedRoomEndpoint.class.getSimpleName()
                + "s taking place at " + referenceDateTime.toString() + ".");
    }

    /**
     * @return started {@link UsedRoomEndpoint} for given {@code ResourceRoomEndpoint} or null if none exists
     */
    public UsedRoomEndpoint getStartedUsedRoomEndpoint(ResourceRoomEndpoint resourceRoomEndpoint)
    {
        List<UsedRoomEndpoint> usedRoomEndpoints = entityManager.createQuery(
                "SELECT room FROM UsedRoomEndpoint room"
                        + " WHERE room.roomEndpoint = :roomEndpoint"
                        + " AND room.state = :stateStarted",
                UsedRoomEndpoint.class)
                .setParameter("roomEndpoint", resourceRoomEndpoint)
                .setParameter("stateStarted", Executable.State.STARTED)
                .getResultList();
        if (usedRoomEndpoints.size() == 0) {
            return null;
        }
        if (usedRoomEndpoints.size() == 1) {
            return usedRoomEndpoints.get(0);
        }
        throw new RuntimeException("Found multiple started " + UsedRoomEndpoint.class.getSimpleName()
                + "s for " + ResourceRoomEndpoint.class + " with id " + resourceRoomEndpoint.getId() + ".");
    }

    /**
     * @param executable
     * @return {@link Reservation} for which is given {@code executable} allocated
     */
    public Reservation getReservation(Executable executable)
    {
        // Go to top parent executable
        Set<Executable> executables = new HashSet<Executable>();
        while (!executables.contains(executable)) {
            executables.add(executable);
            List<Executable> parentExecutables = entityManager.createQuery(
                    "SELECT executable FROM Executable executable"
                            + " LEFT JOIN executable.childExecutables AS childExecutable"
                            + " WHERE childExecutable = :executable", Executable.class)
                    .setParameter("executable", executable)
                    .getResultList();
            if (parentExecutables.size() > 0) {
                executable = parentExecutables.get(0);
            }
        }

        List<Reservation> reservations = entityManager.createQuery(
                "SELECT reservation FROM Reservation reservation"
                        + " WHERE reservation.executable = :executable", Reservation.class)
                .setParameter("executable", executable)
                .getResultList();
        Set<Reservation> topReservations = new HashSet<Reservation>();
        for (Reservation reservation : reservations) {
            while (reservation.getParentReservation() != null) {
                reservation = reservation.getParentReservation();
            }
            topReservations.add(reservation);
        }
        if (topReservations.size() > 0) {
            return topReservations.iterator().next();
        }
        return null;
    }
}
