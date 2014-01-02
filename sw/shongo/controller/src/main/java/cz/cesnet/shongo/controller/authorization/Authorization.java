package cz.cesnet.shongo.controller.authorization;

import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.*;
import cz.cesnet.shongo.controller.acl.*;
import cz.cesnet.shongo.controller.api.Group;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.booking.Allocation;
import cz.cesnet.shongo.controller.booking.ObjectTypeResolver;
import cz.cesnet.shongo.controller.booking.person.UserPerson;
import cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest;
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
     * @see cz.cesnet.shongo.controller.acl.AclProvider
     */
    private AclProvider aclProvider;

    /**
     * @see AuthorizationCache
     */
    private AuthorizationCache cache = new AuthorizationCache();

    /**
     * Set of access-tokens which has administrator access.
     */
    protected Set<String> administratorAccessTokens = new HashSet<String>();

    /**
     * {@link cz.cesnet.shongo.controller.settings.UserSessionSettings}s.
     */
    private Map<SecurityToken, UserSessionSettings> userSessionSettings = new HashMap<SecurityToken, UserSessionSettings>();

    /**
     * {@link AuthorizationExpression} for decision whether an user can perform administration.
     */
    private AuthorizationExpression administrationExpression;

    /**
     * {@link AuthorizationExpression} for decision whether an user can create reservation.
     */
    private AuthorizationExpression reservationExpression;

    /**
     * Constructor.
     *
     * @param configuration to load authorization configuration from
     */
    protected Authorization(ControllerConfiguration configuration, EntityManagerFactory entityManagerFactory)
    {
        this.entityManagerFactory = entityManagerFactory;
        this.aclProvider = new AclProvider(entityManagerFactory)
        {
            @Override
            protected String getObjectClassName(Class<? extends PersistentObject> objectClass)
            {
                if (objectClass.equals(Allocation.class)) {
                    objectClass = AbstractReservationRequest.class;
                }
                return ObjectTypeResolver.getObjectType(objectClass).toString();
            }

            @Override
            protected Long getObjectId(PersistentObject object)
            {
                if (object instanceof AbstractReservationRequest) {
                    AbstractReservationRequest reservationRequest = (AbstractReservationRequest) object;
                    return reservationRequest.getAllocation().getId();
                }
                else {
                    return object.getId();
                }
            }
        };
        this.cache.setUserIdExpiration(configuration.getDuration(
                ControllerConfiguration.SECURITY_EXPIRATION_USER_ID));
        this.cache.setUserInformationExpiration(configuration.getDuration(
                ControllerConfiguration.SECURITY_EXPIRATION_USER_INFORMATION));
        this.cache.setAclExpiration(configuration.getDuration(
                ControllerConfiguration.SECURITY_EXPIRATION_ACL));
        this.cache.setGroupExpiration(configuration.getDuration(
                ControllerConfiguration.SECURITY_EXPIRATION_GROUP));

        // Authorization expressions
        this.administrationExpression = new AuthorizationExpression(
                configuration.getString(ControllerConfiguration.SECURITY_AUTHORIZATION_ADMINISTRATION), this);
        this.reservationExpression = new AuthorizationExpression(
                configuration.getString(ControllerConfiguration.SECURITY_AUTHORIZATION_RESERVATION), this);
    }

    /**
     * Initialize authorization
     */
    protected void initialize()
    {
        // Try to evaluate authorization expressions for root user
        UserInformation rootUserInformation = ROOT_USER_DATA.getUserInformation();
        UserAuthorizationData rootUserAuthorizationData = ROOT_USER_DATA.getUserAuthorizationData();
        this.administrationExpression.evaluate(rootUserInformation, rootUserAuthorizationData);
        this.reservationExpression.evaluate(rootUserInformation, rootUserAuthorizationData);
    }

    /**
     * Destroy this {@link Authorization} (and you be able to create another {@link Authorization} instance again)
     */
    public void destroy()
    {
        authorization = null;
    }

    /**
     * @return {@link cz.cesnet.shongo.controller.acl.AclProvider}
     */
    public AclProvider getAclProvider()
    {
        return aclProvider;
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
     * @return true whether user with given {@code securityToken} has given {@code systemPermission}
     */
    public final boolean hasSystemPermission(SecurityToken securityToken, SystemPermission systemPermission)
    {
        if (isAdministrator(securityToken)) {
            // Administrator has all permissions
            return true;
        }
        UserInformation userInformation = securityToken.getUserInformation();
        UserAuthorizationData userAuthorizationData = getUserAuthorizationData(securityToken);
        switch (systemPermission) {
            case ADMINISTRATION: {
                return administrationExpression.evaluate(userInformation, userAuthorizationData);
            }
            case RESERVATION: {
                return reservationExpression.evaluate(userInformation, userAuthorizationData);
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
     * @param entity           the entity
     * @param objectPermission which the user must have for the entity
     * @return true if the user has given {@code permission} for the entity,
     *         false otherwise
     */
    public boolean hasObjectPermission(SecurityToken securityToken,
            PersistentObject entity, ObjectPermission objectPermission)
    {
        AclObjectIdentity objectIdentity = aclProvider.getObjectIdentity(entity);
        return hasObjectPermission(securityToken, objectIdentity, objectPermission);
    }

    /**
     * @param securityToken    of the user
     * @param objectIdentity           the entity
     * @param objectPermission which the user must have for the entity
     * @return true if the user has given {@code permission} for the entity,
     *         false otherwise
     */
    public boolean hasObjectPermission(SecurityToken securityToken,
            AclObjectIdentity objectIdentity, ObjectPermission objectPermission)
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
        return aclUserState.hasObjectPermission(objectIdentity, objectPermission);
    }

    /**
     * @param securityToken of the user
     * @param object        the object for which the permissions should be returned
     * @return set of {@link ObjectPermission}s which the user have for the object
     */
    public Set<ObjectPermission> getObjectPermissions(SecurityToken securityToken, PersistentObject object)
    {
        if (isAdministrator(securityToken)) {
            // Administrator has all possible permissions
            ObjectType objectType = ObjectTypeResolver.getObjectType(object);
            return objectType.getPermissions();
        }
        String userId = securityToken.getUserId();
        AclUserState aclUserState = cache.getAclUserStateByUserId(userId);
        if (aclUserState == null) {
            aclUserState = fetchAclUserState(userId);
            cache.putAclUserStateByUserId(userId, aclUserState);
        }
        AclObjectIdentity aclObjectIdentity = aclProvider.getObjectIdentity(object);
        Set<ObjectPermission> objectPermissions = aclUserState.getObjectPermissions(aclObjectIdentity);
        if (objectPermissions == null) {
            return Collections.emptySet();
        }
        return objectPermissions;
    }

    /**
     * @param securityToken    of the user
     * @param objectClass of objects which should be returned
     * @param objectPermission which the user must have for the entities
     * @return set of object identifiers for which the user with given {@code userId} has given {@code permission}
     *         or null if the user can view all objects
     */
    public Set<Long> getEntitiesWithPermission(SecurityToken securityToken, AclObjectClass objectClass,
            ObjectPermission objectPermission)
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
        Set<Long> entities = aclUserState.getObjectsByPermission(objectClass, objectPermission);
        if (entities == null) {
            return Collections.emptySet();
        }
        return entities;
    }

    /**
     * @param persistentObject for which the users must have given {@code role}
     * @param objectRole       which the users must have for given {@code persistentObject}
     * @return collection of {@link UserData} of users which have given {@code role}
     *         for given {@code persistentObject}
     */
    public Collection<UserInformation> getUsersWithRole(PersistentObject persistentObject, ObjectRole objectRole)
    {
        AclObjectIdentity aclObjectIdentity = aclProvider.getObjectIdentity(persistentObject);
        AclObjectState aclObjectState = cache.getAclObjectStateByIdentity(aclObjectIdentity);
        if (aclObjectState == null) {
            aclObjectState = fetchAclObjectState(aclObjectIdentity);
            cache.putAclObjectStateByIdentity(aclObjectIdentity, aclObjectState);
        }
        Set<String> userIds = aclObjectState.getUserIdsByRole(objectRole);
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
     * @param securityToken
     * @return {@link UserAuthorizationData} for given {@code securityToken}
     */
    public UserAuthorizationData getUserAuthorizationData(SecurityToken securityToken)
    {
        String accessToken = securityToken.getAccessToken();
        UserAuthorizationData userAuthorizationData;
        if (cache.hasUserAuthorizationDataByAccessToken(accessToken)) {
            userAuthorizationData = cache.getUserAuthorizationDataByAccessToken(accessToken);
        }
        else {
            try {
                UserData userData = onGetUserDataByAccessToken(accessToken);
                userAuthorizationData = userData.getUserAuthorizationData();
                if (userAuthorizationData == null) {
                    userAuthorizationData = new UserAuthorizationData(UserAuthorizationData.LOA_NONE);
                }
            }
            catch (ControllerReportSet.UserNotExistsException exception) {
                userAuthorizationData = null;
            }
            cache.putUserAuthorizationDataByAccessToken(accessToken, userAuthorizationData);
        }
        if (userAuthorizationData == null) {
            throw new ControllerReportSet.UserNotExistsException(accessToken);
        }
        return userAuthorizationData;
    }

    /**
     * Fetch {@link AclUserState} for given {@code userId}.
     *
     * @param userId of user for which the ACL should be fetched
     * @return fetched {@link AclUserState} for given {@code userId}
     */
    private AclUserState fetchAclUserState(String userId)
    {
        AclUserState aclUserState = new AclUserState();
        AclIdentity aclIdentity = aclProvider.getIdentity(AclIdentityType.USER, userId);
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        AuthorizationManager authorizationManager = new AuthorizationManager(entityManager, authorization);
        try {
            for (AclEntry aclEntry : authorizationManager.listAclEntries(aclIdentity)) {
                aclUserState.addAclEntry(aclEntry);
                cache.putAclEntryById(aclEntry);
            }
        }
        finally {
            entityManager.close();
        }
        return aclUserState;
    }

    /**
     * Fetch {@link AclObjectState} for given {@code aclObjectIdentity}.
     *
     * @param aclObjectIdentity of object for which the ACL should be fetched
     * @return fetched {@link AclObjectState} for given {@code aclObjectIdentity}
     */
    private AclObjectState fetchAclObjectState(AclObjectIdentity aclObjectIdentity)
    {
        AclObjectState aclObjectState = new AclObjectState();
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        AuthorizationManager authorizationManager = new AuthorizationManager(entityManager, authorization);
        try {
            for (AclEntry aclEntry : authorizationManager.listAclEntries(aclObjectIdentity)) {
                aclObjectState.addAclEntry(aclEntry);
                cache.putAclEntryById(aclEntry);
            }
        }
        finally {
            entityManager.close();
        }
        return aclObjectState;
    }

    /**
     * Add given {@code aclEntry} to the {@link AuthorizationCache}.
     *
     * @param aclEntry to be added
     */
    void addAclEntryToCache(AclEntry aclEntry)
    {
        // Update AclEntry cache
        cache.putAclEntryById(aclEntry);

        // Update AclUserState cache
        for (String userId : getUserIds(aclEntry.getIdentity())) {
            AclUserState aclUserState = cache.getAclUserStateByUserId(userId);
            if (aclUserState == null) {
                aclUserState = fetchAclUserState(userId);
                cache.putAclUserStateByUserId(userId, aclUserState);
            }
            else {
                aclUserState.addAclEntry(aclEntry);
            }
        }

        // Update AclObjectState cache
        AclObjectIdentity aclObjectIdentity = aclEntry.getObjectIdentity();
        AclObjectState aclObjectState = cache.getAclObjectStateByIdentity(aclObjectIdentity);
        if (aclObjectState == null) {
            aclObjectState = fetchAclObjectState(aclObjectIdentity);
            cache.putAclObjectStateByIdentity(aclObjectIdentity, aclObjectState);
        }
        else {
            aclObjectState.addAclEntry(aclEntry);
        }
    }

    /**
     * @param aclIdentity
     * @return collection of user-id for given {@code aclIdentity}
     */
    public Set<String> getUserIds(AclIdentity aclIdentity)
    {
        switch (aclIdentity.getType()) {
            case USER:
                return Collections.singleton(aclIdentity.getPrincipalId());
            default:
                throw new TodoImplementException(aclIdentity.getType());
        }
    }

    /**
     * Remove given {@code aclEntry} to the {@link AuthorizationCache}.
     *
     * @param aclEntry to be deleted
     */
    void removeAclEntryFromCache(AclEntry aclEntry)
    {
        // Update AclEntry cache
        cache.removeAclEntryById(aclEntry);

        // Update AclUserState cache
        for (String userId : getUserIds(aclEntry.getIdentity())) {
            AclUserState aclUserState = cache.getAclUserStateByUserId(userId);
            if (aclUserState != null) {
                aclUserState.removeAclEntry(aclEntry);
            }
        }

        // Update AclObjectState cache
        AclObjectIdentity aclObjectIdentity = aclEntry.getObjectIdentity();
        AclObjectState aclObjectState = cache.getAclObjectStateByIdentity(aclObjectIdentity);
        if (aclObjectState != null) {
            aclObjectState.removeAclEntry(aclEntry);
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
}
