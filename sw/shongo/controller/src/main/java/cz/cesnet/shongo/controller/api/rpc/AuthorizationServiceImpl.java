package cz.cesnet.shongo.controller.api.rpc;

import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.*;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.request.AclRecordListRequest;
import cz.cesnet.shongo.controller.api.request.EntityPermissionListRequest;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.api.request.UserListRequest;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.booking.EntityIdentifier;
import cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.booking.Allocation;
import cz.cesnet.shongo.controller.booking.request.ReservationRequest;
import cz.cesnet.shongo.controller.booking.resource.Resource;
import cz.cesnet.shongo.controller.settings.UserSettingsManager;
import cz.cesnet.shongo.controller.util.NativeQuery;
import cz.cesnet.shongo.controller.util.QueryFilter;
import cz.cesnet.shongo.util.StringHelper;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
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
    public void init(ControllerConfiguration configuration)
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

    @Override
    public ListResponse<UserInformation> listUsers(UserListRequest request)
    {
        authorization.validate(request.getSecurityToken());

        String search = StringHelper.removeAccents(request.getSearch());

        List<UserInformation> users = new LinkedList<UserInformation>();

        String principalName = request.getPrincipalName();
        if (principalName != null) {
            users.add(authorization.getUserInformationByPrincipalName(principalName));
        }
        else {
            // Get user-ids
            Set<String> userIds = request.getUserIds();
            if (userIds.size() == 0) {
                userIds = null;
            }

            // Update user-ids by groups
            Set<String> groupIds = request.getGroupIds();
            if (groupIds.size() > 0) {
                Set<String> groupsUserIds = new HashSet<String>();
                for (String groupId : groupIds) {
                    groupsUserIds.addAll(authorization.listGroupUserIds(groupId));
                }
                if (userIds != null) {
                    userIds.retainAll(groupsUserIds);
                }
                else {
                    userIds = groupsUserIds;
                }
            }

            // Get users
            if (userIds != null && userIds.size() < 3) {
                for (String userId : userIds) {
                    users.add(authorization.getUserInformation(userId));
                }
                // Filter them
                if (search != null) {
                    UserInformation.filter(users, search);
                }
            }
            else {
                for (UserInformation userInformation : authorization.listUserInformation(search)) {
                    // Filter by user-id
                    if (userIds != null) {
                        if (!userIds.contains(userInformation.getUserId())) {
                            continue;
                        }
                    }
                    users.add(userInformation);
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
    public List<Group> listGroups(SecurityToken token)
    {
        authorization.validate(token);
        if (!authorization.isAdmin(token)) {
            ControllerReportSetHelper.throwSecurityNotAuthorizedFault("list user groups");
        }
        return authorization.listGroups();
    }

    @Override
    public String createGroup(SecurityToken token, Group group)
    {
        authorization.validate(token);
        if (!authorization.isAdmin(token)) {
            ControllerReportSetHelper.throwSecurityNotAuthorizedFault("create group");
        }
        return authorization.createGroup(group);
    }

    @Override
    public void deleteGroup(SecurityToken token, String groupId)
    {
        authorization.validate(token);
        if (!authorization.isAdmin(token)) {
            ControllerReportSetHelper.throwSecurityNotAuthorizedFault("delete group " + groupId);
        }
        authorization.deleteGroup(groupId);
    }

    @Override
    public void addGroupUser(SecurityToken token, String groupId, String userId)
    {
        authorization.validate(token);
        if (!authorization.isAdmin(token)) {
            ControllerReportSetHelper.throwSecurityNotAuthorizedFault("add user to group " + groupId);
        }
        authorization.addGroupUser(groupId, userId);
    }

    @Override
    public void removeGroupUser(SecurityToken token, String groupId, String userId)
    {
        authorization.validate(token);
        if (!authorization.isAdmin(token)) {
            ControllerReportSetHelper.throwSecurityNotAuthorizedFault("remove user from group " + groupId);
        }
        authorization.removeGroupUser(groupId, userId);
    }

    @Override
    public String createAclRecord(SecurityToken securityToken, String userId, String entityId, EntityRole entityRole)
    {
        authorization.validate(securityToken);
        authorization.checkUserExistence(userId);
        EntityIdentifier entityIdentifier = EntityIdentifier.parse(entityId);
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        AuthorizationManager authorizationManager = new AuthorizationManager(entityManager, authorization);
        try {
            PersistentObject entity = checkEntityExistence(entityIdentifier, entityManager);
            if (!authorization.hasEntityPermission(securityToken, entity, EntityPermission.WRITE)) {
                ControllerReportSetHelper.throwSecurityNotAuthorizedFault("create ACL for %s", entityId);
            }
            authorizationManager.beginTransaction();
            entityManager.getTransaction().begin();
            cz.cesnet.shongo.controller.authorization.AclRecord aclRecord =
                    authorizationManager.createAclRecord(userId, entity, entityRole);
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
    public void deleteAclRecord(SecurityToken securityToken, String aclRecordId)
    {
        authorization.validate(securityToken);
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        AuthorizationManager authorizationManager = new AuthorizationManager(entityManager, authorization);
        try {
            cz.cesnet.shongo.controller.authorization.AclRecord aclRecord =
                    authorizationManager.getAclRecord(Long.valueOf(aclRecordId));
            if (!authorization.hasEntityPermission(securityToken, aclRecord.getEntityId(), EntityPermission.WRITE)) {
                ControllerReportSetHelper.throwSecurityNotAuthorizedFault("delete ACL for %s", aclRecord.getEntityId());
            }
            authorizationManager.beginTransaction();
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
        SecurityToken securityToken = request.getSecurityToken();
        authorization.validate(securityToken);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            QueryFilter queryFilter = new QueryFilter("acl_record", true);

            // List only records which are requested
            if (request.getAclRecordIds().size() > 0) {
                queryFilter.addFilter("acl_record.id IN (:aclRecordIds)");
                Set<Long> aclRecordIds = new HashSet<Long>();
                for (String aclRecordId : request.getAclRecordIds()) {
                    aclRecordIds.add(Long.valueOf(aclRecordId));
                }
                queryFilter.addFilterParameter("aclRecordIds", aclRecordIds);
            }

            // List only records for entities which are requested
            if (request.getEntityIds().size() > 0) {
                boolean isAdmin = authorization.isAdmin(securityToken);
                StringBuilder entityIdsFilterBuilder = new StringBuilder();
                for (String entityId : request.getEntityIds()) {
                    EntityIdentifier entityIdentifier = EntityIdentifier.parse(entityId);
                    boolean isGroup = entityIdentifier.isGroup();

                    // Check entity existence
                    PersistentObject entity = null;
                    if (!isGroup) {
                        entity = checkEntityExistence(entityIdentifier, entityManager);
                    }

                    // Check permission for listing
                    if (!isAdmin) {
                        if (isGroup) {
                            throw new TodoImplementException("List only ACL to which the requester has permission.");
                        }
                        else {
                            if (!authorization.hasEntityPermission(securityToken, entity, EntityPermission.READ)) {
                                ControllerReportSetHelper.throwSecurityNotAuthorizedFault("list ACL for %s", entityId);
                            }
                        }
                    }


                    EntityType entityType = entityIdentifier.getEntityType();
                    Long persistenceId = entityIdentifier.getPersistenceId();
                    StringBuilder entityIdFilterBuilder = new StringBuilder();
                    if (entityType != null) {
                        entityIdFilterBuilder.append("acl_record.entity_type = '");
                        entityIdFilterBuilder.append(entityType);
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
                        if (entityIdsFilterBuilder.length() > 0) {
                            entityIdsFilterBuilder.append(" OR ");
                        }
                        entityIdsFilterBuilder.append("(");
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
            if (request.getEntityRoles().size() > 0) {
                queryFilter.addFilter("acl_record.entity_role IN (:entityRoles)");
                queryFilter.addFilterParameter("entityRoles", request.getEntityRoles());
            }

            Map<String, String> parameters = new HashMap<String, String>();
            parameters.put("filter", queryFilter.toQueryWhere());
            String query = NativeQuery.getNativeQuery(NativeQuery.ACL_RECORD_LIST, parameters);

            ListResponse<AclRecord> response = new ListResponse<AclRecord>();
            List<Object[]> aclRecords = performNativeListRequest(query, queryFilter, request, response, entityManager);

            // Fill reservations to response
            for (Object[] aclRecord : aclRecords) {
                AclRecord aclRecordApi = new AclRecord();
                aclRecordApi.setId(aclRecord[0].toString());
                aclRecordApi.setUserId(aclRecord[1].toString());
                aclRecordApi.setEntityId(new EntityIdentifier(
                        EntityType.valueOf(aclRecord[2].toString()), ((Number) aclRecord[3]).longValue()).toId());
                aclRecordApi.setEntityRole(EntityRole.valueOf(aclRecord[4].toString()));
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
    public Map<String, EntityPermissionSet> listEntityPermissions(EntityPermissionListRequest request)
    {
        SecurityToken securityToken = request.getSecurityToken();
        authorization.validate(securityToken);
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            Map<String, EntityPermissionSet> response = new HashMap<String, EntityPermissionSet>();
            for (String entityId : request.getEntityIds()) {
                EntityIdentifier entityIdentifier = EntityIdentifier.parse(entityId);
                PersistentObject entity = checkEntityExistence(entityIdentifier, entityManager);
                response.put(entityId, new EntityPermissionSet(authorization.getEntityPermissions(securityToken, entity)));
            }
            return response;
        }
        finally {
            entityManager.close();
        }
    }

    @Override
    public void setEntityUser(SecurityToken securityToken, String entityId, String newUserId)
    {
        authorization.validate(securityToken);
        authorization.checkUserExistence(newUserId);
        EntityIdentifier entityIdentifier = EntityIdentifier.parse(entityId);
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        AuthorizationManager authorizationManager = new AuthorizationManager(entityManager, authorization);
        try {
            PersistentObject entity = entityManager.find(entityIdentifier.getEntityClass(),
                    entityIdentifier.getPersistenceId());
            if (entity == null) {
                ControllerReportSetHelper.throwEntityNotExistFault(entityIdentifier);
            }
            if (!authorization.isAdmin(securityToken)) {
                ControllerReportSetHelper.throwSecurityNotAuthorizedFault("change user for %s", entityId);
            }
            authorizationManager.beginTransaction();
            entityManager.getTransaction().begin();
            if (entity instanceof Resource) {
                Resource resource = (Resource) entity;
                for (cz.cesnet.shongo.controller.authorization.AclRecord aclRecord :
                        authorizationManager.listAclRecords(resource.getUserId(), resource, EntityRole.OWNER)) {
                    authorizationManager.deleteAclRecord(aclRecord);
                }
                resource.setUserId(newUserId);
                authorizationManager.createAclRecord(newUserId, entity, EntityRole.OWNER);
            }
            else if (entity instanceof AbstractReservationRequest) {
                // Change user to reservation request
                ReservationRequest reservationRequest = (ReservationRequest) entity;
                for (cz.cesnet.shongo.controller.authorization.AclRecord aclRecord :
                        authorizationManager.listAclRecords(
                                reservationRequest.getCreatedBy(), reservationRequest, EntityRole.OWNER)) {
                    authorizationManager.deleteAclRecord(aclRecord);
                }
                reservationRequest.setCreatedBy(newUserId);
                reservationRequest.setUpdatedBy(newUserId);
                authorizationManager.createAclRecord(newUserId, entity, EntityRole.OWNER);

                // Change user to child reservation requests
                Allocation allocation = reservationRequest.getAllocation();
                for (ReservationRequest childReservationRequest : allocation.getChildReservationRequests()) {
                    for (cz.cesnet.shongo.controller.authorization.AclRecord aclRecord :
                            authorizationManager.listAclRecords(
                                    childReservationRequest.getCreatedBy(), childReservationRequest, EntityRole.OWNER)) {
                        authorizationManager.deleteAclRecord(aclRecord);
                    }
                    childReservationRequest.setCreatedBy(newUserId);
                    authorizationManager.createAclRecord(newUserId, entity, EntityRole.OWNER);
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

    @Override
    public UserSettings getUserSettings(SecurityToken securityToken)
    {
        authorization.validate(securityToken);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        UserSettingsManager userSettingsManager = new UserSettingsManager(entityManager, authorization);
        try {
            return userSettingsManager.getUserSettings(securityToken);
        }
        finally {
            entityManager.close();
        }
    }

    @Override
    public void updateUserSettings(SecurityToken securityToken, UserSettings userSettingsApi)
    {
        authorization.validate(securityToken);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        UserSettingsManager userSettingsManager = new UserSettingsManager(entityManager, authorization);
        try {
            entityManager.getTransaction().begin();

            userSettingsManager.updateUserSettings(securityToken, userSettingsApi);

            entityManager.getTransaction().commit();
        }
        finally {
            entityManager.close();
        }
    }

    @Override
    public void modifyUserId(SecurityToken securityToken, String oldUserId, String newUserId)
    {
        authorization.validate(securityToken);
        authorization.clearCache();

        if (!authorization.isAdmin(securityToken)) {
            ControllerReportSetHelper.throwSecurityNotAuthorizedFault("change user id %s to %s", oldUserId, newUserId);
        }

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            entityManager.getTransaction().begin();

            String query = NativeQuery.getNativeQuery(entityManagerFactory, NativeQuery.MODIFY_USER_ID);
            String queryParts[] = query.split("\\[perform\\]");
            String queryInit = queryParts[0].trim();
            String queryDestroy = queryParts[1].trim();

            authorization.checkUserExistence(newUserId);

            if (!queryInit.isEmpty()) {
                entityManager.createNativeQuery(queryInit).executeUpdate();
            }

            entityManager.createQuery("UPDATE AclRecord SET userId = :newUserId WHERE userId = :oldUserId")
                    .setParameter("oldUserId", oldUserId)
                    .setParameter("newUserId", newUserId)
                    .executeUpdate();

            entityManager.createQuery("UPDATE UserPerson SET userId = :newUserId WHERE userId = :oldUserId")
                    .setParameter("oldUserId", oldUserId)
                    .setParameter("newUserId", newUserId)
                    .executeUpdate();

            entityManager.createQuery("UPDATE Resource SET userId = :newUserId WHERE userId = :oldUserId")
                    .setParameter("oldUserId", oldUserId)
                    .setParameter("newUserId", newUserId)
                    .executeUpdate();

            // Delete old UserSettings if the new exists
            if (entityManager.createQuery("SELECT userSettings FROM UserSettings userSettings WHERE userId = :userId")
                    .setParameter("userId", newUserId)
                    .getResultList().size() > 0) {
                try {
                    cz.cesnet.shongo.controller.settings.UserSettings userSettings = entityManager.createQuery(
                            "SELECT userSettings FROM UserSettings userSettings WHERE userId = :userId",
                            cz.cesnet.shongo.controller.settings.UserSettings.class)
                            .setParameter("userId", oldUserId)
                            .getSingleResult();
                    entityManager.remove(userSettings);
                    entityManager.flush();
                }
                catch (NoResultException exception) {
                    // Old user settings doesn't exist, we don't have to delete it
                }
            }
            entityManager.createQuery("UPDATE UserSettings SET userId = :newUserId WHERE userId = :oldUserId")
                    .setParameter("oldUserId", oldUserId)
                    .setParameter("newUserId", newUserId)
                    .executeUpdate();

            entityManager.createQuery(
                    "UPDATE AbstractReservationRequest SET createdBy = :newUserId WHERE createdBy = :oldUserId")
                    .setParameter("oldUserId", oldUserId)
                    .setParameter("newUserId", newUserId)
                    .executeUpdate();

            entityManager.createQuery(
                    "UPDATE AbstractReservationRequest SET updatedBy = :newUserId WHERE updatedBy = :oldUserId")
                    .setParameter("oldUserId", oldUserId)
                    .setParameter("newUserId", newUserId)
                    .executeUpdate();

            if (!queryDestroy.isEmpty()) {
                entityManager.createNativeQuery(queryDestroy).executeUpdate();
            }

            entityManager.getTransaction().commit();

            authorization.clearCache();
        }
        finally {
            entityManager.close();
        }
    }

    @Override
    public Map<String, String> listReferencedUsers(SecurityToken securityToken)
    {
        authorization.validate(securityToken);

        if (!authorization.isAdmin(securityToken)) {
            ControllerReportSetHelper.throwSecurityNotAuthorizedFault("list referenced users");
        }

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            entityManager.getTransaction().begin();

            String referencedUsersQuery = NativeQuery.getNativeQuery(NativeQuery.REFERENCED_USER_LIST);
            List referencedUsersResult = entityManager.createNativeQuery(referencedUsersQuery).getResultList();
            Map<String, String> referencedUsers = new LinkedHashMap<String, String>();
            for (Object referencedUserItem : referencedUsersResult) {
                Object[] referencedUser = (Object[]) referencedUserItem;
                referencedUsers.put(referencedUser[0].toString(), referencedUser[1].toString());
            }
            entityManager.getTransaction().commit();
            return referencedUsers;
        }
        finally {
            entityManager.close();
        }
    }

    /**
     * @param entityId      of entity which should be checked for existence
     * @param entityManager which can be used
     * @return {@link cz.cesnet.shongo.PersistentObject} for given {@code entityId}
     * @throws cz.cesnet.shongo.CommonReportSet.EntityNotExistsException
     *
     */
    private PersistentObject checkEntityExistence(EntityIdentifier entityId, EntityManager entityManager)
            throws CommonReportSet.EntityNotExistsException
    {
        PersistentObject entity = entityManager.find(entityId.getEntityClass(), entityId.getPersistenceId());
        if (entity == null) {
            ControllerReportSetHelper.throwEntityNotExistFault(entityId);
        }
        return entity;
    }
}
