package cz.cesnet.shongo.controller.api.rpc;

import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.ExpirationMap;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.Recording;
import cz.cesnet.shongo.api.Room;
import cz.cesnet.shongo.api.jade.Command;
import cz.cesnet.shongo.connector.api.jade.multipoint.rooms.GetRoom;
import cz.cesnet.shongo.connector.api.jade.recording.ListRecordings;
import cz.cesnet.shongo.controller.*;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.request.ExecutableListRequest;
import cz.cesnet.shongo.controller.api.request.ExecutableRecordingListRequest;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.authorization.AclRecord;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.booking.EntityIdentifier;
import cz.cesnet.shongo.controller.booking.executable.ExecutableManager;
import cz.cesnet.shongo.controller.booking.executable.Migration;
import cz.cesnet.shongo.controller.booking.recording.RecordingCapability;
import cz.cesnet.shongo.controller.booking.resource.DeviceResource;
import cz.cesnet.shongo.controller.booking.resource.ManagedMode;
import cz.cesnet.shongo.controller.booking.room.ResourceRoomEndpoint;
import cz.cesnet.shongo.controller.booking.room.UsedRoomEndpoint;
import cz.cesnet.shongo.controller.executor.ExecutionReport;
import cz.cesnet.shongo.controller.executor.Executor;
import cz.cesnet.shongo.controller.util.DatabaseHelper;
import cz.cesnet.shongo.controller.util.NativeQuery;
import cz.cesnet.shongo.controller.util.QueryFilter;
import cz.cesnet.shongo.controller.util.StateReportSerializer;
import cz.cesnet.shongo.jade.SendLocalCommand;
import cz.cesnet.shongo.report.Report;
import cz.cesnet.shongo.report.ReportException;
import cz.cesnet.shongo.report.ReportRuntimeException;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.format.DateTimeFormatter;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.*;

