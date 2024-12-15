package cz.cesnet.shongo.controller.booking.executable;

import cz.cesnet.shongo.*;
import cz.cesnet.shongo.controller.ControllerReportSetHelper;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.booking.participant.PersonParticipant;
import cz.cesnet.shongo.controller.booking.recording.RecordingCapability;
import cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.booking.reservation.Reservation;
import cz.cesnet.shongo.controller.booking.resource.DeviceResource;
import cz.cesnet.shongo.controller.booking.room.ResourceRoomEndpoint;
import cz.cesnet.shongo.controller.booking.room.RoomEndpoint;
import cz.cesnet.shongo.controller.booking.room.UsedRoomEndpoint;
import cz.cesnet.shongo.controller.executor.ExecutionReportSet;
import cz.cesnet.shongo.controller.util.NativeQuery;
import cz.cesnet.shongo.controller.util.QueryFilter;
import cz.cesnet.shongo.jade.SendLocalCommand;
import org.joda.time.DateTime;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import java.util.*;

/**
 * Manager for {@link Executable}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ExecutableManager extends AbstractManager
{
    /**
     * List of {@link cz.cesnet.shongo.controller.executor.ExecutionReport}s which have been created.
     */
    private List<cz.cesnet.shongo.controller.executor.ExecutionReport> executionReports = new LinkedList<cz.cesnet.shongo.controller.executor.ExecutionReport>();

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
        executable.updateExecutableSummary(entityManager, false);
    }

    /**
     * @param executable to be updated in the database
     */
    public void update(Executable executable)
    {
        super.update(executable);
        executable.updateExecutableSummary(entityManager, false);
    }

    /**
     * @param executable to be deleted in the database
     */
    public void delete(Executable executable, AuthorizationManager authorizationManager)
    {
        authorizationManager.deleteAclEntriesForEntity(executable);
        super.delete(executable);
        executable.updateExecutableSummary(entityManager, true);
    }

    /**
     * Updates table executable_summary, DO NOT USE directly, for more see {@link Executable#updateExecutableSummary(EntityManager, boolean)}
     *
     * @param executable
     * @param deleteOnly
     */
    public void updateExecutableSummary(Executable executable, boolean deleteOnly)
    {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("executable_id", executable.getId().toString());

        String deleteQuery = NativeQuery.getNativeQuery(NativeQuery.EXECUTABLE_SUMMARY_DELETE, parameters);
        entityManager.createNativeQuery(deleteQuery).executeUpdate();

        if (!deleteOnly) {
            String updateQuery = NativeQuery.getNativeQuery(NativeQuery.EXECUTABLE_SUMMARY_INSERT, parameters);
            entityManager.createNativeQuery(updateQuery).executeUpdate();
        }
    }

    /**
     * @param executableId of the {@link Executable}
     * @return {@link Executable} with given id
     * @throws cz.cesnet.shongo.CommonReportSet.ObjectNotExistsException
     *          when the {@link Executable} doesn't exist
     */
    public Executable get(Long executableId) throws CommonReportSet.ObjectNotExistsException
    {
        try {
            return entityManager.createQuery(
                    "SELECT executable FROM Executable executable"
                            + " WHERE executable.id = :id AND executable.state != :notAllocated",
                    Executable.class)
                    .setParameter("id", executableId)
                    .setParameter("notAllocated", Executable.State.NOT_ALLOCATED)
                    .getSingleResult();
        }
        catch (NoResultException exception) {
            return ControllerReportSetHelper.throwObjectNotExistFault(Executable.class, executableId);
        }
    }

    /**
     * @param executableServiceId of the {@link ExecutableService}
     * @return {@link ExecutableService} with given id
     * @throws cz.cesnet.shongo.CommonReportSet.ObjectNotExistsException
     *          when the {@link ExecutableService} doesn't exist
     */
    public ExecutableService getService(Long executableServiceId)
    {
        try {
            return entityManager.createQuery("SELECT service FROM ExecutableService service WHERE service.id = :id",
                    ExecutableService.class)
                    .setParameter("id", executableServiceId)
                    .getSingleResult();
        }
        catch (NoResultException exception) {
            return ControllerReportSetHelper.throwObjectNotExistFault(ExecutableService.class, executableServiceId);
        }
    }

    /**
     * @param ids requested identifiers
     * @return list of all allocated {@link Executable}s
     */
    public List<Executable> list(Set<Long> ids)
    {
        QueryFilter filter = new QueryFilter("executable");
        filter.addFilterIn("id", ids);
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
     * @param referenceDateTime which represents now
     * @param maxAttemptCount
     * @return list of {@link Executable}s which should be started for given {@code referenceDateTime}
     */
    public List<Executable> listExecutablesForStart(DateTime referenceDateTime, int maxAttemptCount)
    {
        return entityManager.createQuery(
                "SELECT executable FROM Executable executable"
                        + " WHERE (executable.state IN(:notStartedStates)"
                        + "        OR (executable.nextAttempt != NULL AND executable.state = :startingFailedState))"
                        + " AND (executable.slotStart <= :dateTime AND executable.slotEnd >= :dateTime)"
                        + " AND ((executable.nextAttempt IS NULL AND executable.attemptCount = 0) OR executable.nextAttempt <= :dateTime)"
                        + " AND (executable.attemptCount < :maxAttemptCount)",
                Executable.class)
                .setParameter("dateTime", referenceDateTime)
                .setParameter("notStartedStates", EnumSet.of(Executable.State.NOT_STARTED))
                .setParameter("startingFailedState", Executable.State.STARTING_FAILED)
                .setParameter("maxAttemptCount", maxAttemptCount)
                .getResultList();
    }

    /**
     * @param referenceDateTime in which the {@link Executable}s must take place
     * @param maxAttemptCount
     * @return list of {@link Executable}s which are in one of given {@code states}
     *         and take place at given {@code dateTime}
     */
    public List<Executable> listExecutablesForUpdate(DateTime referenceDateTime, int maxAttemptCount)
    {
        return entityManager.createQuery(
                "SELECT executable FROM Executable executable"
                        + " WHERE executable.state IN(:modifiableStates) AND executable.modified = TRUE"
                        + " AND ((executable.nextAttempt IS NULL AND executable.attemptCount = 0) OR executable.nextAttempt <= :dateTime)"
                        + " AND (executable.attemptCount < :maxAttemptCount)",
                Executable.class)
                .setParameter("dateTime", referenceDateTime)
                .setParameter("modifiableStates", Executable.MODIFIABLE_STATES)
                .setParameter("maxAttemptCount", maxAttemptCount)
                .getResultList();
    }

    /**
     * @param referenceDateTime which represents now
     * @param maxAttemptCount
     * @return list of {@link Executable}s which should be stopped for given {@code referenceDateTime}
     */
    public List<Executable> listExecutablesForStop(DateTime referenceDateTime, int maxAttemptCount)
    {
        return entityManager.createQuery(
                "SELECT executable FROM Executable executable"
                        + " WHERE (executable.state IN(:startedStates)"
                        + "        OR (executable.nextAttempt != NULL AND executable.state = :stoppingFailedState))"
                        + " AND (executable.slotEnd <= :dateTime)"
                        + " AND ((executable.nextAttempt IS NULL AND executable.attemptCount = 0) OR executable.nextAttempt <= :dateTime)"
                        + " AND (executable.attemptCount < :maxAttemptCount)",
                Executable.class)
                .setParameter("dateTime", referenceDateTime)
                .setParameter("startedStates", EnumSet.of(Executable.State.STARTED, Executable.State.PARTIALLY_STARTED))
                .setParameter("stoppingFailedState", Executable.State.STOPPING_FAILED)
                .setParameter("maxAttemptCount", maxAttemptCount)
                .getResultList();
    }

    /**
     * @param referenceDateTime which represents now
     * @param maxAttemptCount
     * @return list of {@link Executable}s which should be finalized for given {@code referenceDateTime}
     */
    public List<Executable> listExecutablesForFinalization(DateTime referenceDateTime, int maxAttemptCount)
    {
        return entityManager
                .createQuery("SELECT executable FROM Executable executable"
                        + " WHERE executable NOT IN("
                        + "   SELECT childExecutable FROM Executable executable "
                        + "   INNER JOIN executable.childExecutables childExecutable "
                        + " ) AND executable NOT IN (SELECT reservation.executable FROM Reservation reservation)"
                        + " AND (executable.state IN(:states)"
                        + "      OR (executable.nextAttempt != NULL AND executable.state = :failedState))"
                        + " AND (executable.slotEnd < :dateTime)"
                        + " AND ((executable.nextAttempt IS NULL AND executable.attemptCount = 0) OR executable.nextAttempt <= :dateTime)"
                        + " AND (executable.attemptCount < :maxAttemptCount)",
                        Executable.class)
                .setParameter("dateTime", referenceDateTime)
                .setParameter("states", EnumSet.of(Executable.State.STOPPED, Executable.State.SKIPPED))
                .setParameter("failedState", Executable.State.FINALIZATION_FAILED)
                .setParameter("maxAttemptCount", maxAttemptCount)
                .getResultList();
    }

    /**
     * @param referenceDateTime which represents now
     * @param maxAttemptCount
     * @return list of {@link ExecutableService}s which should be activated for given {@code referenceDateTime}
     */
    public List<ExecutableService> listServicesForActivation(DateTime referenceDateTime, int maxAttemptCount)
    {
        return entityManager.createQuery(
                "SELECT service FROM ExecutableService service"
                        + " WHERE (service.state IN(:notActiveStates)"
                        + "        OR (service.nextAttempt != NULL AND service.state = :activationFailedState))"
                        + " AND (service.slotStart <= :dateTime AND service.slotEnd >= :dateTime)"
                        + " AND ((service.nextAttempt IS NULL AND service.attemptCount = 0) OR service.nextAttempt <= :dateTime)"
                        + " AND (service.attemptCount < :maxAttemptCount)",
                ExecutableService.class)
                .setParameter("dateTime", referenceDateTime)
                .setParameter("notActiveStates", EnumSet.of(ExecutableService.State.PREPARED))
                .setParameter("activationFailedState", ExecutableService.State.ACTIVATION_FAILED)
                .setParameter("maxAttemptCount", maxAttemptCount)
                .getResultList();
    }

    /**
     * @param referenceDateTime which represents now
     * @param maxAttemptCount
     * @return list of {@link ExecutableService}s which should be deactivated for given {@code referenceDateTime}
     */
    public List<ExecutableService> listServicesForDeactivation(DateTime referenceDateTime, int maxAttemptCount)
    {
        return entityManager.createQuery(
                "SELECT service FROM ExecutableService service"
                        + " WHERE (service.state IN(:activeStates)"
                        + "        OR (service.nextAttempt != NULL AND service.state = :deactivationFailedState))"
                        + " AND (service.slotStart > :dateTime OR service.slotEnd <= :dateTime)"
                        + " AND ((service.nextAttempt IS NULL AND service.attemptCount = 0) OR service.nextAttempt <= :dateTime)"
                        + " AND (service.attemptCount < :maxAttemptCount)",
                ExecutableService.class)
                .setParameter("dateTime", referenceDateTime)
                .setParameter("activeStates", EnumSet.of(ExecutableService.State.ACTIVE))
                .setParameter("deactivationFailedState", ExecutableService.State.DEACTIVATION_FAILED)
                .setParameter("maxAttemptCount", maxAttemptCount)
                .getResultList();
    }

    /**
     * Delete all {@link Executable}s which are not placed inside another {@link Executable} and not referenced by
     * any {@link Reservation} and which should be automatically
     * deleted ({@link Executable.State#NOT_ALLOCATED} or {@link Executable.State#NOT_STARTED}).
     *
     * @param authorizationManager
     * @return true whether some {@link Executable} has been deleted, false otherwise
     */
    public boolean deleteAllNotReferenced(AuthorizationManager authorizationManager)
    {
        List<Executable> executablesForDeletion = entityManager
                .createQuery("SELECT executable FROM Executable executable"
                        + " WHERE executable NOT IN("
                        + "   SELECT childExecutable FROM Executable executable "
                        + "   INNER JOIN executable.childExecutables childExecutable "
                        + " ) AND ("
                        + "       executable.state = :toDelete "
                        + "   OR ("
                        + "       executable.state = :notStarted "
                        + "       AND executable NOT IN (SELECT reservation.executable FROM Reservation reservation)"
                        + " ))",
                        Executable.class)
                .setParameter("notStarted", Executable.State.NOT_STARTED)
                .setParameter("toDelete", Executable.State.TO_DELETE)
                .getResultList();

        List<Executable> referencedExecutables = new LinkedList<Executable>();
        for (Executable executableForDeletion : executablesForDeletion) {
            getReferencedExecutables(SimplePersistentObject.getLazyImplementation(executableForDeletion),
                    referencedExecutables);
        }
        // Move all reused reservations to the end
        for (Executable referencedExecutable : referencedExecutables) {
            Executable topReferencedReservation = referencedExecutable;
            if (executablesForDeletion.contains(topReferencedReservation)) {
                executablesForDeletion.remove(topReferencedReservation);
                executablesForDeletion.add(topReferencedReservation);
            }
        }
        for (Executable executable : executablesForDeletion) {
            delete(executable, authorizationManager);
        }
        return executablesForDeletion.size() > 0;
    }

    /**
     * @param deviceResourceId
     * @param roomId
     * @return started {@link RoomEndpoint} in given {@code deviceResourceId} with given {@code roomId}
     */
    public RoomEndpoint getRoomEndpoint(Long deviceResourceId, String roomId)
    {
        ResourceRoomEndpoint resourceRoomEndpoint;
        try {
            resourceRoomEndpoint = entityManager.createQuery(
                    "SELECT room FROM ResourceRoomEndpoint room"
                            + " WHERE room.roomProviderCapability.resource.id = :resourceId"
                            + " AND room.roomId = :roomId"
                            + " AND room.state IN(:startedStates)", ResourceRoomEndpoint.class)
                    .setParameter("resourceId", deviceResourceId)
                    .setParameter("roomId", roomId)
                    .setParameter("startedStates", EnumSet.of(
                            Executable.State.STARTED, Executable.State.STOPPING_FAILED))
                    .getSingleResult();
        }
        catch (NoResultException exception) {
            return null;
        }
        List<UsedRoomEndpoint> usedRoomEndpoints = entityManager.createQuery(
                "SELECT room FROM UsedRoomEndpoint room"
                        + " WHERE room.reusedRoomEndpoint = :room"
                        + " AND room.state IN(:startedStates)", UsedRoomEndpoint.class)
                .setParameter("room", resourceRoomEndpoint)
                .setParameter("startedStates", EnumSet.of(
                        Executable.State.STARTED, Executable.State.STOPPING_FAILED))
                .getResultList();
        if (usedRoomEndpoints.size() == 0) {
            return resourceRoomEndpoint;
        }
        else if (usedRoomEndpoints.size() == 1) {
            return usedRoomEndpoints.get(0);
        }
        else {
            //TODO:
            throw new RuntimeException("Found multiple started " + UsedRoomEndpoint.class.getSimpleName()
                    + "s.");
        }
    }

    /**
     * @param deviceResource
     * @param recordingFolderId
     * @return {@link Executable} with recording folder with given {@code recordingFolderId}
     *         in given {@code deviceResource} with {@link RecordingCapability}
     */
    public Executable getExecutableByRecordingFolder(DeviceResource deviceResource, String recordingFolderId)
    {
        RecordingCapability recordingCapability = deviceResource.getCapabilityRequired(RecordingCapability.class);
        List<Executable> executables = entityManager.createQuery(
                "SELECT executable FROM Executable executable"
                        + " WHERE executable IN("
                        + "  SELECT roomEndpoint FROM ResourceRoomEndpoint roomEndpoint"
                        + "  LEFT JOIN roomEndpoint.recordingFolderIds recordingFolder"
                        + "  WHERE roomEndpoint.migrateToExecutable IS NULL "
                        + "  AND INDEX(recordingFolder) = :recordingCapability AND recordingFolder = :recordingFolderId"
                        + " )", Executable.class)
                .setParameter("recordingCapability", recordingCapability)
                .setParameter("recordingFolderId", recordingFolderId)
                .getResultList();
        if (executables.size() == 0) {
            return null;
        }
        else if (executables.size() == 1) {
            return executables.get(0);
        }
        else {
            throw new RuntimeException("Found multiple " + Executable.class.getSimpleName()
                    + "s with recording folder " + recordingFolderId + ".");
        }
    }

    /**
     * @return started {@link UsedRoomEndpoint} for given {@code roomEndpoint} or null if none exists
     */
    public UsedRoomEndpoint getStartedUsedRoomEndpoint(RoomEndpoint roomEndpoint)
    {
        List<UsedRoomEndpoint> usedRoomEndpoints = entityManager.createQuery(
                "SELECT room FROM UsedRoomEndpoint room"
                        + " WHERE room.reusedRoomEndpoint = :roomEndpoint"
                        + " AND room.state = :startedState",
                UsedRoomEndpoint.class)
                .setParameter("roomEndpoint", roomEndpoint)
                .setParameter("startedState", Executable.State.STARTED)
                .getResultList();
        if (usedRoomEndpoints.size() == 0) {
            return null;
        }
        if (usedRoomEndpoints.size() == 1) {
            return usedRoomEndpoints.get(0);
        }
        throw new RuntimeException("Found multiple started " + UsedRoomEndpoint.class.getSimpleName()
                + "s for " + ResourceRoomEndpoint.class + " with id " + roomEndpoint.getId() + ".");
    }

    /**
     * @return list of future {@link UsedRoomEndpoint}s for given {@code roomEndpoint}
     */
    public List<UsedRoomEndpoint> getFutureUsedRoomEndpoint(RoomEndpoint roomEndpoint)
    {
        return entityManager.createQuery(
                "SELECT room FROM UsedRoomEndpoint room"
                        + " WHERE room.reusedRoomEndpoint = :roomEndpoint"
                        + " AND room.state IN(:futureStates)",
                UsedRoomEndpoint.class)
                .setParameter("roomEndpoint", roomEndpoint)
                .setParameter("futureStates", EnumSet.of(
                        Executable.State.STARTED, Executable.State.NOT_STARTED))
                .getResultList();
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
            topReservations.add(reservation.getTopReservation());
        }
        if (topReservations.size() > 0) {
            return topReservations.iterator().next();
        }
        return null;
    }



    /**
     * @param executable
     * @return map of recording folder identifiers by {@link RecordingCapability}s for given {@code executable}
     */
    public boolean isUserParticipantInExecutable(String userId, Executable executable)
    {
        String query = "SELECT participant FROM PersonParticipant participant"
                + " WHERE participant.person.userId = :userId"
                + " AND ("
                + "   participant IN("
                + "     SELECT participant FROM RoomEndpoint roomEndpoint"
                + "     LEFT JOIN roomEndpoint.participants participant"
                + "     WHERE roomEndpoint = :executable OR roomEndpoint.reusedRoomEndpoint = :executable"
                + "   )";
        if (executable instanceof UsedRoomEndpoint) {
            query += " OR participant IN("
                    + "     SELECT participant FROM UsedRoomEndpoint roomEndpoint"
                    + "     LEFT JOIN roomEndpoint.reusedRoomEndpoint reusedRoomEndpoint"
                    + "     LEFT JOIN reusedRoomEndpoint.participants participant"
                    + "     WHERE roomEndpoint = :executable"
                    + "   )";
        }
        query += ")";

        List<PersonParticipant> results = entityManager.createQuery(query, PersonParticipant.class)
                .setParameter("userId", userId)
                .setParameter("executable", executable)
                .getResultList();
        return results.size() > 0;
    }

    /**
     * @param executionTarget to which the {@code executionReport} will be added
     * @param executionReport to be added to the {@code executable}
     */
    public void createExecutionReport(ExecutionTarget executionTarget,
            cz.cesnet.shongo.controller.executor.ExecutionReport executionReport)
    {
        executionReport.setDateTime(DateTime.now());
        executionTarget.addReport(executionReport);

        executionReports.add(executionReport);
    }

    /**
     * @param executionTarget to which the {@code executionReport} will be added
     * @param sendLocalCommand to be reported to the {@code executable}
     */
    public void createExecutionReport(ExecutionTarget executionTarget, SendLocalCommand sendLocalCommand)
    {
        String commandName = sendLocalCommand.getName();
        JadeReport jadeReport = sendLocalCommand.getJadeReport();
        createExecutionReport(executionTarget, new ExecutionReportSet.CommandFailedReport(commandName, jadeReport));
    }

    /**
     * @return {@link #executionReports}
     */
    public List<cz.cesnet.shongo.controller.executor.ExecutionReport> getExecutionReports()
    {
        return executionReports;
    }

    /**
     * Fill {@link Executable}s which are referenced (e.g., by {@link UsedRoomEndpoint})
     * from given {@code executable} to given {@code referencedExecutables}.
     *
     * @param executable
     * @param referencedExecutables
     */
    private void getReferencedExecutables(Executable executable, List<Executable> referencedExecutables)
    {
        if (executable instanceof UsedRoomEndpoint) {
            UsedRoomEndpoint usedRoomEndpoint = (UsedRoomEndpoint) executable;
            Executable referencedExecutable = usedRoomEndpoint.getReusedRoomEndpoint();
            referencedExecutables.add(referencedExecutable);
        }
        for (Executable childExecutable : executable.getChildExecutables()) {
            getReferencedExecutables(childExecutable, referencedExecutables);
        }
    }
}
