package cz.cesnet.shongo.controller.api.rpc;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.*;
import cz.cesnet.shongo.controller.api.Executable;
import cz.cesnet.shongo.controller.api.ExecutableSummary;
import cz.cesnet.shongo.controller.api.RoomExecutableSummary;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.request.ExecutableListRequest;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.controller.executor.ExecutableManager;
import cz.cesnet.shongo.controller.executor.RoomEndpoint;
import cz.cesnet.shongo.controller.util.DatabaseFilter;
import org.joda.time.DateTime;

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
                   Component.AuthorizationAware
{
    /**
     * @see javax.persistence.EntityManagerFactory
     */
    private EntityManagerFactory entityManagerFactory;

    /**
     * @see cz.cesnet.shongo.controller.authorization.Authorization
     */
    private Authorization authorization;

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
    public void deleteExecutable(SecurityToken token, String executableId)
    {
        String userId = authorization.validate(token);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ExecutableManager executableManager = new ExecutableManager(entityManager);
        AuthorizationManager authorizationManager = new AuthorizationManager(entityManager);
        EntityIdentifier entityId = EntityIdentifier.parse(executableId, EntityType.EXECUTABLE);

        try {
            authorizationManager.beginTransaction(authorization);
            entityManager.getTransaction().begin();

            cz.cesnet.shongo.controller.executor.Executable executable =
                    executableManager.get(entityId.getPersistenceId());

            if (!authorization.hasPermission(userId, entityId, Permission.WRITE)) {
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
    public ListResponse<ExecutableSummary> listExecutables(ExecutableListRequest request)
    {
        String userId = authorization.validate(request.getSecurityToken());
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            DatabaseFilter filter = new DatabaseFilter("executable");

            // List only reservations which is current user permitted to read
            filter.addIds(authorization, userId, EntityType.EXECUTABLE, Permission.READ);

            // List only executables which are allocated
            filter.addFilter("executable.state NOT IN(:notAllocatedStates)");
            filter.addFilterParameter("notAllocatedStates",
                    cz.cesnet.shongo.controller.executor.Executable.NOT_ALLOCATED_STATES);

            // List only top executables
            filter.addFilter("executable NOT IN("
                    + "  SELECT childExecutable FROM Executable executable"
                    + "  INNER JOIN executable.childExecutables childExecutable"
                    + ")");

            // If history executables should not be included
            if (!request.isIncludeHistory()) {
                // List only executables which are allocated by any existing reservation
                filter.addFilter("executable IN("
                        + "  SELECT executable FROM Reservation reservation"
                        + "  INNER JOIN reservation.executable executable"
                        + ")");
            }

            // List only executables of requested classes
            Set<Class<? extends Executable>> executableApiClasses = request.getExecutableClasses();
            if (executableApiClasses.size() > 0) {
                filter.addFilter("TYPE(executable) IN(:classes)");
                Set<Class<? extends cz.cesnet.shongo.controller.executor.Executable>> executableClasses =
                        new HashSet<Class<? extends cz.cesnet.shongo.controller.executor.Executable>>();
                for (Class<? extends Executable> executableApiClass : executableApiClasses) {
                    executableClasses.addAll(
                            cz.cesnet.shongo.controller.executor.Executable.getClassesFromApi(executableApiClass));
                }
                filter.addFilterParameter("classes", executableClasses);
            }

            // Sort query part
            String queryOrderBy;
            ExecutableListRequest.Sort sort = request.getSort();
            if (sort != null) {
                switch (sort) {
                    case SLOT:
                        queryOrderBy = "executable.slotStart";
                        break;
                    default:
                        throw new TodoImplementException(sort.toString());
                }
            }
            else {
                queryOrderBy = "executable.id";
            }
            Boolean sortDescending = request.getSortDescending();
            sortDescending = (sortDescending != null ? sortDescending : false);
            if (sortDescending) {
                queryOrderBy = queryOrderBy + " DESC";
            }

            ListResponse<ExecutableSummary> response = new ListResponse<ExecutableSummary>();
            List<cz.cesnet.shongo.controller.executor.Executable> executables = performListRequest(
                    "executable", "executable", cz.cesnet.shongo.controller.executor.Executable.class,
                    "Executable executable", queryOrderBy, filter, request, response, entityManager);

            // Fill executables to response
            Map<Long, RoomExecutableSummary> roomExecutableSummaryById = new HashMap<Long, RoomExecutableSummary>();
            for (cz.cesnet.shongo.controller.executor.Executable executable : executables) {
                Long executableId = executable.getId();
                ExecutableSummary executableSummary;
                if (executable instanceof RoomEndpoint) {
                    RoomExecutableSummary roomExecutableSummary = new RoomExecutableSummary();
                    roomExecutableSummaryById.put(executableId, roomExecutableSummary);
                    executableSummary = roomExecutableSummary;
                }
                else {
                    executableSummary = new ExecutableSummary();
                }
                executableSummary.setId(EntityIdentifier.formatId(EntityType.EXECUTABLE, executableId));
                executableSummary.setSlot(executable.getSlot());
                executableSummary.setState(executable.getState().toApi());
                response.addItem(executableSummary);
            }

            // Fill technologies
            Set<Long> roomIds = roomExecutableSummaryById.keySet();
            if (roomIds.size() > 0) {
                // Fill technologies
                List<Object[]> roomTechnologies = entityManager.createQuery(""
                        + "SELECT room.id, technology"
                        + " FROM RoomEndpoint room"
                        + " LEFT JOIN room.roomConfiguration.technologies technology"
                        + " WHERE room.id IN(:roomIds) AND technology != null",
                        Object[].class)
                        .setParameter("roomIds", roomIds)
                        .getResultList();
                for (Object[] roomTechnology : roomTechnologies) {
                    Long roomId = (Long) roomTechnology[0];
                    Technology technology = (Technology) roomTechnology[1];
                    RoomExecutableSummary roomExecutableSummary = roomExecutableSummaryById.get(roomId);
                    roomExecutableSummary.addTechnology(technology);
                }

                // Fill room names
                List<Object[]> roomNames = entityManager.createQuery(""
                        + "SELECT room.id, alias.value"
                        + " FROM ResourceRoomEndpoint room"
                        + " LEFT JOIN room.assignedAliases alias"
                        + " WHERE room.id IN(:roomIds) AND alias.type = :roomName",
                        Object[].class)
                        .setParameter("roomIds", roomIds)
                        .setParameter("roomName", AliasType.ROOM_NAME)
                        .getResultList();
                for (Object[] roomName : roomNames) {
                    Long roomId = (Long) roomName[0];
                    String name = (String) roomName[1];
                    RoomExecutableSummary roomExecutableSummary = roomExecutableSummaryById.get(roomId);
                    roomExecutableSummary.setName(name);
                }

                // Fill used room names
                List<Object[]> usedRoomNames = entityManager.createQuery(""
                        + "SELECT room.id, alias.value"
                        + " FROM UsedRoomEndpoint room"
                        + " LEFT JOIN room.roomEndpoint.assignedAliases alias"
                        + " WHERE room.id IN(:roomIds) AND alias.type = :roomName",
                        Object[].class)
                        .setParameter("roomIds", roomIds)
                        .setParameter("roomName", AliasType.ROOM_NAME)
                        .getResultList();
                for (Object[] usedRoomName : usedRoomNames) {
                    Long roomId = (Long) usedRoomName[0];
                    String name = (String) usedRoomName[1];
                    RoomExecutableSummary roomExecutableSummary = roomExecutableSummaryById.get(roomId);
                    roomExecutableSummary.setName(name);
                }
            }
            return response;
        }
        finally {
            entityManager.close();
        }
    }

    @Override
    public cz.cesnet.shongo.controller.api.Executable getExecutable(SecurityToken token, String executableId)
    {
        String userId = authorization.validate(token);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ExecutableManager executableManager = new ExecutableManager(entityManager);
        EntityIdentifier entityId = EntityIdentifier.parse(executableId, EntityType.EXECUTABLE);

        try {
            cz.cesnet.shongo.controller.executor.Executable executable =
                    executableManager.get(entityId.getPersistenceId());

            if (!authorization.hasPermission(userId, entityId, Permission.READ)) {
                ControllerReportSetHelper.throwSecurityNotAuthorizedFault("read executable %s", entityId);
            }

            Executable executableApi = executable.toApi(authorization.isAdmin(userId));
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
    public void updateExecutable(SecurityToken token, String executableId)
    {
        String userId = authorization.validate(token);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ExecutableManager executableManager = new ExecutableManager(entityManager);
        EntityIdentifier entityId = EntityIdentifier.parse(executableId, EntityType.EXECUTABLE);

        try {
            entityManager.getTransaction().begin();
            cz.cesnet.shongo.controller.executor.Executable executable =
                    executableManager.get(entityId.getPersistenceId());

            if (!authorization.hasPermission(userId, entityId, Permission.WRITE)) {
                ControllerReportSetHelper.throwSecurityNotAuthorizedFault("start executable %s", entityId);
            }

            executable.setNextAttempt(DateTime.now());

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