/**
 * Implementation of {@link ExecutableService}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ExecutableServiceImpl extends AbstractServiceImpl
        implements ExecutableService, Component.EntityManagerFactoryAware,
                   Component.AuthorizationAware, Component.ControllerAgentAware
{
    /**
     * @see javax.persistence.EntityManagerFactory
     */
    private EntityManagerFactory entityManagerFactory;

    /**
     * @see cz.cesnet.shongo.controller.authorization.Authorization
     */
    private Authorization authorization;

    /**
     * @see ControllerAgent
     */
    private ControllerAgent controllerAgent;

    /**
     * @see Executor
     */
    private final Executor executor;

    /**
     * Collection of {@link Recording}s by executableId.
     */
    private final ExpirationMap<Long, List<Recording>> executableRecordingsCache =
            new ExpirationMap<Long, List<Recording>>();

    /**
     * Constructor.
     */
    public ExecutableServiceImpl(Executor executor)
    {
        this.executor = executor;
        this.executableRecordingsCache.setExpiration(Duration.standardMinutes(1));
    }

    @Override
    public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory)
    {
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public void setAuthorization(Authorization authorization)
    {
        this.authorization = authorization;
    }

    @Override
    public void setControllerAgent(ControllerAgent controllerAgent)
    {
        this.controllerAgent = controllerAgent;
    }

    @Override
    public void init(ControllerConfiguration configuration)
    {
        checkDependency(entityManagerFactory, EntityManagerFactory.class);
        checkDependency(authorization, Authorization.class);
        super.init(configuration);
    }

    @Override
    public String getServiceName()
    {
        return "Executable";
    }

    @Override
    public ListResponse<ExecutableSummary> listExecutables(ExecutableListRequest request)
    {
        SecurityToken securityToken = request.getSecurityToken();
        authorization.validate(securityToken);
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            QueryFilter queryFilter = new QueryFilter("executable_summary", true);

            // List only reservations which is current user permitted to read
            queryFilter.addFilterId(authorization, securityToken, AclRecord.EntityType.EXECUTABLE, Permission.READ);

            // If history executables should not be included
            String filterExecutableId = "id IS NOT NULL";
            if (!request.isHistory()) {
                // List only executables which are allocated by any existing reservation
                filterExecutableId = "id IN(SELECT reservation.executable_id FROM reservation)";
            }

            // List only executables of requested classes
            if (request.getTypes().size() > 0) {
                StringBuilder typeFilterBuilder = new StringBuilder();
                for (ExecutableSummary.Type type : request.getTypes()) {
                    if (typeFilterBuilder.length() > 0) {
                        typeFilterBuilder.append(",");
                    }
                    typeFilterBuilder.append("'");
                    typeFilterBuilder.append(type.toString());
                    typeFilterBuilder.append("'");
                }
                typeFilterBuilder.insert(0, "executable_summary.type IN(");
                typeFilterBuilder.append(")");
                queryFilter.addFilter(typeFilterBuilder.toString());
            }

            // List only usages of specified room
            if (request.getRoomId() != null) {
                queryFilter.addFilter("executable_summary.room_id = :roomId");
                queryFilter.addFilterParameter("roomId", EntityIdentifier.parseId(
                        cz.cesnet.shongo.controller.booking.executable.Executable.class, request.getRoomId()));
            }

            // Sort query part
            String queryOrderBy;
            ExecutableListRequest.Sort sort = request.getSort();
            if (sort != null) {
                switch (sort) {
                    case ROOM_NAME:
                        queryOrderBy = "executable_summary.room_name";
                        break;
                    case SLOT:
                        queryOrderBy = "executable_summary.slot_end";
                        break;
                    case STATE:
                        queryOrderBy = "executable_summary.state";
                        break;
                    case ROOM_TECHNOLOGY:
                        queryOrderBy = "executable_summary.room_technologies";
                        break;
                    case ROOM_LICENSE_COUNT:
                        queryOrderBy = "executable_summary.room_license_count";
                        break;
                    default:
                        throw new TodoImplementException(sort);
                }
            }
            else {
                queryOrderBy = "executable_summary.id";
            }
            Boolean sortDescending = request.getSortDescending();
            sortDescending = (sortDescending != null ? sortDescending : false);
            if (sortDescending) {
                queryOrderBy = queryOrderBy + " DESC";
            }

            Map<String, String> parameters = new HashMap<String, String>();
            parameters.put("filterExecutableId", filterExecutableId);
            parameters.put("filter", queryFilter.toQueryWhere());
            parameters.put("order", queryOrderBy);
            String query = NativeQuery.getNativeQuery(NativeQuery.EXECUTABLE_LIST, parameters);

            ListResponse<ExecutableSummary> response = new ListResponse<ExecutableSummary>();
            List<Object[]> records = performNativeListRequest(query, queryFilter, request, response, entityManager);
            for (Object[] record : records) {
                ExecutableSummary executableSummary = new ExecutableSummary();
                executableSummary.setId(EntityIdentifier.formatId(EntityType.EXECUTABLE, record[0].toString()));
                executableSummary.setType(ExecutableSummary.Type.valueOf(record[1].toString().trim()));
                executableSummary.setSlot(new Interval(new DateTime(record[2]), new DateTime(record[3])));
                executableSummary.setState(cz.cesnet.shongo.controller.booking.executable.Executable.State.valueOf(
                        record[4].toString()).toApi());

                switch (executableSummary.getType()) {
                    case USED_ROOM:
                        executableSummary.setRoomId(
                                EntityIdentifier.formatId(EntityType.EXECUTABLE, record[8].toString()));
                    case ROOM:
                        executableSummary.setRoomName(record[5] != null ? record[5].toString() : null);
                        if (record[6] != null) {
                            String technologies = record[6].toString();
                            if (!technologies.isEmpty()) {
                                for (String technology : technologies.split(",")) {
                                    executableSummary.addTechnology(Technology.valueOf(technology.trim()));
                                }
                            }
                        }
                        executableSummary.setRoomLicenseCount(((Number) record[7]).intValue());
                        if (record[9] != null && record[10] != null) {
                            executableSummary.setRoomUsageSlot(
                                    new Interval(new DateTime(record[9]), new DateTime(record[10])));
                        }
                        if (record[11] != null) {
                            executableSummary.setRoomUsageState(
                                    cz.cesnet.shongo.controller.booking.executable.Executable.State.valueOf(
                                            record[11].toString()).toApi());
                        }
                        if (record[12] != null) {
                            executableSummary.setRoomUsageLicenseCount(((Number) record[12]).intValue());
                        }
                        executableSummary.setRoomUsageCount(((Number) record[13]).intValue());
                        break;
                }

                response.addItem(executableSummary);
            }
            return response;
        }
        finally {
            entityManager.close();
        }
    }

    @Override
    public cz.cesnet.shongo.controller.api.Executable getExecutable(SecurityToken securityToken, String executableId)
    {
        authorization.validate(securityToken);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ExecutableManager executableManager = new ExecutableManager(entityManager);
        EntityIdentifier entityId = EntityIdentifier.parse(executableId, EntityType.EXECUTABLE);
        try {
            cz.cesnet.shongo.controller.booking.executable.Executable executable =
                    executableManager.get(entityId.getPersistenceId());

            if (!authorization.hasPermission(securityToken, executable, Permission.READ)) {
                ControllerReportSetHelper.throwSecurityNotAuthorizedFault("read executable %s", entityId);
            }

            Executable executableApi = executable.toApi(authorization.isAdmin(securityToken));
            cz.cesnet.shongo.controller.booking.reservation.Reservation reservation =
                    executableManager.getReservation(executable);
            if (reservation != null) {
                executableApi.setReservationId(EntityIdentifier.formatId(reservation));
            }
            return executableApi;
        }
        finally {
            entityManager.close();
        }
    }

    @Override
    public void modifyExecutableConfiguration(SecurityToken securityToken, String executableId,
            ExecutableConfiguration executableConfiguration)
    {
        authorization.validate(securityToken);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ExecutableManager executableManager = new ExecutableManager(entityManager);
        EntityIdentifier entityId = EntityIdentifier.parse(executableId, EntityType.EXECUTABLE);
        try {
            entityManager.getTransaction().begin();

            cz.cesnet.shongo.controller.booking.executable.Executable executable =
                    executableManager.get(entityId.getPersistenceId());

            if (!authorization.hasPermission(securityToken, executable, Permission.READ)) {
                ControllerReportSetHelper.throwSecurityNotAuthorizedFault("read executable %s", entityId);
            }
            if (executable.updateFromExecutableConfigurationApi(executableConfiguration, entityManager)) {
                cz.cesnet.shongo.controller.booking.executable.Executable.State executableState = executable.getState();
                if (executableState.equals(cz.cesnet.shongo.controller.booking.executable.Executable.State.STARTED)) {
                    executable.setState(cz.cesnet.shongo.controller.booking.executable.Executable.State.MODIFIED);
                }
            }

            entityManager.getTransaction().commit();
        }
        finally {
            entityManager.close();
        }
    }

    @Override
    public void deleteExecutable(SecurityToken securityToken, String executableId)
    {
        authorization.validate(securityToken);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ExecutableManager executableManager = new ExecutableManager(entityManager);
        AuthorizationManager authorizationManager = new AuthorizationManager(entityManager, authorization);
        EntityIdentifier entityId = EntityIdentifier.parse(executableId, EntityType.EXECUTABLE);
        try {
            authorizationManager.beginTransaction();
            entityManager.getTransaction().begin();

            cz.cesnet.shongo.controller.booking.executable.Executable executable =
                    executableManager.get(entityId.getPersistenceId());

            if (!authorization.hasPermission(securityToken, executable, Permission.WRITE)) {
                ControllerReportSetHelper.throwSecurityNotAuthorizedFault("delete executable %s", entityId);
            }

            executableManager.delete(executable, authorizationManager);

            entityManager.getTransaction().commit();
            authorizationManager.commitTransaction();
        }
        catch (javax.persistence.RollbackException exception) {
            ControllerReportSetHelper.throwEntityNotDeletableReferencedFault(
                    cz.cesnet.shongo.controller.booking.executable.Executable.class, entityId.getPersistenceId());
        }
        finally {
            if (authorizationManager.isTransactionActive()) {
                authorizationManager.rollbackTransaction();
            }
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            entityManager.close();
        }
    }

    @Override
    public void updateExecutable(SecurityToken securityToken, String executableId)
    {
        authorization.validate(securityToken);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ExecutableManager executableManager = new ExecutableManager(entityManager);
        EntityIdentifier entityId = EntityIdentifier.parse(executableId, EntityType.EXECUTABLE);
        try {
            entityManager.getTransaction().begin();

            cz.cesnet.shongo.controller.booking.executable.Executable executable =
                    executableManager.get(entityId.getPersistenceId());

            if (!authorization.hasPermission(securityToken, executable, Permission.WRITE)) {
                ControllerReportSetHelper.throwSecurityNotAuthorizedFault("update executable %s", entityId);
            }

            Set<cz.cesnet.shongo.controller.booking.executable.Executable> executablesToUpdate =
                    new HashSet<cz.cesnet.shongo.controller.booking.executable.Executable>();
            executablesToUpdate.add(executable);
            Migration migration = executable.getMigration();
            if (migration != null) {
                executablesToUpdate.add(migration.getSourceExecutable());
                executablesToUpdate.add(migration.getTargetExecutable());
            }

            int maxAttemptCount = getConfiguration().getInt(ControllerConfiguration.EXECUTOR_EXECUTABLE_MAX_ATTEMPT_COUNT);
            DateTime dateTimeNow = DateTime.now();
            for (cz.cesnet.shongo.controller.booking.executable.Executable executableToUpdate : executablesToUpdate) {
                if (executableToUpdate.getSlot().contains(dateTimeNow)) {
                    // Schedule next attempt
                    if (executableToUpdate.getAttemptCount() >= maxAttemptCount) {
                        executableToUpdate.setAttemptCount(maxAttemptCount - 1);
                    }
                    executableToUpdate.setNextAttempt(dateTimeNow);
                }
                else if (executableToUpdate.getSlotEnd().isBefore(dateTimeNow)) {
                    // Set executable as stopped
                    cz.cesnet.shongo.controller.booking.executable.Executable.State state = executableToUpdate.getState();
                    if (state.isStarted() ||
                            state.equals(cz.cesnet.shongo.controller.booking.executable.Executable.State.STARTING_FAILED)) {
                        executableToUpdate.setState(cz.cesnet.shongo.controller.booking.executable.Executable.State.STOPPED);
                    }
                }
            }

            entityManager.getTransaction().commit();
        }
        finally {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            entityManager.close();
        }
    }

    @Override
    public void attachRoomExecutable(SecurityToken securityToken, String roomExecutableId, String deviceRoomId)
    {
        authorization.validate(securityToken);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ExecutableManager executableManager = new ExecutableManager(entityManager);
        EntityIdentifier entityId = EntityIdentifier.parse(roomExecutableId, EntityType.EXECUTABLE);
        try {
            // Get and check room executable
            ResourceRoomEndpoint roomExecutable =
                    entityManager.find(ResourceRoomEndpoint.class, entityId.getPersistenceId());
            if (roomExecutable == null) {
                ControllerReportSetHelper.throwEntityNotFoundFault(RoomExecutable.class, entityId.getPersistenceId());
                return;
            }
            DeviceResource deviceResource = roomExecutable.getDeviceResource();
            if (!authorization.hasPermission(securityToken, roomExecutable, Permission.WRITE) ||
                    !authorization.hasPermission(securityToken, deviceResource, Permission.WRITE)) {
                ControllerReportSetHelper.throwSecurityNotAuthorizedFault("attach room %s", entityId);
            }
            if (!deviceResource.isManaged()) {
                throw new CommonReportSet.UnknownErrorException("Device is not managed.");
            }
            if (!roomExecutable.getState()
                    .equals(cz.cesnet.shongo.controller.booking.executable.Executable.State.NOT_STARTED)) {
                throw new CommonReportSet.UnknownErrorException("Room executable must be NOT_STARTED.");
            }

            // Get and check device room
            Room deviceRoom = (Room) performDeviceCommand(deviceResource, new GetRoom(deviceRoomId));
            if (deviceRoom == null) {
                throw new CommonReportSet.UnknownErrorException("Device room doesn't exist.");
            }
            List<ResourceRoomEndpoint> roomExecutables = entityManager.createQuery(
                    "SELECT executable FROM ResourceRoomEndpoint executable"
                            + " WHERE executable.roomProviderCapability = :roomProvider"
                            + " AND executable.roomId = :roomId",
                    ResourceRoomEndpoint.class)
                    .setParameter("roomProvider", roomExecutable.getRoomProviderCapability())
                    .setParameter("roomId", deviceRoomId)
                    .getResultList();
            if (roomExecutables.size() > 0) {
                throw new CommonReportSet.UnknownErrorException("Device room is already used in " +
                        EntityIdentifier.formatId(roomExecutables.get(0)) + ".");
            }

            // Attach device room to room executable
            entityManager.getTransaction().begin();
            roomExecutable.setState(cz.cesnet.shongo.controller.booking.executable.Executable.State.MODIFIED);
            roomExecutable.setRoomId(deviceRoomId);
            executableManager.update(roomExecutable);
            entityManager.getTransaction().commit();
        }
        finally {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            entityManager.close();
        }
    }

    @Override
    public Object activateExecutableService(SecurityToken securityToken, String executableId, String executableServiceId)
    {
        authorization.validate(securityToken);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ExecutableManager executableManager = new ExecutableManager(entityManager);
        EntityIdentifier entityId = EntityIdentifier.parse(executableId, EntityType.EXECUTABLE);
        try {
            cz.cesnet.shongo.controller.booking.executable.Executable executable =
                    executableManager.get(entityId.getPersistenceId());

            if (!authorization.hasPermission(securityToken, executable, Permission.WRITE)) {
                ControllerReportSetHelper.throwSecurityNotAuthorizedFault("control executable %s", entityId);
            }

            cz.cesnet.shongo.controller.booking.executable.ExecutableService executableService =
                    executable.getServiceById(Long.parseLong(executableServiceId));

            entityManager.getTransaction().begin();

            synchronized (executor) {
                executableService.activate(executor, executableManager);
            }

            entityManager.getTransaction().commit();

            // Reporting
            for (ExecutionReport executionReport : executableManager.getExecutionReports()) {
                Reporter.report(executionReport.getExecutionTarget(), executionReport);
            }

            // Check activation failed
            if (!executableService.isActive()) {
                ExecutionReport executionReport = executableService.getLastReport();
                cz.cesnet.shongo.controller.api.ExecutionReport executionReportApi =
                        new cz.cesnet.shongo.controller.api.ExecutionReport(authorization.isAdmin(securityToken) ?
                                Report.UserType.DOMAIN_ADMIN : Report.UserType.USER);
                executionReportApi.addReport(new StateReportSerializer(executionReport));
                return executionReportApi;
            }

            return Boolean.TRUE;
        }
        finally {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            entityManager.close();
        }
    }

    @Override
    public Object deactivateExecutableService(SecurityToken securityToken, String executableId,
            String executableServiceId)
    {
        authorization.validate(securityToken);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ExecutableManager executableManager = new ExecutableManager(entityManager);
        EntityIdentifier entityId = EntityIdentifier.parse(executableId, EntityType.EXECUTABLE);
        try {
            Long persistenceId = entityId.getPersistenceId();
            cz.cesnet.shongo.controller.booking.executable.Executable executable =
                    executableManager.get(persistenceId);

            if (!authorization.hasPermission(securityToken, executable, Permission.WRITE)) {
                ControllerReportSetHelper.throwSecurityNotAuthorizedFault("control executable %s", entityId);
            }

            cz.cesnet.shongo.controller.booking.executable.ExecutableService executableService =
                    executable.getServiceById(Long.parseLong(executableServiceId));

            entityManager.getTransaction().begin();

            synchronized (executor) {
                executableService.deactivate(executor, executableManager);
            }

            entityManager.getTransaction().commit();

            // Reporting
            for (ExecutionReport executionReport : executableManager.getExecutionReports()) {
                Reporter.report(executionReport.getExecutionTarget(), executionReport);
            }

            // Check deactivation failed
            if (executableService.isActive()) {
                ExecutionReport executionReport = executableService.getLastReport();
                cz.cesnet.shongo.controller.api.ExecutionReport executionReportApi =
                        new cz.cesnet.shongo.controller.api.ExecutionReport(authorization.isAdmin(securityToken) ?
                                Report.UserType.DOMAIN_ADMIN : Report.UserType.USER);
                executionReportApi.addReport(new StateReportSerializer(executionReport));
                return executionReportApi;
            }

            // Clear recordings cache (new recording should be fetched)
            executableRecordingsCache.remove(executable.getId());
            if (executable instanceof UsedRoomEndpoint) {
                UsedRoomEndpoint usedRoomEndpoint = (UsedRoomEndpoint) executable;
                executableRecordingsCache.remove(usedRoomEndpoint.getRoomEndpoint().getId());
            }

            return Boolean.TRUE;
        }
        finally {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            entityManager.close();
        }
    }

    @Override
    public ListResponse<Recording> listExecutableRecordings(ExecutableRecordingListRequest request)
    {
        SecurityToken securityToken = request.getSecurityToken();
        authorization.validate(securityToken);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ExecutableManager executableManager = new ExecutableManager(entityManager);
        EntityIdentifier entityId = EntityIdentifier.parse(request.getExecutableId(), EntityType.EXECUTABLE);
        try {
            Long executableId = entityId.getPersistenceId();
            cz.cesnet.shongo.controller.booking.executable.Executable executable =
                    executableManager.get(executableId);

            if (!authorization.hasPermission(securityToken, executable, Permission.READ)) {
                ControllerReportSetHelper.throwSecurityNotAuthorizedFault("read executable %s", entityId);
            }

            List<Recording> recordings = executableRecordingsCache.get(executableId);
            if (recordings == null) {
                // Get all recording folders
                Map<RecordingCapability, List<String>> recordingFolders =
                        executableManager.listExecutableRecordingFolders(executable);

                // Get all recordings from folders
                recordings = new LinkedList<Recording>();
                for (Map.Entry<RecordingCapability, List<String>> entry : recordingFolders.entrySet()) {
                    RecordingCapability recordingCapability = entry.getKey();
                    DeviceResource recordingDeviceResource = recordingCapability.getDeviceResource();
                    for (String recordingFolderId : entry.getValue()) {
                        if (recordingFolderId == null) {
                            continue;
                        }
                        @SuppressWarnings("unchecked")
                        Collection<Recording> serviceRecordings = (Collection<Recording>) performDeviceCommand(
                                recordingDeviceResource, new ListRecordings(recordingFolderId));
                        recordings.addAll(serviceRecordings);
                    }
                }
                executableRecordingsCache.put(executableId, recordings);
            }

            Integer start = request.getStart();
            Integer count = request.getCount();
            Integer maxIndex = Math.max(0, recordings.size() - 1);
            if (start == null) {
                start = 0;
            }
            else if (start > maxIndex) {
                start = maxIndex;
            }
            if (count == null) {
                count = recordings.size();
            }
            int end = start + count;
            if (end > recordings.size()) {
                end = recordings.size();
            }
            ListResponse<Recording> response = new ListResponse<Recording>();
            response.setStart(start);
            response.setCount(recordings.size());
            for (Recording recording : recordings.subList(start, end)) {
                response.addItem(recording);
            }
            return response;
        }
        finally {
            entityManager.close();
        }
    }

    private Object performDeviceCommand(DeviceResource deviceResource, Command command)
    {
        if (!deviceResource.isManaged()) {
            throw new CommonReportSet.UnknownErrorException(
                    "Device " + EntityIdentifier.formatId(deviceResource) + " is not managed.");
        }
        ManagedMode managedMode = (ManagedMode) deviceResource.getMode();
        String agentName = managedMode.getConnectorAgentName();
        SendLocalCommand sendLocalCommand = controllerAgent.sendCommand(agentName, command);
        if (!sendLocalCommand.getState().equals(SendLocalCommand.State.SUCCESSFUL)) {
            throw new ControllerReportSet.DeviceCommandFailedException(EntityIdentifier.formatId(deviceResource),
                    command.toString(), sendLocalCommand.getJadeReport());
        }
        return sendLocalCommand.getResult();
    }
}
