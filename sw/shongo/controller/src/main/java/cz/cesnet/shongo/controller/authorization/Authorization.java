package cz.cesnet.shongo.controller.authorization;

import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.*;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.controller.common.UserPerson;
import cz.cesnet.shongo.controller.report.InternalErrorHandler;
import cz.cesnet.shongo.controller.report.InternalErrorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.*;

/**
 * Provides methods for performing authentication and authorization.
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
     * Root user {@link cz.cesnet.shongo.controller.common.Person}.
     */
    public static final UserInformation ROOT_USER_INFORMATION;

    /**
     * Static initialization.
     */
    static {
        ROOT_USER_INFORMATION = new UserInformation();
        ROOT_USER_INFORMATION.setUserId(ROOT_USER_ID);
        ROOT_USER_INFORMATION.setFirstName("root");
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
     * Constructor.
     *
     * @param config to load authorization configuration from
     */
    protected Authorization(Configuration config)
    {
        cache.setUserIdExpiration(config.getDuration(Configuration.SECURITY_EXPIRATION_USER_ID));
        cache.setUserInformationExpiration(config.getDuration(Configuration.SECURITY_EXPIRATION_USER_INFORMATION));
        cache.setAclExpiration(config.getDuration(Configuration.SECURITY_EXPIRATION_ACL));
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
     * Validate given {@code securityToken}.
     *
     * @param securityToken to be validated
     * @return user-id
     */
    public final String validate(SecurityToken securityToken)
    {
        // Check not empty
        if (securityToken == null || securityToken.getAccessToken() == null) {
            throw new RuntimeException(SecurityToken.class.getSimpleName() + " should not be empty.");
        }
        return onValidate(securityToken);
    }

    /**
     * Retrieve {@link UserInformation} for given {@code securityToken}.
     *
     * @param securityToken of an user
     * @return {@link UserInformation} for the user with given {@code securityToken}
     */
    public final UserInformation getUserInformation(SecurityToken securityToken)
    {
        UserInformation userInformation = securityToken.getCachedUserInformation();
        if (userInformation != null) {
            return userInformation;
        }
        String accessToken = securityToken.getAccessToken();

        // Try to use the user-id from access token cache to get the user information
        String userId = cache.getUserIdByAccessToken(accessToken);
        if (userId != null) {
            logger.trace("Using cached user-id '{}' for access token '{}'...", userId, accessToken);
            userInformation = getUserInformation(userId);

            // Store the user information inside the security token
            securityToken.setCachedUserInformation(userInformation);

            return userInformation;
        }
        else {
            logger.debug("Retrieving user information by access token '{}'...", accessToken);

            userInformation = onGetUserInformationByAccessToken(accessToken);
            userId = userInformation.getUserId();
            cache.putUserIdByAccessToken(accessToken, userId);
            cache.putUserInformationByUserId(userId, userInformation);

            // Store the user information inside the security token
            securityToken.setCachedUserInformation(userInformation);

            return userInformation;
        }
    }

    /**
     * Retrieve {@link UserInformation} for given {@code userId}.
     *
     * @param userId of an user
     * @return {@link UserInformation} for the user with given {@code userId}
     */
    public final UserInformation getUserInformation(String userId)
    {
        // Root user
        if (userId.equals(ROOT_USER_ID)) {
            return ROOT_USER_INFORMATION;
        }

        // Try to use the user information from the cache
        UserInformation userInformation = cache.getUserInformationByUserId(userId);
        if (userInformation != null) {
            logger.trace("Using cached user information for user-id '{}'...", userId);
            return userInformation;
        }
        else {
            logger.debug("Retrieving user information by user-id '{}'...", userId);

            userInformation = onGetUserInformationByUserId(userId);
            cache.putUserInformationByUserId(userId, userInformation);
            return userInformation;
        }
    }

    /**
     * Retrieve a {@link UserPerson} by given {@code userId}.
     *
     * @param userId of an user
     * @return {@link UserInformation} for the user with given {@code userId}
     */
    public final UserPerson getUserPerson(String userId)
    {
        return new UserPerson(userId, getUserInformation(userId));
    }

    /**
     * Retrieve all {@link UserInformation}s.
     *
     * @return collection of {@link UserInformation}s
     */
    public final Collection<UserInformation> listUserInformation()
    {
        logger.debug("Retrieving list of user information...");

        return onListUserInformation();
    }

    /**
     * @param userId
     * @return true if the use is Shongo admin (should have all permissions),
     *         false otherwise
     */
    public final boolean isAdmin(String userId)
    {
        return userId.equals(Authorization.ROOT_USER_ID);
    }

    /**
     * @param aclRecordId of the {@link AclRecord}
     * @return {@link AclRecord} with given {@code aclRecordId}
     */
    public final AclRecord getAclRecord(Long aclRecordId)
    {
        AclRecord aclRecord = cache.getAclRecordById(aclRecordId);
        if (aclRecord == null) {
            EntityManager entityManager = entityManagerFactory.createEntityManager();
            AuthorizationManager authorizationManager = new AuthorizationManager(entityManager);
            try {
                aclRecord = authorizationManager.getAclRecord(aclRecordId);
            }
            finally {
                entityManager.close();
            }
            cache.putAclRecordById(aclRecord);
        }
        return aclRecord;
    }

    /**
     * @param userId
     * @param entityId
     * @param role
     * @return {@link AclRecord} for given parameters or null if doesn't exist
     */
    public final AclRecord getAclRecord(String userId, EntityIdentifier entityId, Role role)
    {
        for (AclRecord aclRecord : getAclRecords(userId, entityId)) {
            if (role.equals(aclRecord.getRole())) {
                return aclRecord;
            }
        }
        return null;
    }

    /**
     * Retrieve all {@link AclRecord}s for given {@code userId} and {@code entityId}.
     *
     * @param userId   of the user
     * @param entityId to restrict the entity of the {@link AclRecord}
     * @return collection of matching {@link AclRecord}s
     */
    public Collection<AclRecord> getAclRecords(String userId, EntityIdentifier entityId)
    {
        AclUserState aclUserState = cache.getAclUserStateByUserId(userId);
        if (aclUserState == null) {
            aclUserState = fetchAclUserState(userId);
            cache.putAclUserStateByUserId(userId, aclUserState);
        }
        Collection<AclRecord> aclRecords = aclUserState.getAclRecords(entityId);
        if (aclRecords == null) {
            return Collections.emptyList();
        }
        return aclRecords;
    }

    /**
     * Retrieve all {@link AclRecord}s for given {@code entityId}.
     *
     * @param entityId to restrict the entity of the {@link AclRecord}
     * @return collection of matching {@link AclRecord}s
     */
    public Collection<AclRecord> getAclRecords(EntityIdentifier entityId)
    {
        AclEntityState aclEntityState = cache.getAclEntityStateByEntityId(entityId);
        if (aclEntityState == null) {
            aclEntityState = fetchAclEntityState(entityId);
            cache.putAclEntityStateByEntityId(entityId, aclEntityState);
        }
        Collection<AclRecord> aclRecords = aclEntityState.getAclRecords();
        if (aclRecords == null) {
            return Collections.emptyList();
        }
        return aclRecords;
    }

    /**
     * List of all {@link AclRecord}s which matches given criteria.
     *
     * @param userId   to restrict the user of the {@link AclRecord}
     * @param entityId to restrict the entity of the {@link AclRecord}
     * @param role     to restrict the role of the {@link AclRecord}
     * @return collection of matching {@link AclRecord}s
     */
    public final Collection<AclRecord> getAclRecords(String userId, EntityIdentifier entityId, Role role)
    {
        if (entityId != null && !entityId.isGroup()) {
            if (userId != null) {
                if (role != null) {
                    Collection<AclRecord> aclRecords = new LinkedList<AclRecord>();
                    for (AclRecord aclRecord : getAclRecords(userId, entityId)) {
                        if (role.equals(aclRecord.getRole())) {
                            aclRecords.add(aclRecord);
                        }
                    }
                    return aclRecords;
                }
                else {
                    return getAclRecords(userId, entityId);

                }
            }
            else {
                return getAclRecords(entityId);
            }
        }
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        AuthorizationManager authorizationManager = new AuthorizationManager(entityManager);
        try {
            return authorizationManager.listAclRecords(userId, entityId, role);
        }
        finally {
            entityManager.close();
        }
    }

    /**
     * @param userId     of the user
     * @param entityId   of the entity
     * @param permission which the user must have for the entity
     * @return true if the user has given {@code permission} for the entity,
     *         false otherwise
     */
    public boolean hasPermission(String userId, EntityIdentifier entityId, Permission permission)
    {
        if (isAdmin(userId)) {
            // Administrator has all possible permissions
            return true;
        }
        AclUserState aclUserState = cache.getAclUserStateByUserId(userId);
        if (aclUserState == null) {
            aclUserState = fetchAclUserState(userId);
            cache.putAclUserStateByUserId(userId, aclUserState);
        }
        return aclUserState.hasPermission(entityId, permission);
    }

    /**
     * @param userId   of the user
     * @param entityId of the entity
     * @return set of {@link Permission}s which the user have for the entity
     */
    public Set<Permission> getPermissions(String userId, EntityIdentifier entityId)
    {
        if (isAdmin(userId)) {
            // Administrator has all possible permissions
            EntityType entityType = entityId.getEntityType();
            return entityType.getPermissions();
        }
        AclUserState aclUserState = cache.getAclUserStateByUserId(userId);
        if (aclUserState == null) {
            aclUserState = fetchAclUserState(userId);
            cache.putAclUserStateByUserId(userId, aclUserState);
        }
        Set<Permission> permissions = aclUserState.getPermissions(entityId);
        if (permissions == null) {
            return Collections.emptySet();
        }
        return permissions;
    }

    /**
     * @param userId     of the user
     * @param entityType for entities which should be returned
     * @param permission which the user must have for the entities
     * @return set of entity identifiers for which the user with given {@code userId} has given {@code permission}
     *         or null if the user can view all entities
     */
    public Set<Long> getEntitiesWithPermission(String userId, EntityType entityType, Permission permission)
    {
        if (isAdmin(userId)) {
            return null;
        }
        AclUserState aclUserState = cache.getAclUserStateByUserId(userId);
        if (aclUserState == null) {
            aclUserState = fetchAclUserState(userId);
            cache.putAclUserStateByUserId(userId, aclUserState);
        }
        Set<Long> entities = aclUserState.getEntitiesByPermission(entityType, permission);
        if (entities == null) {
            return Collections.emptySet();
        }
        return entities;
    }

    /**
     * @param persistentObject for which the users must have given {@code role}
     * @param role             which the users must have for given {@code persistentObject}
     * @return collection of user-ids for users which have given {@code role}
     *         for given {@code persistentObject}
     */
    public Set<String> getUserIdsWithRole(PersistentObject persistentObject, Role role)
    {
        EntityIdentifier entityId = new EntityIdentifier(persistentObject);
        AclEntityState aclEntityState = cache.getAclEntityStateByEntityId(entityId);
        if (aclEntityState == null) {
            aclEntityState = fetchAclEntityState(entityId);
            cache.putAclEntityStateByEntityId(entityId, aclEntityState);
        }
        Set<String> userIds = aclEntityState.getUserIdsByRole(role);
        if (userIds == null) {
            return Collections.emptySet();
        }
        return userIds;
    }

    /**
     * @param persistentObject for which the users must have given {@code role}
     * @param role             which the users must have for given {@code persistentObject}
     * @return collection of {@link UserInformation} of users which have given {@code role}
     *         for given {@code persistentObject}
     */
    public Collection<UserInformation> getUsersWithRole(PersistentObject persistentObject, Role role)
    {
        List<UserInformation> users = new LinkedList<UserInformation>();
        for (String userId : getUserIdsWithRole(persistentObject, role)) {
            users.add(getUserInformation(userId));
        }
        return users;
    }

    /**
     * Validate given {@code securityToken}.
     *
     * @param securityToken to be validated
     * @return user-id
     * @throws ControllerReportSet.SecurityInvalidTokenException when the validation fails
     */
    protected String onValidate(SecurityToken securityToken) throws ControllerReportSet.SecurityInvalidTokenException
    {
        // Validate access token by getting user info
        try {
            UserInformation userInformation = getUserInformation(securityToken);
            logger.trace("Access token '{}' is valid for {} (id: {}).",
                    new Object[]{securityToken.getAccessToken(), userInformation.getFullName(),
                            userInformation.getUserId()
                    });
            return userInformation.getUserId();
        }
        catch (Exception exception) {
            String message = String.format("Access token '%s' cannot be validated.", securityToken.getAccessToken());
            InternalErrorHandler.handle(InternalErrorType.AUTHORIZATION, message, exception);
            throw new ControllerReportSet.SecurityInvalidTokenException(securityToken.getAccessToken());
        }
    }

    /**
     * Retrieve {@link UserInformation} for given {@code accessToken}.
     *
     * @param accessToken of an user
     * @return {@link UserInformation} for the user with given {@code accessToken}
     */
    protected abstract UserInformation onGetUserInformationByAccessToken(String accessToken);

    /**
     * Retrieve {@link UserInformation} for given {@code userId}.
     *
     * @param userId of an user
     * @return {@link UserInformation} for the user with given {@code userId}
     */
    protected abstract UserInformation onGetUserInformationByUserId(String userId);

    /**
     * Retrieve all {@link UserInformation}s.
     *
     * @return collection of {@link UserInformation}s
     */
    protected abstract Collection<UserInformation> onListUserInformation();

    /**
     * @param aclRecord to be created on the server
     * @return new {@link AclRecord}
     */
    protected abstract void onPropagateAclRecordCreation(AclRecord aclRecord);

    /**
     * @param aclRecord to be deleted on the server
     */
    protected abstract void onPropagateAclRecordDeletion(AclRecord aclRecord);

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
        AuthorizationManager authorizationManager = new AuthorizationManager(entityManager);
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
    private AclEntityState fetchAclEntityState(EntityIdentifier entityId)
    {
        AclEntityState aclEntityState = new AclEntityState();

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        AuthorizationManager authorizationManager = new AuthorizationManager(entityManager);
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
    public void addAclRecordToCache(AclRecord aclRecord)
    {
        String userId = aclRecord.getUserId();
        EntityIdentifier entityId = aclRecord.getEntityId();
        Role role = aclRecord.getRole();

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
        EntityIdentifier entityId = aclRecord.getEntityId();
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
}
