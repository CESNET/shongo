package cz.cesnet.shongo.controller.api.rpc;

import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.*;
import cz.cesnet.shongo.controller.api.AclRecord;
import cz.cesnet.shongo.controller.api.PermissionSet;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.request.AclRecordListRequest;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.api.request.PermissionListRequest;
import cz.cesnet.shongo.controller.api.request.UserListRequest;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.controller.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.request.Allocation;
import cz.cesnet.shongo.controller.request.ReservationRequest;
import cz.cesnet.shongo.controller.resource.Resource;
import cz.cesnet.shongo.controller.util.QueryFilter;
import cz.cesnet.shongo.util.StringHelper;
import org.apache.commons.lang.StringUtils;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.*;

/**
 * Implementation of {@link AuthorizationService}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AuthorizationServiceImpl extends AbstractServiceImpl
        implements AuthorizationService, Component.EntityManagerFactoryAware, Component.AuthorizationAware
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
        return "Authorization";
    }

    /**
     * @param entityId of entity which should be checked for existence
     * @throws CommonReportSet.EntityNotFoundException
     *
     */
    private void checkEntityExistence(EntityIdentifier entityId) throws CommonReportSet.EntityNotFoundException
    {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            PersistentObject entity = entityManager.find(entityId.getEntityClass(), entityId.getPersistenceId());
            if (entity == null) {
                ControllerReportSetHelper.throwEntityNotFoundFault(entityId);
            }
        }
        finally {
            entityManager.close();
        }
    }

    @Override
    public String createAclRecord(SecurityToken token, String userId, String entityId, Role role)
    {
        String requesterUserId = authorization.validate(token);
        authorization.checkUserExistence(userId);
        EntityIdentifier entityIdentifier = EntityIdentifier.parse(entityId);
        checkEntityExistence(entityIdentifier);
        if (!authorization.hasPermission(requesterUserId, entityIdentifier, Permission.WRITE)) {
            ControllerReportSetHelper.throwSecurityNotAuthorizedFault("create ACL for %s", entityId);
        }
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        AuthorizationManager authorizationManager = new AuthorizationManager(entityManager);
        try {
            authorizationManager.beginTransaction(authorization);
            entityManager.getTransaction().begin();
            cz.cesnet.shongo.controller.authorization.AclRecord aclRecord =
                    authorizationManager.createAclRecord(userId, entityIdentifier, role);
            entityManager.getTransaction().commit();
            authorizationManager.commitTransaction();
            return (aclRecord != null ? aclRecord.getId().toString() : null);
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
    public void deleteAclRecord(SecurityToken token, String aclRecordId)
    {
        String userId = authorization.validate(token);
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        AuthorizationManager authorizationManager = new AuthorizationManager(entityManager);
        try {
            cz.cesnet.shongo.controller.authorization.AclRecord aclRecord =
                    authorizationManager.getAclRecord(Long.valueOf(aclRecordId));
            if (!authorization.hasPermission(userId, aclRecord.getEntityId(), Permission.WRITE)) {
                ControllerReportSetHelper.throwSecurityNotAuthorizedFault("delete ACL for %s", aclRecord.getEntityId());
            }
            authorizationManager.beginTransaction(authorization);
            entityManager.getTransaction().begin();
            authorizationManager.deleteAclRecord(aclRecord);
            entityManager.getTransaction().commit();
            authorizationManager.commitTransaction();
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
    public ListResponse<AclRecord> listAclRecords(AclRecordListRequest request)
    {
        String requesterUserId = authorization.validate(request.getSecurityToken());

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            QueryFilter queryFilter = new QueryFilter("acl_record", true);
            queryFilter.addFilter("acl_record.deleted = FALSE");

            // List only records which are requested
            if (request.getAclRecordIds().size() > 0) {
                queryFilter.addFilter("acl_record.id IN (:aclRecordIds)");
                Set<Long> aclRecordIds = new HashSet<Long>();
                for (String aclRecordId : request.getAclRecordIds()) {
                    aclRecordIds.add(Long.valueOf(aclRecordId));
                }
                queryFilter.addFilterParameter("aclRecordIds", aclRecordIds);
            }

            // List only records which are requested
            if (request.getEntityIds().size() > 0) {
                boolean isAdmin = authorization.isAdmin(requesterUserId);
                StringBuilder entityIdsFilterBuilder = new StringBuilder();
                entityIdsFilterBuilder.append("1=1");
                for (String entityId : request.getEntityIds()) {
                    EntityIdentifier entityIdentifier = EntityIdentifier.parse(entityId);
                    boolean isGroup = entityIdentifier.isGroup();

                    // Check entity existence
                    if (!isGroup) {
                        checkEntityExistence(entityIdentifier);
                    }

                    // Check permission for listing
                    if (!isAdmin) {
                        if (isGroup) {
                            throw new TodoImplementException("List only ACL to which the requester has permission.");
                        }
                        else {
                            if (!authorization.hasPermission(requesterUserId, entityIdentifier, Permission.READ)) {
                                ControllerReportSetHelper.throwSecurityNotAuthorizedFault("list ACL for %s", entityId);
                            }
                        }
                    }

                    EntityType entityType = entityIdentifier.getEntityType();
                    Long persistenceId = entityIdentifier.getPersistenceId();
                    StringBuilder entityIdFilterBuilder = new StringBuilder();
                    if (entityType != null) {
                        entityIdFilterBuilder.append("acl_record.entity_type = '");
                        entityIdFilterBuilder.append(entityType.toString());
                        entityIdFilterBuilder.append("'");
                    }
                    if (persistenceId != null) {
                        if (entityIdFilterBuilder.length() > 0) {
                            entityIdFilterBuilder.append(" AND ");
                        }
                        entityIdFilterBuilder.append("acl_record.entity_id = ");
                        entityIdFilterBuilder.append(persistenceId);
                    }
                    if (entityIdFilterBuilder.length() > 0) {
                        entityIdsFilterBuilder.append(" AND (");
                        entityIdsFilterBuilder.append(entityIdFilterBuilder);
                        entityIdsFilterBuilder.append(")");
                    }
                }
                queryFilter.addFilter(entityIdsFilterBuilder.toString());
            }

            // List only records for requested users
            if (request.getUserIds().size() > 0) {
                queryFilter.addFilter("acl_record.user_id IN (:userIds)");
                queryFilter.addFilterParameter("userIds", request.getUserIds());
            }

            // List only records for requested roles
            if (request.getRoles().size() > 0) {
                queryFilter.addFilter("acl_record.role IN (:roles)");
                queryFilter.addFilterParameter("roles", request.getRoles());
            }

            // Query
            String query = "SELECT "
                    + " acl_record.id,"
                    + " acl_record.user_id,"
                    + " acl_record.entity_type,"
                    + " acl_record.entity_id,"
                    + " acl_record.role,"
                    + " COUNT(acl_record_dependency.id)"
                    + " FROM acl_record"
                    + " LEFT JOIN acl_record_dependency ON acl_record_dependency.child_acl_record_id = acl_record.id"
                    + " WHERE " + queryFilter.toQueryWhere()
                    + " GROUP BY acl_record.id, acl_record.user_id, acl_record.entity_type, "
                    + "          acl_record.entity_id, acl_record.role";

            ListResponse<AclRecord> response = new ListResponse<AclRecord>();
            List<Object[]> aclRecords = performNativeListRequest(query, queryFilter, request, response, entityManager);

            // Fill reservations to response
            for (Object[] aclRecord : aclRecords) {
                AclRecord aclRecordApi = new AclRecord();
                aclRecordApi.setId(aclRecord[0].toString());
                aclRecordApi.setUserId(aclRecord[1].toString());
                aclRecordApi.setEntityId(new EntityIdentifier(
                        EntityType.valueOf(aclRecord[2].toString()), ((Number) aclRecord[3]).longValue()).toId());
                aclRecordApi.setRole(Role.valueOf(aclRecord[4].toString()));
                aclRecordApi.setDeletable(((Number) aclRecord[5]).intValue() == 0);
                response.addItem(aclRecordApi);
            }
            return response;
        }
        finally {
            entityManager.close();
        }
    }

    @Override
    public Map<String, PermissionSet> listPermissions(PermissionListRequest request)
    {
        String userId = authorization.validate(request.getSecurityToken());
        Map<String, PermissionSet> response = new HashMap<String, PermissionSet>();
        for (String entityId : request.getEntityIds()) {
            EntityIdentifier entityIdentifier = EntityIdentifier.parse(entityId);
            checkEntityExistence(entityIdentifier);
            response.put(entityId, new PermissionSet(authorization.getPermissions(userId, entityIdentifier)));
        }
        return response;
    }

    @Override
    public ListResponse<UserInformation> listUsers(UserListRequest request)
    {
        authorization.validate(request.getSecurityToken());

        // Get users
        Set<String> userIds = request.getUserIds();
        if (userIds.size() == 0) {
            userIds = null;
        }
        List<UserInformation> users = new LinkedList<UserInformation>();
        if (userIds != null && userIds.size() < 3) {
            for (String userId : userIds) {
                users.add(authorization.getUserInformation(userId));
            }
        }
        else {
            for (UserInformation userInformation : authorization.listUserInformation()) {
                // Filter by user-id
                if (userIds != null) {
                    if (!userIds.contains(userInformation.getUserId())) {
                        continue;
                    }
                }
                users.add(userInformation);
            }
        }

        // Filter them
        String filter = StringHelper.removeAccents(request.getFilter());
        if (filter != null) {
            for (Iterator<UserInformation> iterator = users.iterator(); iterator.hasNext(); ) {
                UserInformation userInformation = iterator.next();

                // Filter by data
                StringBuilder filterData = new StringBuilder();
                filterData.append(userInformation.getFirstName());
                filterData.append(" ");
                filterData.append(userInformation.getLastName());
                for (String email : userInformation.getEmails()) {
                    filterData.append(email);
                }
                filterData.append(userInformation.getOrganization());
                if (!StringUtils.containsIgnoreCase(StringHelper.removeAccents(filterData.toString()), filter)) {
                    iterator.remove();
                }
            }
        }

        int start = request.getStart(0);
        int end = start + request.getCount(users.size() - start);
        ListResponse<UserInformation> response = new ListResponse<UserInformation>();
        response.setStart(start);
        response.setCount(end - start);
        for (UserInformation userInformation : users.subList(start, end)) {
            response.addItem(userInformation);
        }
        return response;
    }

    @Override
    public void setEntityUser(SecurityToken token, String entityId, String newUserId)
    {
        String userId = authorization.validate(token);
        authorization.checkUserExistence(newUserId);
        EntityIdentifier entityIdentifier = EntityIdentifier.parse(entityId);
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        AuthorizationManager authorizationManager = new AuthorizationManager(entityManager);
        try {
            PersistentObject entity = entityManager.find(entityIdentifier.getEntityClass(),
                    entityIdentifier.getPersistenceId());
            if (entity == null) {
                ControllerReportSetHelper.throwEntityNotFoundFault(entityIdentifier);
            }
            if (!authorization.isAdmin(userId)) {
                ControllerReportSetHelper.throwSecurityNotAuthorizedFault("change user for %s", entityId);
            }
            authorizationManager.beginTransaction(authorization);
            entityManager.getTransaction().begin();
            if (entity instanceof Resource) {
                Resource resource = (Resource) entity;
                for (cz.cesnet.shongo.controller.authorization.AclRecord aclRecord :
                        authorizationManager.listAclRecords(resource.getUserId(), entityIdentifier, Role.OWNER)) {
                    authorizationManager.deleteAclRecord(aclRecord);
                }
                resource.setUserId(newUserId);
                authorizationManager.createAclRecord(newUserId, entityIdentifier, Role.OWNER);
            }
            else if (entity instanceof AbstractReservationRequest) {
                // Change user to reservation request
                ReservationRequest reservationRequest = (ReservationRequest) entity;
                for (cz.cesnet.shongo.controller.authorization.AclRecord aclRecord :
                        authorizationManager.listAclRecords(reservationRequest.getCreatedBy(),
                                entityIdentifier, Role.OWNER)) {
                    authorizationManager.deleteAclRecord(aclRecord);
                }
                reservationRequest.setCreatedBy(newUserId);
                reservationRequest.setUpdatedBy(newUserId);
                authorizationManager.createAclRecord(newUserId, entityIdentifier, Role.OWNER);

                // Change user to child reservation requests
                Allocation allocation = reservationRequest.getAllocation();
                for (ReservationRequest childReservationRequest : allocation.getChildReservationRequests()) {
                    EntityIdentifier reservationRequestId = new EntityIdentifier(childReservationRequest);
                    for (cz.cesnet.shongo.controller.authorization.AclRecord aclRecord :
                            authorizationManager.listAclRecords(childReservationRequest.getCreatedBy(),
                                    reservationRequestId, Role.OWNER)) {
                        authorizationManager.deleteAclRecord(aclRecord);
                    }
                    childReservationRequest.setCreatedBy(newUserId);
                    authorizationManager.createAclRecord(newUserId, reservationRequestId, Role.OWNER);
                }
            }
            else {
                throw new RuntimeException("The user cannot be set for entity of type "
                        + entity.getClass().getSimpleName() + ".");
            }
            entityManager.getTransaction().commit();
            authorizationManager.commitTransaction();
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
}
