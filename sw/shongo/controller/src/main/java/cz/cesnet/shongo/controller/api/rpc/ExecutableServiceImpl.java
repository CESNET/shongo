package cz.cesnet.shongo.controller.api.rpc;

import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.Room;
import cz.cesnet.shongo.connector.api.jade.multipoint.rooms.GetRoom;
import cz.cesnet.shongo.controller.*;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.Executable;
import cz.cesnet.shongo.controller.api.request.ExecutableListRequest;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.authorization.AclRecord;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.controller.executor.*;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.resource.ManagedMode;
import cz.cesnet.shongo.controller.util.NativeQuery;
import cz.cesnet.shongo.controller.util.QueryFilter;
import cz.cesnet.shongo.jade.SendLocalCommand;
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
    public void init(Configuration configuration)
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
                        cz.cesnet.shongo.controller.executor.Executable.class, request.getRoomId()));
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
                executableSummary.setState(
                        cz.cesnet.shongo.controller.executor.Executable.State.valueOf(record[4].toString()).toApi());

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
                                    cz.cesnet.shongo.controller.executor.Executable.State.valueOf(
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
            cz.cesnet.shongo.controller.executor.Executable executable =
                    executableManager.get(entityId.getPersistenceId());

            if (!authorization.hasPermission(securityToken, executable, Permission.READ)) {
                ControllerReportSetHelper.throwSecurityNotAuthorizedFault("read executable %s", entityId);
            }

            Executable executableApi = executable.toApi(authorization.isAdmin(securityToken));
            cz.cesnet.shongo.controller.reservation.Reservation reservation =
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

            cz.cesnet.shongo.controller.executor.Executable executable =
                    executableManager.get(entityId.getPersistenceId());

            if (!authorization.hasPermission(securityToken, executable, Permission.READ)) {
                ControllerReportSetHelper.throwSecurityNotAuthorizedFault("read executable %s", entityId);
            }
            if (executable.updateFromExecutableConfigurationApi(executableConfiguration, entityManager)) {
                cz.cesnet.shongo.controller.executor.Executable.State executableState = executable.getState();
                if (executableState.equals(cz.cesnet.shongo.controller.executor.Executable.State.STARTED)) {
                    executable.setState(cz.cesnet.shongo.controller.executor.Executable.State.MODIFIED);
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

            cz.cesnet.shongo.controller.executor.Executable executable =
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
                    cz.cesnet.shongo.controller.executor.Executable.class, entityId.getPersistenceId());
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

            cz.cesnet.shongo.controller.executor.Executable executable =
                    executableManager.get(entityId.getPersistenceId());

            if (!authorization.hasPermission(securityToken, executable, Permission.WRITE)) {
                ControllerReportSetHelper.throwSecurityNotAuthorizedFault("update executable %s", entityId);
            }

            Set<cz.cesnet.shongo.controller.executor.Executable> executablesToUpdate =
                    new HashSet<cz.cesnet.shongo.controller.executor.Executable>();
            executablesToUpdate.add(executable);
            cz.cesnet.shongo.controller.executor.Migration migration = executable.getMigration();
            if (migration != null) {
                executablesToUpdate.add(migration.getSourceExecutable());
                executablesToUpdate.add(migration.getTargetExecutable());
            }

            int maxAttemptCount = getConfiguration().getInt(Configuration.EXECUTOR_EXECUTABLE_MAX_ATTEMPT_COUNT);
            DateTime dateTimeNow = DateTime.now();
            for (cz.cesnet.shongo.controller.executor.Executable executableToUpdate : executablesToUpdate) {
                if (executableToUpdate.getSlot().contains(dateTimeNow)) {
                    // Schedule next attempt
                    if (executableToUpdate.getAttemptCount() >= maxAttemptCount) {
                        executableToUpdate.setAttemptCount(maxAttemptCount - 1);
                    }
                    executableToUpdate.setNextAttempt(dateTimeNow);
                }
                else if (executableToUpdate.getSlotEnd().isBefore(dateTimeNow)) {
                    // Set executable as stopped
                    cz.cesnet.shongo.controller.executor.Executable.State state = executableToUpdate.getState();
                    if (state.isStarted() ||
                            state.equals(cz.cesnet.shongo.controller.executor.Executable.State.STARTING_FAILED)) {
                        executableToUpdate.setState(cz.cesnet.shongo.controller.executor.Executable.State.STOPPED);
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
            if (!roomExecutable.getState().equals(cz.cesnet.shongo.controller.executor.Executable.State.NOT_STARTED)) {
                throw new CommonReportSet.UnknownErrorException("Room executable must be NOT_STARTED.");
            }
            ManagedMode managedMode = (ManagedMode) deviceResource.getMode();
            String agentName = managedMode.getConnectorAgentName();

            // Get and check device room
            GetRoom deviceAction = new GetRoom(deviceRoomId);
            SendLocalCommand sendLocalCommand = controllerAgent.sendCommand(agentName, deviceAction);
            if (!sendLocalCommand.getState().equals(SendLocalCommand.State.SUCCESSFUL)) {
                throw new ControllerReportSet.DeviceCommandFailedException(EntityIdentifier.formatId(deviceResource),
                        deviceAction.toString(), sendLocalCommand.getJadeReport());
            }
            Room deviceRoom = (Room) sendLocalCommand.getResult();
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
            roomExecutable.setState(cz.cesnet.shongo.controller.executor.Executable.State.MODIFIED);
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
}
