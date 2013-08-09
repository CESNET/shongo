package cz.cesnet.shongo.controller.api.rpc;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.*;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.request.ExecutableListRequest;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.controller.executor.ExecutableManager;
import cz.cesnet.shongo.controller.executor.RoomEndpoint;
import cz.cesnet.shongo.controller.util.NativeQuery;
import cz.cesnet.shongo.controller.util.QueryFilter;
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
            QueryFilter queryFilter = new QueryFilter("executable_summary", true);

            // List only reservations which is current user permitted to read
            queryFilter.addIds(authorization, userId, EntityType.EXECUTABLE, Permission.READ);

            // If history executables should not be included
            if (!request.isIncludeHistory()) {
                // List only executables which are allocated by any existing reservation
                queryFilter.addFilter("executable_summary.id IN("
                        + "  SELECT reservation.executable_id FROM reservation)");
            }

            // List only executables of requested classes
            if (request.getExecutableClasses().size() > 0) {
                StringBuilder leftJoinBuilder = new StringBuilder();
                StringBuilder whereBuilder = new StringBuilder();
                for (Class<? extends Executable> type : request.getExecutableClasses()) {
                    leftJoinBuilder.append(" LEFT JOIN ");
                    if (whereBuilder.length() > 0) {
                        whereBuilder.append(" OR ");
                    }
                    if (type.equals(RoomExecutable.class)) {
                        leftJoinBuilder.append("room_endpoint ON room_endpoint.id");
                        whereBuilder.append("room_endpoint.id");
                    }
                    else {
                        throw new TodoImplementException(type);
                    }
                    leftJoinBuilder.append(" = executable.id");
                    whereBuilder.append(" IS NOT NULL");
                }
                StringBuilder filterBuilder = new StringBuilder();
                filterBuilder.append("executable_summary.id IN (");
                filterBuilder.append(" SELECT executable.id FROM executable");
                filterBuilder.append(leftJoinBuilder);
                filterBuilder.append(" WHERE ");
                filterBuilder.append(whereBuilder);
                filterBuilder.append(")");
                queryFilter.addFilter(filterBuilder.toString());
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
                        queryOrderBy = "executable_summary.slot_start";
                        break;
                    case STATE:
                        queryOrderBy = "executable_summary.state";
                        break;
                    case TECHNOLOGY:
                        queryOrderBy = "executable_summary.room_technologies";
                        break;
                    default:
                        throw new TodoImplementException(sort.toString());
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
            parameters.put("filter", queryFilter.toQueryWhere());
            parameters.put("order", queryOrderBy);
            String query = NativeQuery.getNativeQuery(NativeQuery.EXECUTABLE_LIST, parameters);

            ListResponse<ExecutableSummary> response = new ListResponse<ExecutableSummary>();
            List<Object[]> records = performNativeListRequest(query, queryFilter, request, response, entityManager);
            for (Object[] record : records) {
                ExecutableSummary executableSummary;

                String type = record[1].toString().trim();
                if (type.equals("ROOM")) {
                    executableSummary = new RoomExecutableSummary();
                }
                else if (type.equals("USED_ROOM")) {
                    executableSummary = new RoomExecutableSummary();
                }
                else {
                    executableSummary = new ExecutableSummary();
                }

                executableSummary.setId(EntityIdentifier.formatId(EntityType.EXECUTABLE, record[0].toString()));
                executableSummary.setSlot(new Interval(new DateTime(record[2]), new DateTime(record[3])));
                executableSummary.setState(
                        cz.cesnet.shongo.controller.executor.Executable.State.valueOf(record[4].toString()).toApi());

                if (executableSummary instanceof RoomExecutableSummary) {
                    RoomExecutableSummary roomExecutableSummary = (RoomExecutableSummary) executableSummary;
                    roomExecutableSummary.setName(record[5] != null ? record[5].toString() : null);
                    if (record[6] != null) {
                        String technologies = record[6].toString();
                        if (!technologies.isEmpty()) {
                            for (String technology : technologies.split(",")) {
                                roomExecutableSummary.addTechnology(Technology.valueOf(technology.trim()));
                            }
                        }
                    }
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
