package cz.cesnet.shongo.controller.api.rpc;

import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.*;
import cz.cesnet.shongo.controller.acl.*;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.AclEntry;
import cz.cesnet.shongo.controller.api.request.*;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.booking.Allocation;
import cz.cesnet.shongo.controller.booking.request.ReservationRequest;
import cz.cesnet.shongo.controller.booking.resource.Resource;
import cz.cesnet.shongo.controller.settings.UserSettingsManager;
import cz.cesnet.shongo.controller.util.NativeQuery;
import cz.cesnet.shongo.controller.util.QueryFilter;
import cz.cesnet.shongo.util.StringHelper;
import org.apache.commons.lang.StringUtils;
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
    public boolean hasSystemPermission(SecurityToken securityToken, SystemPermission systemPermission)
    {
        return authorization.hasSystemPermission(securityToken, systemPermission);
    }

    @Override
    public Set<SystemPermission> getSystemPermissions(SecurityToken securityToken)
    {
        Set<SystemPermission> systemPermissions = new HashSet<SystemPermission>();
        for (SystemPermission systemPermission : SystemPermission.values()) {
            authorization.hasSystemPermission(securityToken, systemPermission);
        }
        return systemPermissions;
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

        if (!authorization.isAdministrator(securityToken)) {
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

            entityManager.createQuery("UPDATE AclIdentity SET principalId = :newUserId WHERE type = :type AND principalId = :oldUserId")
                    .setParameter("oldUserId", oldUserId)
                    .setParameter("newUserId", newUserId)
                    .setParameter("type", AclIdentityType.USER)
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
    public ListResponse<Group> listGroups(GroupListRequest request)
    {
        SecurityToken securityToken = request.getSecurityToken();
        authorization.validate(securityToken);

        List<Group> groups = new LinkedList<Group>(authorization.listGroups());

        String search = StringHelper.removeAccents(request.getSearch());
        if (search != null) {
            for (Iterator<Group> iterator = groups.iterator(); iterator.hasNext(); ) {
                Group group = iterator.next();
                if (!StringUtils.containsIgnoreCase(StringHelper.removeAccents(group.getName()), search)) {
                    iterator.remove();
                }
            }
        }

        Set<String> groupIds = request.getGroupIds();
        if (groupIds.size() > 0) {
            for (Iterator<Group> iterator = groups.iterator(); iterator.hasNext(); ) {
                Group group = iterator.next();
                if (!groupIds.contains(group.getId())) {
                    iterator.remove();
                }
            }
        }

        int start = request.getStart(0);
        int end = start + request.getCount(groups.size() - start);
        ListResponse<Group> response = new ListResponse<Group>();
        response.setStart(start);
        response.setCount(end - start);
        for (Group group : groups.subList(start, end)) {
            response.addItem(group);
        }
        return response;
    }

    @Override
    public String createGroup(SecurityToken token, Group group)
    {
        authorization.validate(token);
        if (!authorization.isAdministrator(token)) {
            ControllerReportSetHelper.throwSecurityNotAuthorizedFault("create group");
        }
        return authorization.createGroup(group);
    }

    @Override
    public void deleteGroup(SecurityToken token, String groupId)
    {
        authorization.validate(token);
        if (!authorization.isAdministrator(token)) {
            ControllerReportSetHelper.throwSecurityNotAuthorizedFault("delete group " + groupId);
        }
        authorization.deleteGroup(groupId);
    }

    @Override
    public void addGroupUser(SecurityToken token, String groupId, String userId)
    {
        authorization.validate(token);
        if (!authorization.isAdministrator(token)) {
            ControllerReportSetHelper.throwSecurityNotAuthorizedFault("add user to group " + groupId);
        }
        authorization.addGroupUser(groupId, userId);
    }

    @Override
    public void removeGroupUser(SecurityToken token, String groupId, String userId)
    {
        authorization.validate(token);
        if (!authorization.isAdministrator(token)) {
            ControllerReportSetHelper.throwSecurityNotAuthorizedFault("remove user from group " + groupId);
        }
        authorization.removeGroupUser(groupId, userId);
    }

    @Override
    public String createAclEntry(SecurityToken securityToken, AclEntry aclEntryApi)
    {
        authorization.validate(securityToken);
        switch (aclEntryApi.getIdentityType()) {
            case USER:
                authorization.checkUserExistence(aclEntryApi.getIdentityPrincipalId());
                break;
            case GROUP:
                authorization.checkGroupExistence(aclEntryApi.getIdentityPrincipalId());
                break;
            default:
                throw new TodoImplementException(aclEntryApi.getIdentityType());
        }

        ObjectIdentifier objectIdentifier = ObjectIdentifier.parse(aclEntryApi.getObjectId());
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        AuthorizationManager authorizationManager = new AuthorizationManager(entityManager, authorization);
        try {
            PersistentObject object = checkObjectExistence(objectIdentifier, entityManager);
            if (!authorization.hasObjectPermission(securityToken, object, ObjectPermission.WRITE)) {
                ControllerReportSetHelper.throwSecurityNotAuthorizedFault("create ACL for %s", aclEntryApi.getObjectId());
            }
            authorizationManager.beginTransaction();
            entityManager.getTransaction().begin();
            cz.cesnet.shongo.controller.acl.AclEntry aclEntry = authorizationManager.createAclEntry(
                    aclEntryApi.getIdentityType(), aclEntryApi.getIdentityPrincipalId(), object, aclEntryApi.getRole());
            entityManager.getTransaction().commit();
            authorizationManager.commitTransaction();
            return (aclEntry != null ? aclEntry.getId().toString() : null);
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
    public void deleteAclEntry(SecurityToken securityToken, String aclEntryId)
    {
        authorization.validate(securityToken);
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        AuthorizationManager authorizationManager = new AuthorizationManager(entityManager, authorization);
        try {
            cz.cesnet.shongo.controller.acl.AclEntry aclEntry = authorizationManager.getAclEntry(Long.valueOf(aclEntryId));
            if (!authorization.hasObjectPermission(securityToken, aclEntry.getObjectIdentity(), ObjectPermission.WRITE)) {
                ControllerReportSetHelper.throwSecurityNotAuthorizedFault("delete ACL for %s", aclEntry.getObjectIdentity());
            }
            authorizationManager.beginTransaction();
            entityManager.getTransaction().begin();
            authorizationManager.deleteAclEntry(aclEntry);
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
    public ListResponse<AclEntry> listAclEntries(AclEntryListRequest request)
    {
        SecurityToken securityToken = request.getSecurityToken();
        authorization.validate(securityToken);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            QueryFilter queryFilter = new QueryFilter("acl_entry", true);

            // List only records which are requested
            if (request.getEntryIds().size() > 0) {
                queryFilter.addFilter("acl_entry.id IN (:aclEntryIds)");
                Set<Long> aclEntryIds = new HashSet<Long>();
                for (String aclEntryId : request.getEntryIds()) {
                    aclEntryIds.add(Long.valueOf(aclEntryId));
                }
                queryFilter.addFilterParameter("aclEntryIds", aclEntryIds);
            }

            // List only records for entities which are requested
            if (request.getObjectIds().size() > 0) {
                boolean isAdmin = authorization.isAdministrator(securityToken);
                AclProvider aclProvider = authorization.getAclProvider();
                Set<Long> objectClassesIds = new HashSet<Long>();
                Set<Long> objectIdentityIds = new HashSet<Long>();
                for (String objectId : request.getObjectIds()) {
                    ObjectIdentifier objectIdentifier = ObjectIdentifier.parse(objectId);
                    boolean isGroup = objectIdentifier.isGroup();

                    // Check object existence
                    PersistentObject object = null;
                    if (!isGroup) {
                        object = checkObjectExistence(objectIdentifier, entityManager);
                    }

                    // Check permission for listing
                    if (!isAdmin) {
                        if (isGroup) {
                            throw new TodoImplementException("List only ACL to which the requester has permission.");
                        }
                        else {
                            if (!authorization.hasObjectPermission(securityToken, object, ObjectPermission.READ)) {
                                ControllerReportSetHelper.throwSecurityNotAuthorizedFault("list ACL for %s", objectId);
                            }
                        }
                    }

                    if (isGroup) {
                        AclObjectClass objectClass = aclProvider.getObjectClass(objectIdentifier.getObjectClass());
                        objectClassesIds.add(objectClass.getId());
                    }
                    else {
                        AclObjectIdentity objectIdentity = aclProvider.getObjectIdentity(object);
                        objectIdentityIds.add(objectIdentity.getId());
                    }
                }
                StringBuilder objectFilter = new StringBuilder();
                if (objectClassesIds.size() > 0) {
                    objectFilter.append("acl_entry.object_class_id IN(:objectClassesIds)");
                    queryFilter.addFilterParameter("objectClassesIds", objectClassesIds);
                }
                if (objectIdentityIds.size() > 0) {
                    if (objectFilter.length() > 0) {
                        objectFilter.append(" OR ");
                    }
                    objectFilter.append("acl_entry.object_identity_id IN(:objectIdentityIds)");
                    queryFilter.addFilterParameter("objectIdentityIds", objectIdentityIds);
                }
                queryFilter.addFilter(objectFilter.toString());
            }

            // List only records for requested users
            if (request.getUserIds().size() > 0) {
                queryFilter.addFilter("acl_entry.identity_type = 'USER' AND acl_entry.identity_principal_id IN (:userIds)");
                queryFilter.addFilterParameter("userIds", request.getUserIds());
            }

            // List only records for requested roles
            if (request.getRoles().size() > 0) {
                queryFilter.addFilter("acl_entry.role IN (:roles)");
                queryFilter.addFilterParameter("roles", request.getRoles());
            }

            Map<String, String> parameters = new HashMap<String, String>();
            parameters.put("filter", queryFilter.toQueryWhere());
            String query = NativeQuery.getNativeQuery(NativeQuery.ACL_ENTRY_LIST, parameters);

            ListResponse<AclEntry> response = new ListResponse<AclEntry>();
            List<Object[]> aclEntries = performNativeListRequest(query, queryFilter, request, response, entityManager);

            // Fill reservations to response
            for (Object[] aclEntry : aclEntries) {
                AclEntry aclEntryApi = new AclEntry();
                aclEntryApi.setId(aclEntry[0].toString());
                aclEntryApi.setIdentityType(AclIdentityType.valueOf(aclEntry[2].toString()));
                aclEntryApi.setIdentityPrincipalId(aclEntry[3].toString());
                aclEntryApi.setObjectId(new ObjectIdentifier(
                        ObjectType.valueOf(aclEntry[6].toString()), ((Number) aclEntry[7]).longValue()).toId());
                aclEntryApi.setRole(ObjectRole.valueOf(aclEntry[8].toString()));
                aclEntryApi.setDeletable(((Number) aclEntry[9]).intValue() == 0);
                response.addItem(aclEntryApi);
            }
            return response;
        }
        finally {
            entityManager.close();
        }
    }

    @Override
    public Map<String, ObjectPermissionSet> listObjectPermissions(ObjectPermissionListRequest request)
    {
        SecurityToken securityToken = request.getSecurityToken();
        authorization.validate(securityToken);
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            Map<String, ObjectPermissionSet> response = new HashMap<String, ObjectPermissionSet>();
            for (String objectId : request.getObjectIds()) {
                ObjectIdentifier objectIdentifier = ObjectIdentifier.parse(objectId);
                PersistentObject object = checkObjectExistence(objectIdentifier, entityManager);
                response.put(objectId, new ObjectPermissionSet(authorization.getObjectPermissions(securityToken, object)));
            }
            return response;
        }
        finally {
            entityManager.close();
        }
    }

    @Override
    public void setObjectUser(SecurityToken securityToken, String objectId, String newUserId)
    {
        authorization.validate(securityToken);
        authorization.checkUserExistence(newUserId);
        ObjectIdentifier objectIdentifier = ObjectIdentifier.parse(objectId);
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        AuthorizationManager authorizationManager = new AuthorizationManager(entityManager, authorization);
        try {
            PersistentObject object = entityManager.find(objectIdentifier.getObjectClass(),
                    objectIdentifier.getPersistenceId());
            if (object == null) {
                ControllerReportSetHelper.throwObjectNotExistFault(objectIdentifier);
            }
            if (!authorization.isAdministrator(securityToken)) {
                ControllerReportSetHelper.throwSecurityNotAuthorizedFault("change user for %s", objectId);
            }
            authorizationManager.beginTransaction();
            entityManager.getTransaction().begin();
            if (object instanceof Resource) {
                Resource resource = (Resource) object;
                for (cz.cesnet.shongo.controller.acl.AclEntry aclEntry :
                        authorizationManager.listAclEntries(resource.getUserId(), resource, ObjectRole.OWNER)) {
                    authorizationManager.deleteAclEntry(aclEntry);
                }
                resource.setUserId(newUserId);
                authorizationManager.createAclEntry(AclIdentityType.USER, newUserId, object, ObjectRole.OWNER);
            }
            else if (object instanceof AbstractReservationRequest) {
                // Change user to reservation request
                ReservationRequest reservationRequest = (ReservationRequest) object;
                for (cz.cesnet.shongo.controller.acl.AclEntry aclEntry :
                        authorizationManager.listAclEntries(
                                reservationRequest.getCreatedBy(), reservationRequest, ObjectRole.OWNER)) {
                    authorizationManager.deleteAclEntry(aclEntry);
                }
                reservationRequest.setCreatedBy(newUserId);
                reservationRequest.setUpdatedBy(newUserId);
                authorizationManager.createAclEntry(AclIdentityType.USER, newUserId, object, ObjectRole.OWNER);

                // Change user to child reservation requests
                Allocation allocation = reservationRequest.getAllocation();
                for (ReservationRequest childReservationRequest : allocation.getChildReservationRequests()) {
                    for (cz.cesnet.shongo.controller.acl.AclEntry aclEntry :
                            authorizationManager.listAclEntries(
                                    childReservationRequest.getCreatedBy(), childReservationRequest, ObjectRole.OWNER)) {
                        authorizationManager.deleteAclEntry(aclEntry);
                    }
                    childReservationRequest.setCreatedBy(newUserId);
                    authorizationManager.createAclEntry(AclIdentityType.USER, newUserId, object, ObjectRole.OWNER);
                }
            }
            else {
                throw new RuntimeException("The user cannot be set for object of type "
                        + object.getClass().getSimpleName() + ".");
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
    public Map<String, String> listReferencedUsers(SecurityToken securityToken)
    {
        authorization.validate(securityToken);

        if (!authorization.isAdministrator(securityToken)) {
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
     * @param objectId      of object which should be checked for existence
     * @param entityManager which can be used
     * @return {@link cz.cesnet.shongo.PersistentObject} for given {@code objectId}
     * @throws cz.cesnet.shongo.CommonReportSet.ObjectNotExistsException
     *
     */
    private PersistentObject checkObjectExistence(ObjectIdentifier objectId, EntityManager entityManager)
            throws CommonReportSet.ObjectNotExistsException
    {
        PersistentObject object = entityManager.find(objectId.getObjectClass(), objectId.getPersistenceId());
        if (object == null) {
            ControllerReportSetHelper.throwObjectNotExistFault(objectId);
        }
        return object;
    }
}
