package cz.cesnet.shongo.controller.api.rpc;

import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.Recording;
import cz.cesnet.shongo.api.Room;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.api.jade.Command;
import cz.cesnet.shongo.connector.api.jade.multipoint.GetRoom;
import cz.cesnet.shongo.connector.api.jade.recording.ListRecordings;
import cz.cesnet.shongo.controller.*;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.Executable;
import cz.cesnet.shongo.controller.api.RecordingService;
import cz.cesnet.shongo.controller.api.domains.request.StartRecording;
import cz.cesnet.shongo.controller.api.domains.response.*;
import cz.cesnet.shongo.controller.api.domains.response.RoomSpecification;
import cz.cesnet.shongo.controller.api.request.ExecutableListRequest;
import cz.cesnet.shongo.controller.api.request.ExecutableRecordingListRequest;
import cz.cesnet.shongo.controller.api.request.ExecutableServiceListRequest;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.executable.*;
import cz.cesnet.shongo.controller.booking.participant.AbstractParticipant;
import cz.cesnet.shongo.controller.booking.recording.*;
import cz.cesnet.shongo.controller.booking.recording.RecordingCapability;
import cz.cesnet.shongo.controller.booking.resource.DeviceResource;
import cz.cesnet.shongo.controller.booking.resource.ManagedMode;
import cz.cesnet.shongo.controller.booking.room.ResourceRoomEndpoint;
import cz.cesnet.shongo.controller.booking.room.RoomEndpoint;
import cz.cesnet.shongo.controller.booking.room.UsedRoomEndpoint;
import cz.cesnet.shongo.controller.domains.DomainsConnector;
import cz.cesnet.shongo.controller.domains.InterDomainAgent;
import cz.cesnet.shongo.controller.executor.ExecutionAction;
import cz.cesnet.shongo.controller.executor.ExecutionPlan;
import cz.cesnet.shongo.controller.executor.ExecutionReport;
import cz.cesnet.shongo.controller.executor.Executor;
import cz.cesnet.shongo.controller.notification.AbstractNotification;
import cz.cesnet.shongo.controller.notification.NotificationManager;
import cz.cesnet.shongo.controller.notification.RoomNotification;
import cz.cesnet.shongo.controller.util.NativeQuery;
import cz.cesnet.shongo.controller.util.QueryFilter;
import cz.cesnet.shongo.controller.util.StateReportSerializer;
import cz.cesnet.shongo.jade.SendLocalCommand;
import cz.cesnet.shongo.report.Report;
import org.joda.time.DateTime;
import org.joda.time.Interval;

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
     * @see RecordingsCache
     */
    private final RecordingsCache recordingsCache;

    /**
     * Constructor.
     */
    public ExecutableServiceImpl(Executor executor, RecordingsCache recordingsCache)
    {
        this.executor = executor;
        this.recordingsCache = recordingsCache;
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
        checkNotNull("request", request);
        SecurityToken securityToken = request.getSecurityToken();
        authorization.validate(securityToken);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            QueryFilter queryFilter = new QueryFilter("executable_summary", true);

            String participantUserId = request.getParticipantUserId();
            if (participantUserId != null) {
                // Do not filter executables by permissions because they will be filtered by participation,
                // check only the existence of the participant user
                authorization.checkUserExistence(participantUserId);


                if (!participantUserId.equals(securityToken.getUserId()) && !authorization.isOperator(securityToken)) {
                    throw new ControllerReportSet.SecurityNotAuthorizedException(
                            "read participation executables for user " + participantUserId);
                }
            }
            else {
                // List only executables which is current user permitted to read
                queryFilter.addFilterId(authorization, securityToken,
                        cz.cesnet.shongo.controller.booking.executable.Executable.class, ObjectPermission.READ);
            }

            // If history executables should not be included
            String filterExecutableId = "id IS NOT NULL";
            if (!request.isHistory()) {
                // List only executables which are allocated by any existing reservation
                filterExecutableId = "id IN(SELECT reservation.executable_id FROM reservation)";
            }

            // List only rooms with given user-id participant
            if (request.getParticipantUserId() != null) {
                queryFilter.addFilter("executable_summary.id IN("
                        + " SELECT room_endpoint.id"
                        + " FROM room_endpoint"
                        + " LEFT JOIN used_room_endpoint ON used_room_endpoint.id = room_endpoint.id"
                        + " LEFT JOIN room_endpoint_participants ON room_endpoint_participants.room_endpoint_id = room_endpoint.id OR room_endpoint_participants.room_endpoint_id = used_room_endpoint.room_endpoint_id"
                        + " LEFT JOIN person_participant ON person_participant.id = room_endpoint_participants.abstract_participant_id"
                        + " LEFT JOIN person ON person.id = person_participant.person_id"
                        + " WHERE person.user_id = :participantUserId"
                        + ")");
                queryFilter.addFilterParameter("participantUserId", request.getParticipantUserId());
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
            if (request.getResourceId() != null) {
                queryFilter.addFilter("executable_summary.resource_id = :resourceId");
                queryFilter.addFilterParameter("resourceId", ObjectIdentifier.parseLocalId(
                        request.getResourceId(), ObjectType.RESOURCE));
            }

            // List only usages of specified room
            if (request.getRoomId() != null) {
                queryFilter.addFilter("executable_summary.room_id = :roomId");
                queryFilter.addFilterParameter("roomId", ObjectIdentifier.parseLocalId(
                        request.getRoomId(), ObjectType.EXECUTABLE));
            }

            // Filter room license count
            String roomLicenseCount = request.getRoomLicenseCount();
            if (roomLicenseCount != null) {
                if (roomLicenseCount.equals(ExecutableListRequest.FILTER_NON_ZERO)) {
                    queryFilter.addFilter("executable_summary.room_license_count > 0");
                }
                else {
                    queryFilter.addFilter("executable_summary.room_license_count = :roomLicenseCount");
                    queryFilter.addFilterParameter("roomLicenseCount", Long.parseLong(roomLicenseCount));
                }
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
                DateTime slotStart = new DateTime(record[2]);
                DateTime slotEnd = new DateTime(record[3]);
                ExecutableSummary executableSummary = new ExecutableSummary();
                executableSummary.setId(ObjectIdentifier.formatId(ObjectType.EXECUTABLE, record[0].toString()));
                executableSummary.setType(ExecutableSummary.Type.valueOf(record[1].toString().trim()));
                executableSummary.setSlot(new Interval(slotStart, (slotEnd.isBefore(slotStart) ? slotStart : slotEnd)));
                executableSummary.setState(cz.cesnet.shongo.controller.booking.executable.Executable.State.valueOf(
                        record[4].toString()).toApi());
                executableSummary.setRoomDescription(record[8] != null ? (String) record[8] : null);

                ExecutableSummary.Type executableSummaryType = executableSummary.getType();
                if (ExecutableSummary.Type.USED_ROOM.equals(executableSummaryType)) {
                    executableSummary.setRoomId(
                            ObjectIdentifier.formatId(ObjectType.EXECUTABLE, record[9].toString()));
                }
                if (ExecutableSummary.Type.ROOM.equals(executableSummaryType) ||
                        ExecutableSummary.Type.USED_ROOM.equals(executableSummaryType)) {
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
                    if (record[10] != null && record[11] != null) {
                        executableSummary.setRoomUsageSlot(
                                new Interval(new DateTime(record[10]), new DateTime(record[11])));
                    }
                    if (record[12] != null) {
                        executableSummary.setRoomUsageState(
                                cz.cesnet.shongo.controller.booking.executable.Executable.State.valueOf(
                                        record[12].toString()).toApi());
                    }
                    if (record[13] != null) {
                        executableSummary.setRoomUsageLicenseCount(((Number) record[13]).intValue());
                    }
                    executableSummary.setRoomUsageCount(((Number) record[14]).intValue());
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
        UserInformation userInformation = authorization.validate(securityToken);
        checkNotNull("executableId", executableId);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ExecutableManager executableManager = new ExecutableManager(entityManager);
        ObjectIdentifier objectId = ObjectIdentifier.parse(executableId, ObjectType.EXECUTABLE);
        try {
            cz.cesnet.shongo.controller.booking.executable.Executable executable =
                    executableManager.get(objectId.getPersistenceId());

            if (!authorization.hasObjectPermission(securityToken, executable, ObjectPermission.READ)) {
                // Participants can also read the executable
                // TODO: consider to cache whether participant is in executable
                if (!executableManager.isUserParticipantInExecutable(userInformation.getUserId(), executable)) {
                    ControllerReportSetHelper.throwSecurityNotAuthorizedFault("read executable %s", objectId);
                }
            }

            Executable executableApi = executable.toApi(entityManager, authorization.isOperator(securityToken));
            cz.cesnet.shongo.controller.booking.reservation.Reservation reservation =
                    executableManager.getReservation(executable);
            if (reservation != null) {
                executableApi.setReservationId(ObjectIdentifier.formatId(reservation));
            }
            return executableApi;
        }
        finally {
            entityManager.close();
        }
    }

    @Override
    public ListResponse<cz.cesnet.shongo.controller.api.ExecutableService> listExecutableServices(
            ExecutableServiceListRequest request)
    {
        checkNotNull("request", request);
        SecurityToken securityToken = request.getSecurityToken();
        authorization.validate(securityToken);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ExecutableManager executableManager = new ExecutableManager(entityManager);
        ObjectIdentifier objectId = ObjectIdentifier.parse(request.getExecutableId(), ObjectType.EXECUTABLE);
        try {
            cz.cesnet.shongo.controller.booking.executable.Executable executable =
                    executableManager.get(objectId.getPersistenceId());

            if (!authorization.hasObjectPermission(securityToken, executable, ObjectPermission.READ)) {
                ControllerReportSetHelper.throwSecurityNotAuthorizedFault("read executable %s", objectId);
            }

            QueryFilter queryFilter = new QueryFilter("executableService", true);
            queryFilter.addFilterParameter("executable", executable);
            if (request.getServiceClasses().size() > 0) {
                Set<Class<? extends cz.cesnet.shongo.controller.booking.executable.ExecutableService>> serviceClasses =
                        new HashSet<Class<? extends cz.cesnet.shongo.controller.booking.executable.ExecutableService>>();
                for (Class<? extends cz.cesnet.shongo.controller.api.ExecutableService> serviceClass :
                        request.getServiceClasses()) {
                    if (serviceClass.equals(RecordingService.class)) {
                        serviceClasses.add(cz.cesnet.shongo.controller.booking.recording.RecordingService.class);
                    }
                    else {
                        throw new TodoImplementException(serviceClass);
                    }
                }
                queryFilter.addFilter("TYPE(executableService) IN (:executableServiceClasses)");
                queryFilter.addFilterParameter("executableServiceClasses", serviceClasses);
            }

            String query = "SELECT executableService FROM ExecutableService executableService"
                    + " WHERE (executableService.executable = :executable "
                    + "    OR executableService.executable IN("
                    + "     SELECT usedRoomEndpoint FROM UsedRoomEndpoint usedRoomEndpoint"
                    + "     WHERE usedRoomEndpoint.reusedRoomEndpoint = :executable"
                    + "    ) OR executableService.executable IN("
                    + "     SELECT usedRoomEndpoint.reusedRoomEndpoint FROM UsedRoomEndpoint usedRoomEndpoint"
                    + "     WHERE usedRoomEndpoint = :executable"
                    + " ))"
                    + " AND " + queryFilter.toQueryWhere();

            ListResponse<cz.cesnet.shongo.controller.api.ExecutableService> response =
                    new ListResponse<cz.cesnet.shongo.controller.api.ExecutableService>();

            // List services for ForeignExecutables and return
            // TODO: combine both in the future
            if (executable instanceof ForeignExecutable) {
                ForeignExecutable foreignExecutable = (ForeignExecutable) executable;
                String foreignReservationRequestId = foreignExecutable.getForeignReservationRequestId();
                cz.cesnet.shongo.controller.api.domains.response.Reservation reservation;
                try {
                    reservation = InterDomainAgent.getInstance().getConnector().getReservationByRequest(foreignReservationRequestId);
                }
                catch (ForeignDomainConnectException e) {
                    throw new RuntimeException("Failed to get foreign reservation.", e);
                }

                ForeignSpecification specification = reservation.getSpecification();
                if (specification instanceof RoomSpecification) {
                    RoomSpecification roomSpecification = (RoomSpecification) specification;
                    if (roomSpecification.isRecorded()) {
                        RecordingService recordingService;
                        recordingService = new RecordingService();
                        // Sets executableId to foreign reservation request id
                        recordingService.setExecutableId(foreignReservationRequestId);
                        recordingService.setActive(roomSpecification.isRecordingActive());

                        response.addItem(recordingService);
                        response.setCount(1);
                        response.setStart(0);
                    }

                } else {
                    throw new TodoImplementException("Unsupported foreign specification: " + specification.getClass());
                }

                //Return only if any service has been found
                if (response.getCount() > 0) {
                    return response;
                }
            }

            List<cz.cesnet.shongo.controller.booking.executable.ExecutableService> services = performListRequest(
                    query, queryFilter, cz.cesnet.shongo.controller.booking.executable.ExecutableService.class,
                    request, response, entityManager);

            // Determine which services should be checked
            List<cz.cesnet.shongo.controller.booking.executable.ExecutableService> checkServices =
                    new LinkedList<cz.cesnet.shongo.controller.booking.executable.ExecutableService>();
            DateTime referenceDateTime = DateTime.now();
            for (cz.cesnet.shongo.controller.booking.executable.ExecutableService service : services) {
                if (service.getSlot().contains(referenceDateTime) && executor.isExecutableServiceCheckable(service)) {
                    checkServices.add(service);
                }
            }

            // Check services
            if (checkServices.size() > 0) {
                entityManager.getTransaction().begin();
                synchronized (executor) {
                    // Build execution plan
                    ExecutionPlan executionPlan = new ExecutionPlan(executor);
                    for (cz.cesnet.shongo.controller.booking.executable.ExecutableService service : checkServices) {
                        executionPlan.addExecutionAction(new ExecutionAction.CheckExecutableServiceAction(service));
                    }
                    executionPlan.build();

                    // Perform execution plan
                    while (!executionPlan.isEmpty()) {
                        Collection<ExecutionAction> executionActions = executionPlan.popExecutionActions();
                        for (ExecutionAction executionAction : executionActions) {
                            executionAction.start();
                        }
                        try {
                            Thread.sleep(100);
                        }
                        catch (InterruptedException exception) {
                            executor.getLogger().error("Execution interrupted.", exception);
                        }
                    }

                    // Set services as checked
                    for (cz.cesnet.shongo.controller.booking.executable.ExecutableService service : checkServices) {
                        executor.addCheckedExecutableService(service);

                        // Refresh service
                        entityManager.refresh(service);
                    }
                }
                entityManager.getTransaction().commit();
            }

            // Return services
            for (cz.cesnet.shongo.controller.booking.executable.ExecutableService service : services) {
                response.addItem(service.toApi());
            }
            return response;
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
        checkNotNull("executableId", executableId);
        checkNotNull("executableConfiguration", executableConfiguration);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ExecutableManager executableManager = new ExecutableManager(entityManager);
        ObjectIdentifier objectId = ObjectIdentifier.parse(executableId, ObjectType.EXECUTABLE);
        try {
            entityManager.getTransaction().begin();

            cz.cesnet.shongo.controller.booking.executable.Executable executable =
                    executableManager.get(objectId.getPersistenceId());
            if (!authorization.hasObjectPermission(securityToken, executable, ObjectPermission.READ)) {
                ControllerReportSetHelper.throwSecurityNotAuthorizedFault("read executable %s", objectId);
            }

            // Get participation
            Map<Long, AbstractParticipant> participants =
                    new HashMap<Long, AbstractParticipant>();
            if (executable instanceof RoomEndpoint) {
                RoomEndpoint roomEndpoint = (RoomEndpoint) executable;
                for (AbstractParticipant participant : roomEndpoint.getParticipants()) {
                    try {
                        participants.put(participant.getId(), participant.clone());
                    }
                    catch (CloneNotSupportedException exception) {
                        throw new RuntimeException(exception);
                    }
                }
            }

            if (executable.updateFromExecutableConfigurationApi(executableConfiguration, entityManager)) {
                if (executable.canBeModified()) {
                    executable.setModified(true);
                }
            }

            // Create participation notifications
            List<AbstractNotification> notifications = new LinkedList<AbstractNotification>();
            if (executable instanceof RoomEndpoint) {
                RoomEndpoint roomEndpoint = (RoomEndpoint) executable;
                if (roomEndpoint.isParticipantNotificationEnabled()) {
                    for (AbstractParticipant participant : roomEndpoint.getParticipants()) {
                        Long participantId = participant.getId();
                        if (participants.containsKey(participantId)) {
                            AbstractParticipant oldParticipant = participants.get(participantId);
                            RoomNotification.RoomModified roomModified =
                                    RoomNotification.RoomModified.create(roomEndpoint, oldParticipant, participant);
                            if (roomModified != null) {
                                notifications.add(roomModified);
                            }
                        }
                        else {
                            notifications.add(new RoomNotification.RoomCreated(roomEndpoint, participant));
                        }
                        participants.remove(participantId);
                    }
                    for (AbstractParticipant participant : participants.values()) {
                        notifications.add(new RoomNotification.RoomDeleted(roomEndpoint, participant));
                    }
                }
            }

            entityManager.getTransaction().commit();

            NotificationManager notificationManager = executor.getNotificationManager();
            notificationManager.addNotifications(notifications, entityManager);
        }
        finally {
            entityManager.close();
        }
    }

    @Override
    public void deleteExecutable(SecurityToken securityToken, String executableId)
    {
        authorization.validate(securityToken);
        checkNotNull("executableId", executableId);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ExecutableManager executableManager = new ExecutableManager(entityManager);
        AuthorizationManager authorizationManager = new AuthorizationManager(entityManager, authorization);
        ObjectIdentifier objectId = ObjectIdentifier.parse(executableId, ObjectType.EXECUTABLE);
        try {
            authorizationManager.beginTransaction();
            entityManager.getTransaction().begin();

            cz.cesnet.shongo.controller.booking.executable.Executable executable =
                    executableManager.get(objectId.getPersistenceId());

            if (!authorization.hasObjectPermission(securityToken, executable, ObjectPermission.WRITE)) {
                ControllerReportSetHelper.throwSecurityNotAuthorizedFault("delete executable %s", objectId);
            }

            executableManager.delete(executable, authorizationManager);

            entityManager.getTransaction().commit();
            authorizationManager.commitTransaction(securityToken);
        }
        catch (javax.persistence.RollbackException exception) {
            ControllerReportSetHelper.throwObjectNotDeletableReferencedFault(
                    cz.cesnet.shongo.controller.booking.executable.Executable.class, objectId.getPersistenceId());
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
    public void updateExecutable(SecurityToken securityToken, String executableId, Boolean skipExecution)
    {
        authorization.validate(securityToken);
        checkNotNull("executableId", executableId);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ExecutableManager executableManager = new ExecutableManager(entityManager);
        ObjectIdentifier objectId = ObjectIdentifier.parse(executableId, ObjectType.EXECUTABLE);
        try {
            entityManager.getTransaction().begin();

            cz.cesnet.shongo.controller.booking.executable.Executable executable =
                    executableManager.get(objectId.getPersistenceId());

            if (!authorization.hasObjectPermission(securityToken, executable, ObjectPermission.WRITE)) {
                ControllerReportSetHelper.throwSecurityNotAuthorizedFault("update executable %s", objectId);
            }

            Set<cz.cesnet.shongo.controller.booking.executable.Executable> executablesToUpdate =
                    new HashSet<cz.cesnet.shongo.controller.booking.executable.Executable>();
            executablesToUpdate.add(executable);
            cz.cesnet.shongo.controller.booking.executable.Executable migrateFromExecutable =
                    executable.getMigrateFromExecutable();
            if (migrateFromExecutable != null) {
                executablesToUpdate.add(migrateFromExecutable);
            }

            int maxAttemptCount = configuration.getInt(ControllerConfiguration.EXECUTOR_EXECUTABLE_MAX_ATTEMPT_COUNT);
            DateTime dateTimeNow = DateTime.now();
            for (cz.cesnet.shongo.controller.booking.executable.Executable executableToUpdate : executablesToUpdate) {
                cz.cesnet.shongo.controller.booking.executable.Executable.State executableState =
                        executableToUpdate.getState();
                // When executable slot is active or the executable is modified
                if (executableToUpdate.getSlot().contains(dateTimeNow) ||
                    (executableToUpdate.isModified() &&
                            cz.cesnet.shongo.controller.booking.executable.Executable.MODIFIABLE_STATES.contains(
                                    executableState))) {
                        if (Boolean.TRUE.equals(skipExecution)) {
                            throw new TodoImplementException("Skip execution for " + executableState + ".");
                        }
                        // When some attempt has been already made
                        if (executableToUpdate.getAttemptCount() > 0) {
                            // Schedule next attempt
                            if (executableToUpdate.getAttemptCount() >= maxAttemptCount) {
                                executableToUpdate.setAttemptCount(maxAttemptCount - 1);
                            }
                            executableToUpdate.setNextAttempt(dateTimeNow);
                        }
                }
                // When executable slot is in history
                else if (executableToUpdate.getSlotEnd().isBefore(dateTimeNow)) {
                    // When executable is started or failed to start, set it as stopped
                    if (executableState.isStarted() || executableState.equals(
                            cz.cesnet.shongo.controller.booking.executable.Executable.State.STARTING_FAILED)) {
                        if (Boolean.TRUE.equals(skipExecution)) {
                            executableToUpdate.setState(
                                    cz.cesnet.shongo.controller.booking.executable.Executable.State.STOPPED);
                            executableToUpdate.setAttemptCount(0);
                            executableToUpdate.setNextAttempt(null);
                        }
                    }
                    // If executable is not started
                    else if (executableState.equals(
                            cz.cesnet.shongo.controller.booking.executable.Executable.State.NOT_STARTED)) {
                        executableToUpdate.setState(
                                cz.cesnet.shongo.controller.booking.executable.Executable.State.STOPPED);
                    }
                    // If executable failed to finalize, set it as finalized or prepare the finalization again
                    else if (executableState.equals(
                            cz.cesnet.shongo.controller.booking.executable.Executable.State.FINALIZATION_FAILED)) {
                        if (Boolean.TRUE.equals(skipExecution)) {
                            executableToUpdate.setState(
                                    cz.cesnet.shongo.controller.booking.executable.Executable.State.FINALIZED);
                            executableToUpdate.setAttemptCount(0);
                            executableToUpdate.setNextAttempt(null);
                        }
                        else {
                            executableToUpdate.setState(
                                    cz.cesnet.shongo.controller.booking.executable.Executable.State.STOPPED);
                        }
                    }
                    // When execution should not be skipped
                    if (!Boolean.TRUE.equals(skipExecution)) {
                        // When some attempt has been already made
                        if (executableToUpdate.getAttemptCount() > 0) {
                            // Schedule next attempt
                            if (executableToUpdate.getAttemptCount() >= maxAttemptCount) {
                                executableToUpdate.setAttemptCount(maxAttemptCount - 1);
                            }
                            executableToUpdate.setNextAttempt(dateTimeNow);
                        }
                    }
                }
            }

            executable.updateExecutableSummary(entityManager, false);

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
        checkNotNull("roomExecutableId", roomExecutableId);
        checkNotNull("deviceRoomId", deviceRoomId);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ExecutableManager executableManager = new ExecutableManager(entityManager);
        ObjectIdentifier objectId = ObjectIdentifier.parse(roomExecutableId, ObjectType.EXECUTABLE);
        try {
            // Get and check room executable
            ResourceRoomEndpoint roomExecutable =
                    entityManager.find(ResourceRoomEndpoint.class, objectId.getPersistenceId());
            if (roomExecutable == null) {
                ControllerReportSetHelper.throwObjectNotExistFault(
                        cz.cesnet.shongo.controller.booking.executable.Executable.class, objectId.getPersistenceId());
                return;
            }
            DeviceResource deviceResource = roomExecutable.getResource();
            if (!authorization.hasObjectPermission(securityToken, roomExecutable, ObjectPermission.WRITE) ||
                    !authorization.hasObjectPermission(securityToken, deviceResource, ObjectPermission.WRITE)) {
                ControllerReportSetHelper.throwSecurityNotAuthorizedFault("attach room %s", objectId);
            }
            if (!roomExecutable.getState().equals(
                    cz.cesnet.shongo.controller.booking.executable.Executable.State.NOT_STARTED)) {
                throw new CommonReportSet.UnknownErrorException("Room executable must be NOT_STARTED.");
            }

            // Get and check device room
            Room deviceRoom = (Room) performDeviceCommand(deviceResource, new GetRoom(deviceRoomId));
            if (deviceRoom == null) {
                throw new CommonReportSet.UnknownErrorException("Room " + deviceRoomId + " doesn't exist.");
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
                        ObjectIdentifier.formatId(roomExecutables.get(0)) + ".");
            }

            // Attach device room to room executable
            entityManager.getTransaction().begin();
            roomExecutable.setState(cz.cesnet.shongo.controller.booking.executable.Executable.State.STARTED);
            roomExecutable.setModified(true);
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
    public Object activateExecutableService(SecurityToken securityToken, String executableId,
            String executableServiceId)
    {
        authorization.validate(securityToken);
        checkNotNull("executableId", executableId);
        checkNotNull("executableServiceId", executableServiceId);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ExecutableManager executableManager = new ExecutableManager(entityManager);
        ObjectIdentifier objectId;
        try {
            objectId = ObjectIdentifier.parse(executableId, ObjectType.EXECUTABLE);
        }
        //TODO: only for foreign recording, generalize for other services
        catch (ControllerReportSet.IdentifierInvalidDomainException ex) {
            objectId = ObjectIdentifier.parseForeignId(executableId);
            // Check if id is for foreign reservation request
            if (!ObjectType.RESERVATION_REQUEST.equals(objectId.getObjectType())) {
                throw ex;
            }

            DomainsConnector domainsConnector = InterDomainAgent.getInstance().getConnector();
            try {
                AbstractResponse response = domainsConnector.sendRoomAction(new StartRecording(), executableId, AbstractResponse.class);
                if (response.success()) {
                    return Boolean.TRUE;
                }
                else {
                    return Boolean.FALSE;
                }
            } catch (ForeignDomainConnectException e) {
                return Boolean.FALSE;
            }
        }

        try {
            cz.cesnet.shongo.controller.booking.executable.Executable executable =
                    executableManager.get(objectId.getPersistenceId());

            if (!authorization.hasObjectPermission(securityToken, executable, ObjectPermission.WRITE)) {
                ControllerReportSetHelper.throwSecurityNotAuthorizedFault("control executable %s", objectId);
            }

            cz.cesnet.shongo.controller.booking.executable.ExecutableService executableService =
                    executable.getServiceById(Long.parseLong(executableServiceId));
            if (executableService.isActive()) {
                return Boolean.FALSE;
            }

            entityManager.getTransaction().begin();

            synchronized (executor) {
                executableService.activate(executor, executableManager);
            }

            executableService.getExecutable().updateExecutableSummary(entityManager, false);

            entityManager.getTransaction().commit();

            // Reporting
            Reporter reporter = Reporter.getInstance();
            for (ExecutionReport executionReport : executableManager.getExecutionReports()) {
                reporter.report(executionReport.getExecutionTarget(), executionReport);
            }

            // Check activation failed
            if (!executableService.isActive()) {
                ExecutionReport executionReport = executableService.getLastReport();
                cz.cesnet.shongo.controller.api.ExecutionReport executionReportApi =
                        new cz.cesnet.shongo.controller.api.ExecutionReport(
                                authorization.isAdministrator(securityToken) ?
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
        checkNotNull("executableId", executableId);
        checkNotNull("executableServiceId", executableServiceId);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ExecutableManager executableManager = new ExecutableManager(entityManager);
        ObjectIdentifier objectId = ObjectIdentifier.parse(executableId, ObjectType.EXECUTABLE);
        try {
            Long persistenceId = objectId.getPersistenceId();
            cz.cesnet.shongo.controller.booking.executable.Executable executable =
                    executableManager.get(persistenceId);

            if (!authorization.hasObjectPermission(securityToken, executable, ObjectPermission.WRITE)) {
                ControllerReportSetHelper.throwSecurityNotAuthorizedFault("control executable %s", objectId);
            }

            cz.cesnet.shongo.controller.booking.executable.ExecutableService executableService =
                    executable.getServiceById(Long.parseLong(executableServiceId));
            if (!executableService.isActive()) {
                return Boolean.FALSE;
            }

            entityManager.getTransaction().begin();

            synchronized (executor) {
                executableService.deactivate(executor, executableManager);
            }

            executableService.getExecutable().updateExecutableSummary(entityManager, false);

            entityManager.getTransaction().commit();

            // Reporting
            Reporter reporter = Reporter.getInstance();
            for (ExecutionReport executionReport : executableManager.getExecutionReports()) {
                reporter.report(executionReport.getExecutionTarget(), executionReport);
            }

            // Check deactivation failed
            if (executableService.isActive()) {
                ExecutionReport executionReport = executableService.getLastReport();
                cz.cesnet.shongo.controller.api.ExecutionReport executionReportApi =
                        new cz.cesnet.shongo.controller.api.ExecutionReport(
                                authorization.isAdministrator(securityToken) ?
                                        Report.UserType.DOMAIN_ADMIN : Report.UserType.USER);
                executionReportApi.addReport(new StateReportSerializer(executionReport));
                return executionReportApi;
            }

            // Clear recordings cache (new recording should be fetched)
            recordingsCache.removeExecutableRecordings(executable.getId());
            if (executable instanceof UsedRoomEndpoint) {
                UsedRoomEndpoint usedRoomEndpoint = (UsedRoomEndpoint) executable;
                recordingsCache.removeExecutableRecordings(usedRoomEndpoint.getReusedRoomEndpoint().getId());
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
    public ListResponse<ResourceRecording> listExecutableRecordings(ExecutableRecordingListRequest request)
    {
        checkNotNull("request", request);
        SecurityToken securityToken = request.getSecurityToken();
        authorization.validate(securityToken);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ExecutableManager executableManager = new ExecutableManager(entityManager);
        ObjectIdentifier objectId = ObjectIdentifier.parse(request.getExecutableId(), ObjectType.EXECUTABLE);
        try {
            Long executableId = objectId.getPersistenceId();
            cz.cesnet.shongo.controller.booking.executable.Executable executable =
                    executableManager.get(executableId);

            if (!authorization.hasObjectPermission(securityToken, executable, ObjectPermission.READ)) {
                ControllerReportSetHelper.throwSecurityNotAuthorizedFault("read executable %s", objectId);
            }

            // Get reused executable
            while (executable instanceof UsedRoomEndpoint) {
                UsedRoomEndpoint usedRoomEndpoint = (UsedRoomEndpoint) executable;
                executable = usedRoomEndpoint.getReusedRoomEndpoint();
            }
            // Get most recent version of executable
            while (executable.getMigrateToExecutable() != null) {
                executable = executable.getMigrateToExecutable();
            }
            executableId = executable.getId();

            List<ResourceRecording> resourceRecordings = recordingsCache.getExecutableRecordings(executableId);
            if (resourceRecordings == null) {
                // Get all recording folders
                Map<RecordingCapability, String> recordingFolders;
                if (executable instanceof RecordableEndpoint) {
                    RecordableEndpoint recordableEndpoint = (RecordableEndpoint) executable;
                    recordingFolders = recordableEndpoint.getRecordingFolderIds();
                }
                else {
                    recordingFolders = Collections.emptyMap();
                }

                // Get all recordings from folders
                resourceRecordings = new LinkedList<ResourceRecording>();
                for (Map.Entry<RecordingCapability, String> entry : recordingFolders.entrySet()) {
                    RecordingCapability recordingCapability = entry.getKey();
                    DeviceResource recordingDeviceResource = recordingCapability.getDeviceResource();
                    String recordingDeviceResourceId = ObjectIdentifier.formatId(recordingDeviceResource);
                    String recordingFolderId = entry.getValue();
                    if (recordingFolderId == null) {
                        continue;
                    }
                    @SuppressWarnings("unchecked")
                    Collection<Recording> serviceRecordings = (Collection<Recording>) performDeviceCommand(
                            recordingDeviceResource, new ListRecordings(recordingFolderId));
                    for (Recording recording : serviceRecordings) {
                        resourceRecordings.add(
                                new ResourceRecording(recordingDeviceResourceId, recording));
                    }
                }
                recordingsCache.putExecutableRecordings(executableId, resourceRecordings);
            }

            ExecutableRecordingListRequest.Sort sort = request.getSort();
            if (sort != null) {
                List<ResourceRecording> sortedRecordings = new ArrayList<ResourceRecording>(resourceRecordings);
                Comparator<ResourceRecording> comparator;
                switch (sort) {
                    case NAME:
                        comparator = new Comparator<ResourceRecording>()
                        {
                            @Override
                            public int compare(ResourceRecording o1, ResourceRecording o2)
                            {
                                return o1.getName().compareTo(o2.getName());
                            }
                        };
                        break;
                    case START:
                        comparator = new Comparator<ResourceRecording>()
                        {
                            @Override
                            public int compare(ResourceRecording o1, ResourceRecording o2)
                            {
                                return o1.getBeginDate().compareTo(o2.getBeginDate());
                            }
                        };
                        break;
                    case DURATION:
                        comparator = new Comparator<ResourceRecording>()
                        {
                            @Override
                            public int compare(ResourceRecording o1, ResourceRecording o2)
                            {
                                return o1.getDuration().compareTo(o2.getDuration());
                            }
                        };
                        break;
                    default:
                        throw new TodoImplementException(sort);
                }
                if (request.getSortDescending()) {
                    comparator = Collections.reverseOrder(comparator);
                }
                Collections.sort(sortedRecordings, comparator);
                resourceRecordings = sortedRecordings;
            }

            return ListResponse.fromRequest(request, resourceRecordings);
        }
        finally {
            entityManager.close();
        }
    }

    private Object performDeviceCommand(DeviceResource deviceResource, Command command)
    {
        ManagedMode managedMode = deviceResource.requireManaged();
        String agentName = managedMode.getConnectorAgentName();
        SendLocalCommand sendLocalCommand = controllerAgent.sendCommand(agentName, command);
        if (!sendLocalCommand.getState().equals(SendLocalCommand.State.SUCCESSFUL)) {
            throw new ControllerReportSet.DeviceCommandFailedException(ObjectIdentifier.formatId(deviceResource),
                    command.toString(), sendLocalCommand.getJadeReport());
        }
        return sendLocalCommand.getResult();
    }
}
