package cz.cesnet.shongo.controller.api.rpc;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.*;
import cz.cesnet.shongo.controller.api.Executable;
import cz.cesnet.shongo.controller.api.ExecutableSummary;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.request.ExecutableListRequest;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.controller.executor.ExecutableManager;
import cz.cesnet.shongo.controller.util.NativeQuery;
import cz.cesnet.shongo.controller.util.QueryFilter;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

            if (!authorization.hasPermission(securityToken, entityId, Permission.WRITE)) {
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
        SecurityToken securityToken = request.getSecurityToken();
        authorization.validate(securityToken);
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            QueryFilter queryFilter = new QueryFilter("executable_summary", true);

            // List only reservations which is current user permitted to read
            queryFilter.addIds(authorization, securityToken, EntityType.EXECUTABLE, Permission.READ);

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

            if (!authorization.hasPermission(securityToken, entityId, Permission.READ)) {
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

            if (!authorization.hasPermission(securityToken, entityId, Permission.WRITE)) {
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
