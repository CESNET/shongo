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
import cz.cesnet.shongo.controller.authorization.UserIdSet;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.person.UserPerson;
import cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.booking.Allocation;
import cz.cesnet.shongo.controller.booking.request.ReservationRequest;
import cz.cesnet.shongo.controller.booking.resource.ForeignResources;
import cz.cesnet.shongo.controller.booking.resource.Resource;
import cz.cesnet.shongo.controller.booking.resource.ResourceManager;
import cz.cesnet.shongo.controller.settings.UserSettingsManager;
import cz.cesnet.shongo.controller.util.NativeQuery;
import cz.cesnet.shongo.controller.util.QueryFilter;
import cz.cesnet.shongo.util.StringHelper;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTimeZone;

import javax.persistence.*;
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
    public SystemPermissionSet getSystemPermissions(SecurityToken securityToken)
    {
        Set<SystemPermission> systemPermissions = new HashSet<SystemPermission>();
        for (SystemPermission systemPermission : SystemPermission.values()) {
            if (authorization.hasSystemPermission(securityToken, systemPermission)){
                systemPermissions.add(systemPermission);
            }
        }
        return new SystemPermissionSet(systemPermissions);
    }

    @Override
    public ListResponse<UserInformation> listUsers(UserListRequest request)
    {
        checkNotNull("request", request);
        authorization.validate(request.getSecurityToken());

        String search = StringHelper.removeAccents(request.getSearch());

        List<UserInformation> users = new LinkedList<UserInformation>();

        String principalName = request.getPrincipalName();
        if (principalName != null) {
            users.add(authorization.getUserInformationByPrincipalName(principalName));
        }
        else {
            // Get user-ids
            UserIdSet userIds = new  UserIdSet(request.getUserIds());
            if (userIds.size() == 0) {
                userIds = null;
            }

            // Update user-ids by groups
            Set<String> groupIds = request.getGroupIds();
            if (groupIds.size() > 0) {
                UserIdSet groupsUserIds = new UserIdSet();
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
                for (String userId : userIds.getUserIds()) {
                    users.add(authorization.getUserInformation(userId));

                }
                // Filter them
                if (search != null) {
                    UserInformation.filter(users, search);
                }
            }
            else {
                Collection<UserInformation> result = authorization.listUserInformation(userIds != null ? userIds.getUserIds() : null, search);
                for (UserInformation userInformation : result) {
                    users.add(userInformation);
                }
            }
        }

        return ListResponse.fromRequest(request, users);
    }

    @Override
    public UserSettings getUserSettings(SecurityToken securityToken)
    {
        authorization.validate(securityToken);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        UserSettingsManager userSettingsManager = new UserSettingsManager(entityManager, authorization);
        try {
            return userSettingsManager.getUserSettings(securityToken, null);
        } catch (ControllerReportSet.UserNotExistsException exception) {
            return getUserSettingsFromToken(securityToken);
        }
        finally {
            entityManager.close();
        }
    }

    @Override
    public UserSettings getUserSettings(SecurityToken securityToken, boolean useWebService)
    {
        authorization.validate(securityToken);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        UserSettingsManager userSettingsManager = new UserSettingsManager(entityManager, authorization);
        try {
            return userSettingsManager.getUserSettings(securityToken, useWebService);
        }
        catch (ControllerReportSet.UserNotExistsException exception) {
            return getUserSettingsFromToken(securityToken);
        }
        finally {
            entityManager.close();
        }
    }

    @Override
    public UserSettings getUserSettings(SecurityToken securityToken, String userId)
    {
        authorization.validate(securityToken);
        checkNotNull("userId", userId);
        if (!authorization.isOperator(securityToken) && !userId.equals(securityToken.getUserId())) {
            ControllerReportSetHelper.throwSecurityNotAuthorizedFault("get settings for user %s ", userId);
        }

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        UserSettingsManager userSettingsManager = new UserSettingsManager(entityManager, authorization);
        try {
            return userSettingsManager.getUserSettings(userId, null);
        } catch (ControllerReportSet.UserNotExistsException exception) {
            return getUserSettingsFromToken(securityToken);
        }
        finally {
            entityManager.close();
        }
    }

    @Override
    public void updateUserSettings(SecurityToken securityToken, UserSettings userSettingsApi)
    {
        authorization.validate(securityToken);
        checkNotNull("userSettings", userSettingsApi);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        UserSettingsManager userSettingsManager = new UserSettingsManager(entityManager, authorization);
        try {
            entityManager.getTransaction().begin();

            userSettingsManager.updateUserSessionSettings(securityToken, userSettingsApi);
            userSettingsManager.updateUserSettings(securityToken.getUserId(), userSettingsApi);

            entityManager.getTransaction().commit();
        }
        finally {
            entityManager.close();
        }
    }

    @Override
    public void updateUserSettings(SecurityToken securityToken, String userId, UserSettings userSettingsApi)
    {
        authorization.validate(securityToken);
        checkNotNull("userId", userId);
        checkNotNull("userSettings", userSettingsApi);
        if (!authorization.isAdministrator(securityToken) && !userId.equals(securityToken.getUserId())) {
            ControllerReportSetHelper.throwSecurityNotAuthorizedFault("update settings for user %s ", userId);
        }

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        UserSettingsManager userSettingsManager = new UserSettingsManager(entityManager, authorization);
        try {
            entityManager.getTransaction().begin();

            userSettingsManager.updateUserSettings(userId, userSettingsApi);

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
        checkNotNull("oldUserId", oldUserId);
        checkNotNull("newUserId", newUserId);
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

            AclIdentity newAclIdentity;
            try {
                newAclIdentity = entityManager.createNamedQuery("AclIdentity.find", AclIdentity.class)
                        .setParameter("type", AclIdentityType.USER)
                        .setParameter("principalId", newUserId)
                        .getSingleResult();
            }
            catch (NoResultException exception) {
                newAclIdentity = null;
            }
            if (newAclIdentity != null) {
                try {
                    AclIdentity oldAclIdentity = entityManager.createNamedQuery("AclIdentity.find", AclIdentity.class)
                            .setParameter("type", AclIdentityType.USER)
                            .setParameter("principalId", oldUserId)
                            .getSingleResult();

                    entityManager.createQuery("UPDATE AclEntry SET identity = :newIdentity WHERE identity = :oldIdentity")
                            .setParameter("oldIdentity", oldAclIdentity)
                            .setParameter("newIdentity", newAclIdentity)
                            .executeUpdate();
                }
                catch (NoResultException exception) {
                    // No update needed
                }
            }
            else {
                entityManager.createQuery("UPDATE AclIdentity SET principalId = :newUserId WHERE type = :type AND principalId = :oldUserId")
                        .setParameter("oldUserId", oldUserId)
                        .setParameter("newUserId", newUserId)
                        .setParameter("type", AclIdentityType.USER)
                        .executeUpdate();
            }

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
                    "UPDATE Reservation SET userId = :newUserId WHERE userId = :oldUserId")
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
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            entityManager.close();
        }
    }

    @Override
    public ListResponse<Group> listGroups(GroupListRequest request)
    {
        checkNotNull("request", request);
        SecurityToken securityToken = request.getSecurityToken();
        authorization.validate(securityToken);

        boolean isOperator = authorization.isOperator(securityToken);
        Set<Group.Type> groupTypes = request.getGroupTypes();
        if (groupTypes.isEmpty()) {
            groupTypes.add(Group.Type.USER);
            if (isOperator) {
                groupTypes.add(Group.Type.SYSTEM);
            }
        }
        else if (groupTypes.contains(Group.Type.SYSTEM) && !isOperator) {
            groupTypes.remove(Group.Type.SYSTEM);
        }

        List<Group> groups = new LinkedList<Group>(authorization.listGroups(null, groupTypes));

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

        return ListResponse.fromRequest(request, groups);
    }

    @Override
    public Group getGroup(SecurityToken token, String groupId)
    {
        authorization.validate(token);
        checkNotNull("groupId", groupId);

        Group group = authorization.getGroup(groupId);
        if (Group.Type.SYSTEM.equals(group.getType()) && !authorization.isOperator(token)) {
            ControllerReportSetHelper.throwSecurityNotAuthorizedFault("read system group");
        }
        return group;
    }

    @Override
    public String createGroup(SecurityToken token, Group group)
    {
        authorization.validate(token);
        checkNotNull("group", group);
        if (!authorization.isAdministrator(token)) {
            ControllerReportSetHelper.throwSecurityNotAuthorizedFault("create group");
        }
        return authorization.createGroup(group);
    }

    @Override
    public void modifyGroup(SecurityToken token, Group group)
    {
        authorization.validate(token);
        checkNotNull("group", group);
        String groupId = group.getId();
        Group existingGroup = authorization.checkGroupExistence(groupId);
        if (Group.Type.SYSTEM.equals(existingGroup.getType()) && !authorization.isAdministrator(token)) {
            ControllerReportSetHelper.throwSecurityNotAuthorizedFault("modify system group");
        }
        authorization.modifyGroup(group);
    }

    @Override
    public void deleteGroup(SecurityToken token, String groupId)
    {
        authorization.validate(token);
        checkNotNull("groupId", groupId);
        if (!authorization.isAdministrator(token)) {
            ControllerReportSetHelper.throwSecurityNotAuthorizedFault("delete group " + groupId);
        }
        authorization.deleteGroup(groupId);
    }

    @Override
    public void addGroupUser(SecurityToken token, String groupId, String userId)
    {
        authorization.validate(token);
        checkNotNull("groupId", groupId);
        checkNotNull("userId", userId);
        if (!authorization.isAdministrator(token)) {
            ControllerReportSetHelper.throwSecurityNotAuthorizedFault("add user to group " + groupId);
        }
        authorization.addGroupUser(groupId, userId);
    }

    @Override
    public void removeGroupUser(SecurityToken token, String groupId, String userId)
    {
        authorization.validate(token);
        checkNotNull("groupId", groupId);
        checkNotNull("userId", userId);
        if (!authorization.isAdministrator(token)) {
            ControllerReportSetHelper.throwSecurityNotAuthorizedFault("remove user from group " + groupId);
        }
        authorization.removeGroupUser(groupId, userId);
    }

    @Override
    public String createAclEntry(SecurityToken securityToken, AclEntry aclEntryApi)
    {
        authorization.validate(securityToken);
        checkNotNull("aclEntry", aclEntryApi);
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

        ObjectIdentifier objectIdentifier;
        if (ObjectIdentifier.isLocal(aclEntryApi.getObjectId())) {
            objectIdentifier = ObjectIdentifier.parse(aclEntryApi.getObjectId());
        }
        else {
            ObjectIdentifier foreignObjectIdentifier = ObjectIdentifier.parseForeignId(aclEntryApi.getObjectId());
            switch (foreignObjectIdentifier.getObjectType()) {
                case RESOURCE:
                    EntityManager entityManager = entityManagerFactory.createEntityManager();
                    ResourceManager resourceManager = new ResourceManager(entityManager);
                    String domainName = foreignObjectIdentifier.getDomainName();
                    try {
                        entityManager.getTransaction().begin();
                        ForeignResources foreignResources = resourceManager.findOrCreateForeignResources(domainName, foreignObjectIdentifier.getPersistenceId());
                        entityManager.getTransaction().commit();

                        objectIdentifier = new ObjectIdentifier(ObjectType.FOREIGN_RESOURCES, foreignResources.getId());
                    }
                    finally {
                        if (entityManager.getTransaction().isActive()) {
                            entityManager.getTransaction().rollback();
                        }
                        entityManager.close();
                    }
                    break;
                default:
                    throw new TodoImplementException("Unsupported type of foreign object");
            }
        }
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
            authorizationManager.commitTransaction(securityToken);
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
        checkNotNull("aclEntryId", aclEntryId);
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
            authorizationManager.commitTransaction(securityToken);
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
        checkNotNull("request", request);
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
                boolean isOperator = authorization.isOperator(securityToken);
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
                    if (!isOperator) {
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
        checkNotNull("request", request);
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
        checkNotNull("objectId", objectId);
        checkNotNull("newUserId", newUserId);
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
                AbstractReservationRequest reservationRequest = (AbstractReservationRequest) object;
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
            authorizationManager.commitTransaction(securityToken);
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
    public List<ReferencedUser> listReferencedUsers(SecurityToken securityToken)
    {
        authorization.validate(securityToken);

        if (!authorization.isOperator(securityToken)) {
            ControllerReportSetHelper.throwSecurityNotAuthorizedFault("list referenced users");
        }

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            entityManager.getTransaction().begin();

            String referencedUsersQuery = NativeQuery.getNativeQuery(NativeQuery.REFERENCED_USER_LIST);
            List referencedUsersResult = entityManager.createNativeQuery(referencedUsersQuery).getResultList();
            List<ReferencedUser> referencedUsers = new LinkedList<ReferencedUser>();
            Set<String> userIds = new LinkedHashSet<String>();
            for (Object referencedUserItem : referencedUsersResult) {
                Object[] referencedUser = (Object[]) referencedUserItem;
                String userId = referencedUser[0].toString();
                if (userId.equals("0")) {
                    continue;
                }
                userIds.add(userId);
            }
            Map<String, UserInformation> userInformationByUserId = new HashMap<String, UserInformation>();
            for (UserInformation userInformation : authorization.listUserInformation(userIds, null)) {
                userInformationByUserId.put(userInformation.getUserId(), userInformation);
            }
            for (Object referencedUserItem : referencedUsersResult) {
                Object[] referencedUserResult = (Object[]) referencedUserItem;
                String userId = referencedUserResult[0].toString();
                UserInformation userInformation = userInformationByUserId.get(userId);
                if (userInformation == null) {
                    userInformation = authorization.getUserInformation(userId);
                }
                ReferencedUser referencedUser = new ReferencedUser();
                referencedUser.setUserInformation(userInformation);
                referencedUser.setReservationRequestCount(
                        referencedUserResult[1] != null ? ((Number) referencedUserResult[1]).intValue() : 0);
                referencedUser.setResourceCount(
                        referencedUserResult[2] != null ? ((Number) referencedUserResult[2]).intValue() : 0);
                referencedUser.setUserSettingCount(
                        referencedUserResult[3] != null ? ((Number) referencedUserResult[3]).intValue() : 0);
                referencedUser.setAclEntryCount(
                        referencedUserResult[4] != null ? ((Number) referencedUserResult[4]).intValue() : 0);
                referencedUser.setUserPersonCount(
                        referencedUserResult[5] != null ? ((Number) referencedUserResult[5]).intValue() : 0);
                referencedUsers.add(referencedUser);
            }
            entityManager.getTransaction().commit();
            return referencedUsers;
        }
        finally {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
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

    private UserSettings getUserSettingsFromToken(SecurityToken securityToken){
            UserSettings userSettings = new UserSettings();
            userSettings.setUseWebService(true);
            userSettings.setSystemAdministratorNotifications(true);
            userSettings.setResourceAdministratorNotifications(true);
            userSettings.setLocale(new Locale(securityToken.getUserInformation().getLocale()));
            userSettings.setHomeTimeZone(DateTimeZone.forID(securityToken.getUserInformation().getZoneInfo()));
            return userSettings;
    }
}
