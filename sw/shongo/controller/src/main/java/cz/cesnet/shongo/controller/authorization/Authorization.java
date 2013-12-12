package cz.cesnet.shongo.controller.authorization;

import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.*;
import cz.cesnet.shongo.controller.api.Group;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.booking.person.UserPerson;
import cz.cesnet.shongo.controller.settings.UserSessionSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.*;

/**
 * Provides methods for performing authentication, authorization and fetching user data from web service.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class Authorization
{
    private static Logger logger = LoggerFactory.getLogger(Authorization.class);

    /**
     * Root user-id.
     */
    public static final String ROOT_USER_ID = "0";

    /**
     * Root shongo-user.
     */
    public static final UserData ROOT_USER_DATA;

    /**
     * Static initialization.
     */
    static {
        ROOT_USER_DATA = new UserData();
        UserInformation rootUserInformation = ROOT_USER_DATA.getUserInformation();
        rootUserInformation.setUserId(ROOT_USER_ID);
        rootUserInformation.setLastName("root");
    }

    /**
     * @see EntityManagerFactory
     */
    protected EntityManagerFactory entityManagerFactory;

    /**
     * @see AuthorizationCache
     */
    private AuthorizationCache cache = new AuthorizationCache();

    /**
     * @see ControllerConfiguration#SECURITY_ADMIN_GROUP
     */
    protected String administratorGroupName;

    /**
     * Set of access-tokens which has administrator access.
     */
    protected Set<String> administratorAccessTokens = new HashSet<String>();

    /**
     * {@link cz.cesnet.shongo.controller.settings.UserSessionSettings}s.
     */
    private Map<SecurityToken, UserSessionSettings> userSessionSettings = new HashMap<SecurityToken, UserSessionSettings>();

    /**
     * Constructor.
     *
     * @param configuration to load authorization configuration from
     */
    protected Authorization(ControllerConfiguration configuration)
    {
        this.cache.setUserIdExpiration(configuration.getDuration(
                ControllerConfiguration.SECURITY_EXPIRATION_USER_ID));
        this.cache.setUserInformationExpiration(configuration.getDuration(
                ControllerConfiguration.SECURITY_EXPIRATION_USER_INFORMATION));
        this.cache.setAclExpiration(configuration.getDuration(
                ControllerConfiguration.SECURITY_EXPIRATION_ACL));
        this.cache.setGroupExpiration(configuration.getDuration(
                ControllerConfiguration.SECURITY_EXPIRATION_GROUP));
        this.administratorGroupName = configuration.getString(ControllerConfiguration.SECURITY_ADMIN_GROUP);
    }

    /**
     * Destroy this {@link Authorization} (and you be able to create another {@link Authorization} instance again)
     */
    public void destroy()
    {
        authorization = null;
    }

    /**
     * @param entityManagerFactory sets the {@link #entityManagerFactory}
     */
    public final void setEntityManagerFactory(EntityManagerFactory entityManagerFactory)
    {
        this.entityManagerFactory = entityManagerFactory;
    }

    /**
     * Clear the {@link AuthorizationCache}.
     */
    public void clearCache()
    {
        cache.clear();
    }

    /**
     * Validate given {@code securityToken}.
     *
     * @param securityToken to be validated
     * @return {@link UserInformation}
     * @throws ControllerReportSet.SecurityInvalidTokenException
     *          when the validation fails
     */
    public final UserInformation validate(SecurityToken securityToken)
            throws ControllerReportSet.SecurityInvalidTokenException
    {
        // Check not empty
        if (securityToken == null || securityToken.getAccessToken() == null) {
            throw new ControllerReportSet.SecurityMissingTokenException();
        }
        return onValidate(securityToken);
    }

    /**
     * @param securityToken
     * @param systemPermission
     * @return
     */
    public final boolean hasSystemPermission(SecurityToken securityToken, SystemPermission systemPermission)
    {
        if (isAdministrator(securityToken)) {
            // Administrator has all permissions
            return true;
        }
        String userId = securityToken.getUserId();
        switch (systemPermission) {
            case ADMINISTRATION: {
                return listGroupUserIds(getGroupIdByName(administratorGroupName)).contains(userId);
            }
            case RESERVATION: {
                // TODO: check some user attributes
                return true;
            }
            default: {
                throw new TodoImplementException(systemPermission);
            }
        }
    }

    /**
     * Retrieve {@link UserData} for given {@code securityToken}.
     *
     * @param securityToken of an user
     * @return {@link UserInformation} for the user with given {@code securityToken}
     * @throws ControllerReportSet.UserNotExistsException
     *          when user not exists
     */
    public final UserInformation getUserInformation(SecurityToken securityToken)
            throws ControllerReportSet.UserNotExistsException
    {
        UserInformation userInformation = securityToken.getUserInformation();
        if (userInformation != null) {
            return userInformation;
        }
        String accessToken = securityToken.getAccessToken();

        // Try to use the user-id from access token cache to get the user information
        String userId = cache.getUserIdByAccessToken(accessToken);
        if (userId != null) {
            logger.trace("Using cached user-id '{}' for access token '{}'...", userId, accessToken);
            userInformation = getUserData(userId).getUserInformation();

            // Store the user information inside the security token
            securityToken.setUserInformation(userInformation);

            return userInformation;
        }
        else {
            logger.debug("Retrieving user information by access token '{}'...", accessToken);

            UserData userData = onGetUserDataByAccessToken(accessToken);
            userInformation = userData.getUserInformation();
            userId = userInformation.getUserId();
            cache.putUserIdByAccessToken(accessToken, userId);
            cache.putUserDataByUserId(userId, userData);

            // Store the user information inside the security token
            securityToken.setUserInformation(userInformation);

            return userInformation;
        }
    }

    /**
     * Retrieve {@link UserData} for given {@code userId}.
     *
     * @param userId of an user
     * @return {@link UserInformation} for the user with given {@code userId}
     * @throws ControllerReportSet.UserNotExistsException
     *          when user not exists
     */
    public final UserInformation getUserInformation(String userId)
            throws ControllerReportSet.UserNotExistsException
    {
        UserData userData = getUserData(userId);
        return userData.getUserInformation();
    }

    /**
     * Retrieve {@link UserData} for given {@code principalName}.
     *
     * @param principalName of an user
     * @return {@link UserInformation} for the user with given {@code principalName}
     * @throws ControllerReportSet.UserNotExistsException
     *          when user not exists
     */
    public UserInformation getUserInformationByPrincipalName(String principalName)
            throws ControllerReportSet.UserNotExistsException
    {
        String userId;
        if (cache.hasUserIdByPrincipalName(principalName)) {
            userId = cache.getUserIdByPrincipalName(principalName);
        }
        else {
            try {
                userId = onGetUserIdByPrincipalName(principalName);
            }
            catch (ControllerReportSet.UserNotExistsException exception) {
                userId = null;
            }
            cache.putUserIdByPrincipalName(principalName, userId);
        }
        if (userId == null) {
            throw new ControllerReportSet.UserNotExistsException(principalName);
        }
        return getUserInformation(userId);
    }

    /**
     * Retrieve {@link UserData} for given {@code userId}.
     *
     * @param userId of an user
     * @return {@link UserData} for the user with given {@code userId}
     * @throws ControllerReportSet.UserNotExistsException
     *          when user not exists
     */
    public final UserData getUserData(String userId)
            throws ControllerReportSet.UserNotExistsException
    {
        // Root user
        if (userId.equals(ROOT_USER_ID)) {
            return ROOT_USER_DATA;
        }
        UserData userData;
        if (cache.hasUserDataByUserId(userId)) {
            userData = cache.getUserDataByUserId(userId);
        }
        else {
            try {
                userData = onGetUserDataByUserId(userId);
            }
            catch (ControllerReportSet.UserNotExistsException exception) {
                userData = null;
            }
            cache.putUserDataByUserId(userId, userData);
        }
        if (userData == null) {
            throw new ControllerReportSet.UserNotExistsException(userId);
        }
        return userData;
    }

    /**
     * Checks whether user with given {@code userId} exists.
     *
     * @param userId of the user to be checked for existence
     * @throws cz.cesnet.shongo.controller.ControllerReportSet.UserNotExistsException
     *          when the user doesn't exist
     */
    public void checkUserExistence(String userId)
            throws ControllerReportSet.UserNotExistsException
    {
        getUserData(userId);
    }

    /**
     * Retrieve a {@link UserPerson} by given {@code userId}.
     *
     * @param userId of an user
     * @return {@link UserData} for the user with given {@code userId}
     * @throws ControllerReportSet.UserNotExistsException
     *          when user not exists
     */
    public final UserPerson getUserPerson(String userId)
            throws ControllerReportSet.UserNotExistsException
    {
        return new UserPerson(userId, getUserData(userId).getUserInformation());
    }

    /**
     * Retrieve all {@link UserInformation}s which match given {@code search} criteria.
     *
     * @param search to filter users
     * @return collection of {@link UserInformation}s
     */
    public final Collection<UserInformation> listUserInformation(String search)
    {
        logger.debug("Retrieving list of user information...");
        List<UserInformation> userInformationList = new LinkedList<UserInformation>();
        for (UserData userData : onListUserData(search)) {
            userInformationList.add(userData.getUserInformation());
        }
        return userInformationList;
    }

    /**
     * @param securityToken
     * @return true if the user is Shongo admin (should have all permissions),
     *         false otherwise
     */
    public final boolean isAdministrator(SecurityToken securityToken)
    {
        return administratorAccessTokens.contains(securityToken.getAccessToken());
    }

    /**
     * @param securityToken    of the user
     * @param entityId         of the entity
     * @param entityPermission which the user must have for the entity
     * @return true if the user has given {@code permission} for the entity,
     *         false otherwise
     */
    public boolean hasEntityPermission(SecurityToken securityToken,
            AclRecord.EntityId entityId, EntityPermission entityPermission)
    {
        if (isAdministrator(securityToken)) {
            // Administrator has all possible permissions
            return true;
        }
        String userId = securityToken.getUserId();
        AclUserState aclUserState = cache.getAclUserStateByUserId(userId);
        if (aclUserState == null) {
            aclUserState = fetchAclUserState(userId);
            cache.putAclUserStateByUserId(userId, aclUserState);
        }
        return aclUserState.hasEntityPermission(entityId, entityPermission);
    }

    /**
     * @param securityToken    of the user
     * @param entity           the entity
     * @param entityPermission which the user must have for the entity
     * @return true if the user has given {@code permission} for the entity,
     *         false otherwise
     */
    public boolean hasEntityPermission(SecurityToken securityToken,
            PersistentObject entity, EntityPermission entityPermission)
    {
        return hasEntityPermission(securityToken, new AclRecord.EntityId(entity), entityPermission);
    }

    /**
     * @param securityToken of the user
     * @param entity        the entity
     * @return set of {@link cz.cesnet.shongo.controller.EntityPermission}s which the user have for the entity
     */
    public Set<EntityPermission> getEntityPermissions(SecurityToken securityToken, PersistentObject entity)
    {
        AclRecord.EntityId entityId = new AclRecord.EntityId(entity);
        if (isAdministrator(securityToken)) {
            // Administrator has all possible permissions
            EntityType entityType = entityId.getEntityType().getEntityType();
            return entityType.getPermissions();
        }
        String userId = securityToken.getUserId();
        AclUserState aclUserState = cache.getAclUserStateByUserId(userId);
        if (aclUserState == null) {
            aclUserState = fetchAclUserState(userId);
            cache.putAclUserStateByUserId(userId, aclUserState);
        }
        Set<EntityPermission> entityPermissions = aclUserState.getEntityPermissions(entityId);
        if (entityPermissions == null) {
            return Collections.emptySet();
        }
        return entityPermissions;
    }

    /**
     * @param securityToken    of the user
     * @param entityType       for entities which should be returned
     * @param entityPermission which the user must have for the entities
     * @return set of entity identifiers for which the user with given {@code userId} has given {@code permission}
     *         or null if the user can view all entities
     */
    public Set<Long> getEntitiesWithPermission(SecurityToken securityToken, AclRecord.EntityType entityType,
            EntityPermission entityPermission)
    {
        if (isAdministrator(securityToken)) {
            return null;
        }
        String userId = securityToken.getUserId();
        AclUserState aclUserState = cache.getAclUserStateByUserId(userId);
        if (aclUserState == null) {
            aclUserState = fetchAclUserState(userId);
            cache.putAclUserStateByUserId(userId, aclUserState);
        }
        Set<Long> entities = aclUserState.getEntitiesByPermission(entityType, entityPermission);
        if (entities == null) {
            return Collections.emptySet();
        }
        return entities;
    }

    /**
     * @param persistentObject for which the users must have given {@code role}
     * @param entityRole       which the users must have for given {@code persistentObject}
     * @return collection of {@link UserData} of users which have given {@code role}
     *         for given {@code persistentObject}
     */
    public Collection<UserInformation> getUsersWithRole(PersistentObject persistentObject, EntityRole entityRole)
    {
        AclRecord.EntityId entityId = new AclRecord.EntityId(persistentObject);
        AclEntityState aclEntityState = cache.getAclEntityStateByEntityId(entityId);
        if (aclEntityState == null) {
            aclEntityState = fetchAclEntityState(entityId);
            cache.putAclEntityStateByEntityId(entityId, aclEntityState);
        }
        Set<String> userIds = aclEntityState.getUserIdsByRole(entityRole);
        if (userIds == null) {
            return Collections.emptySet();
        }
        List<UserInformation> users = new LinkedList<UserInformation>();
        for (String userId : userIds) {
            users.add(getUserInformation(userId));
        }
        return users;
    }

    /**
     * @param securityToken
     * @return {@link UserSessionSettings} for given {@code securityToken}
     */
    public UserSessionSettings getUserSessionSettings(SecurityToken securityToken)
    {
        UserSessionSettings userSessionSettings = this.userSessionSettings.get(securityToken);
        if (userSessionSettings == null) {
            // Create new user session settings
            userSessionSettings = new UserSessionSettings(securityToken);
            // Store it
            this.userSessionSettings.put(securityToken, userSessionSettings);
        }
        return userSessionSettings;
    }

    /**
     * @param userSessionSettings to be updated
     */
    public void updateUserSessionSettings(UserSessionSettings userSessionSettings)
    {
        SecurityToken securityToken = userSessionSettings.getSecurityToken();
        if (userSessionSettings.getAdministratorMode()) {
            administratorAccessTokens.add(securityToken.getAccessToken());
        }
        else {
            administratorAccessTokens.remove(securityToken.getAccessToken());
        }
    }

    /**
     * @param groupName
     * @return group-id for given {@code groupName}
     * @throws ControllerReportSet.GroupNotExistsException
     *          when the group doesn't exist
     */
    public final String getGroupIdByName(String groupName)
    {
        String groupId = cache.getGroupIdByName(groupName);
        if (groupId == null) {
            for (Group group : listGroups()) {
                if (group.getName().equals(groupName)) {
                    groupId = group.getId();
                    break;
                }
            }
            if (groupId == null) {
                throw new ControllerReportSet.GroupNotExistsException(groupName);
            }
            cache.putGroupIdByName(groupName, groupId);
        }
        return groupId;
    }

    /**
     * @return list of {@link cz.cesnet.shongo.controller.api.Group}s
     */
    public final List<Group> listGroups()
    {
        return onListGroups();
    }

    /**
     * @return list of user-ids for users which are in group with given {@code groupId}
     */
    public final Set<String> listGroupUserIds(String groupId)
    {
        Set<String> userIds = cache.getUserIdsInGroup(groupId);
        if (userIds == null) {
            userIds = new HashSet<String>(onListGroupUserIds(groupId));
            cache.putUserIdsInGroup(groupId, userIds);
        }
        return userIds;
    }

    /**
     * @param group
     * @return identifier of the new group
     */
    public final String createGroup(Group group)
    {
        return onCreateGroup(group);
    }

    /**
     * @param groupId of the group to be deleted
     */
    public final void deleteGroup(String groupId)
    {
        onDeleteGroup(groupId);

        // Update cache
        cache.removeGroup(groupId);
    }

    /**
     * @param groupId of the group to which the user should be added
     * @param userId  of the user to be added
     */
    public final void addGroupUser(String groupId, String userId)
    {
        onAddGroupUser(groupId, userId);

        // Update cache
        Set<String> userIds = cache.getUserIdsInGroup(groupId);
        if (userIds != null) {
            userIds.add(userId);
        }
    }

    /**
     * @param groupId of the group from which the user should be removed
     * @param userId  of the user to be removed
     */
    public void removeGroupUser(String groupId, String userId)
    {
        onRemoveGroupUser(groupId, userId);

        // Update cache
        Set<String> userIds = cache.getUserIdsInGroup(groupId);
        if (userIds != null) {
            userIds.remove(userId);
        }
    }

    /**
     * Validate given {@code securityToken}.
     *
     * @param securityToken to be validated
     * @return {@link UserInformation}
     * @throws ControllerReportSet.SecurityInvalidTokenException
     *          when the validation fails
     */
    protected UserInformation onValidate(SecurityToken securityToken)
            throws ControllerReportSet.SecurityInvalidTokenException
    {
        // Validate access token by getting user info
        try {
            UserInformation userInformation = getUserInformation(securityToken);
            logger.trace("Access token '{}' is valid for {} (id: {}).",
                    new Object[]{securityToken.getAccessToken(), userInformation.getFullName(),
                            userInformation.getUserId()
                    });
            return userInformation;
        }
        catch (Exception exception) {
            String message = String.format("Access token '%s' cannot be validated.", securityToken.getAccessToken());
            Reporter.reportInternalError(Reporter.AUTHORIZATION, message, exception);
            throw new ControllerReportSet.SecurityInvalidTokenException(securityToken.getAccessToken());
        }
    }

    /**
     * Retrieve {@link UserData} for given {@code accessToken}.
     *
     * @param accessToken of an user
     * @return {@link UserData} for the user with given {@code accessToken}
     */
    protected abstract UserData onGetUserDataByAccessToken(String accessToken)
            throws ControllerReportSet.UserNotExistsException;

    /**
     * Retrieve {@link UserData} for given {@code userId}.
     *
     * @param userId of an user
     * @return {@link UserData} for the user with given {@code userId}
     */
    protected abstract UserData onGetUserDataByUserId(String userId)
            throws ControllerReportSet.UserNotExistsException;

    /**
     * Retrieve {@link UserData} for given {@code principalName}.
     *
     * @param principalName of an user
     * @return {@link UserData} for the user with given {@code principalName}
     */
    protected abstract String onGetUserIdByPrincipalName(String principalName)
            throws ControllerReportSet.UserNotExistsException;

    /**
     * Retrieve all {@link UserData}s which match given {@code search} criteria.
     *
     * @param search to filter {@link UserData}s
     * @return collection of {@link UserData}s
     */
    protected abstract Collection<UserData> onListUserData(String search);

    /**
     * @return list of {@link cz.cesnet.shongo.controller.api.Group}s
     */
    protected abstract List<Group> onListGroups();

    /**
     * @return list of user-ids for users which are in group with given {@code groupId}
     */
    protected abstract Set<String> onListGroupUserIds(String groupId);

    /**
     * @param group
     * @return identifier of the new group
     */
    protected abstract String onCreateGroup(Group group);

    /**
     * @param groupId of the group to be deleted
     */
    protected abstract void onDeleteGroup(String groupId);

    /**
     * @param groupId of the group to which the user should be added
     * @param userId  of the user to be added
     */
    protected abstract void onAddGroupUser(String groupId, String userId);

    /**
     * @param groupId of the group from which the user should be removed
     * @param userId  of the user to be removed
     */
    protected abstract void onRemoveGroupUser(String groupId, String userId);

    /**
     * Fetch {@link AclUserState} for given {@code userId}.
     *
     * @param userId of user for which the ACL should be fetched
     * @return fetched {@link AclUserState} for given {@code userId}
     */
    private AclUserState fetchAclUserState(String userId)
    {
        AclUserState aclUserState = new AclUserState();

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        AuthorizationManager authorizationManager = new AuthorizationManager(entityManager, authorization);
        try {
            for (AclRecord aclRecord : authorizationManager.listAclRecords(userId)) {
                aclUserState.addAclRecord(aclRecord);
                cache.putAclRecordById(aclRecord);
            }
        }
        finally {
            entityManager.close();
        }
        return aclUserState;
    }

    /**
     * Fetch {@link AclEntityState} for given {@code entityId}.
     *
     * @param entityId of entity for which the ACL should be fetched
     * @return fetched {@link AclEntityState} for given {@code entityId}
     */
    private AclEntityState fetchAclEntityState(AclRecord.EntityId entityId)
    {
        AclEntityState aclEntityState = new AclEntityState();
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        AuthorizationManager authorizationManager = new AuthorizationManager(entityManager, authorization);
        try {
            for (AclRecord aclRecord : authorizationManager.listAclRecords(entityId)) {
                aclEntityState.addAclRecord(aclRecord);
                cache.putAclRecordById(aclRecord);
            }
        }
        finally {
            entityManager.close();
        }
        return aclEntityState;
    }

    /**
     * Add given {@code aclRecord} to the {@link AuthorizationCache}.
     *
     * @param aclRecord to be added
     */
    void addAclRecordToCache(AclRecord aclRecord)
    {
        String userId = aclRecord.getUserId();
        AclRecord.EntityId entityId = aclRecord.getEntityId();

        // Update AclRecord cache
        cache.putAclRecordById(aclRecord);

        // Update AclUserState cache
        AclUserState aclUserState = cache.getAclUserStateByUserId(userId);
        if (aclUserState == null) {
            aclUserState = fetchAclUserState(userId);
            cache.putAclUserStateByUserId(userId, aclUserState);
        }
        else {
            aclUserState.addAclRecord(aclRecord);
        }

        // Update AclEntityState cache
        AclEntityState aclEntityState = cache.getAclEntityStateByEntityId(entityId);
        if (aclEntityState == null) {
            aclEntityState = fetchAclEntityState(entityId);
            cache.putAclEntityStateByEntityId(entityId, aclEntityState);
        }
        else {
            aclEntityState.addAclRecord(aclRecord);
        }
    }

    /**
     * Remove given {@code aclRecord} to the {@link AuthorizationCache}.
     *
     * @param aclRecord to be deleted
     */
    void removeAclRecordFromCache(AclRecord aclRecord)
    {
        // Update AclRecord cache
        cache.removeAclRecordById(aclRecord);

        // Update AclUserState cache
        String userId = aclRecord.getUserId();
        AclUserState aclUserState = cache.getAclUserStateByUserId(userId);
        if (aclUserState != null) {
            aclUserState.removeAclRecord(aclRecord);
        }

        // Update AclEntityState cache
        AclRecord.EntityId entityId = aclRecord.getEntityId();
        AclEntityState aclEntityState = cache.getAclEntityStateByEntityId(entityId);
        if (aclEntityState != null) {
            aclEntityState.removeAclRecord(aclRecord);
        }
    }

    /**
     * Single instance of {@link Authorization}.
     */
    protected static Authorization authorization;

    /**
     * @param authorization sets the {@link #authorization}
     * @throws IllegalStateException when the {@link #authorization} is already set
     */
    protected static void setInstance(Authorization authorization)
    {
        if (Authorization.authorization != null) {
            throw new IllegalStateException("Another instance of " + Authorization.class.getSimpleName()
                    + "has been created and wasn't destroyed.");
        }
        Authorization.authorization = authorization;
    }

    /**
     * @return {@link #authorization}
     * @throws IllegalStateException when the no {@link Authorization} has been created
     */
    public static Authorization getInstance() throws IllegalStateException
    {
        if (authorization == null) {
            throw new IllegalStateException("No instance of " + Authorization.class.getSimpleName()
                    + "has been created.");
        }
        return authorization;
    }

    /**
     * Represents user data fetched from web service.
     */
    public static class UserData
    {
        /**
         * @see UserInformation
         */
        private final UserInformation userInformation = new UserInformation();

        /**
         * Use preferred language.
         */
        private Locale locale;

        /**
         * @return {@link #userInformation}
         */
        public UserInformation getUserInformation()
        {
            return userInformation;
        }

        /**
         * @return {@link #userInformation#getUserId()}
         */
        public String getUserId()
        {
            return userInformation.getUserId();
        }

        /**
         * @return {@link #userInformation#getFullName()} ()}
         */
        public String getFullName()
        {
            return userInformation.getFullName();
        }

        /**
         * @return {@link #locale}
         */
        public Locale getLocale()
        {
            return locale;
        }

        /**
         * @param locale sets the {@link #locale}
         */
        public void setLocale(Locale locale)
        {
            this.locale = locale;
        }
    }
}
