package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.PersonInformation;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.authorization.AclEntityState;
import cz.cesnet.shongo.controller.authorization.AclRecord;
import cz.cesnet.shongo.controller.authorization.AclUserState;
import cz.cesnet.shongo.controller.authorization.AuthorizationCache;
import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.controller.common.OwnedPersistentObject;
import cz.cesnet.shongo.controller.common.UserPerson;
import cz.cesnet.shongo.controller.executor.Executable;
import cz.cesnet.shongo.controller.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.resource.Resource;
import cz.cesnet.shongo.fault.FaultException;
import cz.cesnet.shongo.fault.SecurityException;
import cz.cesnet.shongo.ssl.ConfiguredSSLContext;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import java.io.InputStream;
import java.util.*;

/**
 * Provides methods for performing authentication and authorization.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Authorization
{
    private static Logger logger = LoggerFactory.getLogger(Authorization.class);

    /**
     * User web service.
     */
    private static final String USER_WEB_SERVICE = "https://hroch.cesnet.cz/perun-ws/resource/user";

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
     * URL to authorization server.
     */
    private String authorizationServer;

    /**
     * Access token which won't be verified and can be used for testing purposes.
     */
    private String testingAccessToken;

    /**
     * @see AuthorizationCache
     */
    private AuthorizationCache cache = new AuthorizationCache();

    /**
     * Constructor.
     *
     * @param config to load authorization configuration from
     */
    private Authorization(Configuration config)
    {
        authorizationServer = config.getString(Configuration.SECURITY_AUTHORIZATION_SERVER);
        if (authorizationServer == null) {
            throw new IllegalStateException("Authorization server is not set in the configuration.");
        }
        cache.setUserIdExpiration(config.getDuration(Configuration.SECURITY_EXPIRATION_USER_ID));
        cache.setUserInformationExpiration(config.getDuration(Configuration.SECURITY_EXPIRATION_USER_INFORMATION));
        cache.setAclExpiration(config.getDuration(Configuration.SECURITY_EXPIRATION_ACL));
        testingAccessToken = config.getString(Configuration.SECURITY_TESTING_ACCESS_TOKEN);
    }

    /**
     * Destroy this {@link Authorization} (and you be able to call {@link #createInstance(Configuration)} again)
     */
    public void destroy()
    {
        authorization = null;
    }

    /**
     * @param testingAccessToken sets the {@link #testingAccessToken}
     */
    public void setTestingAccessToken(String testingAccessToken)
    {
        this.testingAccessToken = testingAccessToken;
    }

    /**
     * Validate given {@code securityToken}.
     *
     * @param securityToken to be validated
     * @return user-id
     */
    public String validate(SecurityToken securityToken)
    {
        // Check not empty
        if (securityToken == null || securityToken.getAccessToken() == null) {
            throw new cz.cesnet.shongo.fault.SecurityException(
                    SecurityToken.class.getSimpleName() + " should not be empty.");
        }
        // Always allow testing access token
        if (testingAccessToken != null && securityToken.getAccessToken().equals(testingAccessToken)) {
            logger.debug("Access token '{}' is valid for testing.", securityToken.getAccessToken());
            return ROOT_USER_ID;
        }

        // Validate access token by getting user info
        try {
            UserInformation userInformation = getUserInformation(securityToken);
            logger.debug("Access token '{}' is valid for {} (id: {}).",
                    new Object[]{securityToken.getAccessToken(), userInformation.getFullName(),
                            userInformation.getUserId()
                    });
            return userInformation.getUserId();
        }
        catch (Exception exception) {
            throw new SecurityException(exception, "Access token '%s' cannot be validated. %s",
                    securityToken.getAccessToken(), exception.getMessage());
        }
    }

    /**
     * @param securityToken of an user
     * @return {@link cz.cesnet.shongo.controller.common.Person} for user with given {@code securityToken}
     */
    public UserInformation getUserInformation(SecurityToken securityToken) throws IllegalStateException
    {
        PersonInformation personInformation = securityToken.getCachedPersonInformation();
        if (personInformation instanceof UserInformation) {
            return (UserInformation) personInformation;
        }
        String accessToken = securityToken.getAccessToken();

        // Testing security token represents root user
        if (testingAccessToken != null && accessToken.equals(testingAccessToken)) {
            return ROOT_USER_INFORMATION;
        }

        // Try to use the user-id from access token cache to get the user information
        String userId = cache.getUserIdByAccessToken(accessToken);
        if (userId != null) {
            logger.debug("Using cached user-id '{}' for access token '{}'...", userId, accessToken);
            UserInformation userInformation = getUserInformation(userId);

            // Store the user information inside the security token
            securityToken.setCachedPersonInformation(userInformation);

            return userInformation;
        }
        else {
            logger.debug("Retrieving user information by access token '{}'...", accessToken);

            Map<String, Object> content = null;
            try {
                // Build url
                URIBuilder uriBuilder = new URIBuilder(authorizationServer + "userinfo");
                uriBuilder.setParameter("schema", "openid");
                String url = uriBuilder.build().toString();

                // Perform request
                HttpGet httpGet = new HttpGet(url);
                httpGet.setHeader("Authorization", "Bearer " + accessToken);
                HttpClient httpClient = ConfiguredSSLContext.getInstance().createHttpClient();
                HttpResponse response = httpClient.execute(httpGet);
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    InputStream inputStream = entity.getContent();
                    ObjectMapper mapper = new ObjectMapper();
                    content = mapper.readValue(inputStream, Map.class);
                    inputStream.close();
                }
                if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                    String error = "Error";
                    if (content != null) {
                        error = String.format("Error: %s. %s", content.get("error"), content.get("error_description"));
                    }
                    throw new Exception(error);
                }
            }
            catch (Exception exception) {
                throw new IllegalStateException(exception);
            }
            UserInformation userInformation = createUserInformationFromData(content);
            userId = userInformation.getUserId();
            cache.putUserIdByAccessToken(accessToken, userId);
            cache.putUserInformationByUserId(userId, userInformation);

            // Store the user information inside the security token
            securityToken.setCachedPersonInformation(userInformation);

            return userInformation;
        }
    }

    /**
     * @param userId
     * @return {@link cz.cesnet.shongo.controller.common.Person} for user with given {@code userId}
     * @throws Exception
     */
    public UserInformation getUserInformation(String userId) throws IllegalStateException
    {
        // Root user
        if (userId.equals(ROOT_USER_ID)) {
            return ROOT_USER_INFORMATION;
        }

        // Try to use the user information from the cache
        UserInformation userInformation = cache.getUserInformationByUserId(userId);
        if (userInformation != null) {
            logger.debug("Using cached user information for user-id '{}'...", userId);
            return userInformation;
        }
        else {
            logger.debug("Retrieving user information by user-id '{}'...", userId);

            Map<String, Object> content = null;
            try {
                // Build url
                URIBuilder uriBuilder = new URIBuilder(USER_WEB_SERVICE + "/" + userId);
                String url = uriBuilder.build().toString();

                // Perform request
                HttpGet httpGet = new HttpGet(url);
                HttpClient httpClient = ConfiguredSSLContext.getInstance().createHttpClient();
                HttpResponse response = httpClient.execute(httpGet);
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    InputStream inputStream = entity.getContent();
                    ObjectMapper mapper = new ObjectMapper();
                    content = mapper.readValue(inputStream, Map.class);
                    inputStream.close();
                }
                if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                    throw new Exception("Error while retrieving user information by id.");
                }
            }
            catch (Exception exception) {
                throw new IllegalStateException(exception);
            }
            userInformation = createUserInformationFromData(content);
            cache.putUserInformationByUserId(userId, userInformation);
            return userInformation;
        }
    }

    /**
     * @return collection of {@link UserInformation}s
     * @throws IllegalStateException
     */
    public Collection<UserInformation> listUserInformation() throws IllegalStateException
    {
        logger.debug("Retrieving list of user information...");

        List content = null;
        try {
            // Build url
            URIBuilder uriBuilder = new URIBuilder(USER_WEB_SERVICE);
            String url = uriBuilder.build().toString();

            // Perform request
            HttpGet httpGet = new HttpGet(url);
            HttpClient httpClient = ConfiguredSSLContext.getInstance().createHttpClient();
            HttpResponse response = httpClient.execute(httpGet);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream inputStream = entity.getContent();
                ObjectMapper mapper = new ObjectMapper();
                content = mapper.readValue(inputStream, List.class);
                inputStream.close();
            }
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                throw new Exception("Error while retrieving user information.");
            }
        }
        catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
        List<UserInformation> userInformationList = new LinkedList<UserInformation>();
        if (content != null) {
            for (Object object : content) {
                if (object instanceof Map) {
                    UserInformation userInformation = createUserInformationFromData((Map) object);
                    userInformationList.add(userInformation);
                }
            }
        }
        return userInformationList;
    }

    /**
     * @param userId
     * @return {@link cz.cesnet.shongo.controller.common.Person} for user with given {@code userId}
     * @throws Exception
     */
    public UserPerson getUserPerson(String userId) throws IllegalStateException
    {
        return new UserPerson(userId, getUserInformation(userId));
    }

    /**
     * @param data
     * @return {@link UserInformation}
     */
    private UserInformation createUserInformationFromData(Map<String, Object> data)
    {
        if (!data.containsKey("id")) {
            throw new IllegalStateException("User information must contains identifier.");
        }
        if (!data.containsKey("given_name") || !data.containsKey("family_name")) {
            throw new IllegalStateException("User information must contains given and family name.");
        }
        UserInformation userInformation = new UserInformation();
        userInformation.setUserId((String) data.get("id"));
        userInformation.setFirstName((String) data.get("given_name"));
        userInformation.setLastName((String) data.get("family_name"));

        if (data.containsKey("original_id")) {
            userInformation.setEduPersonPrincipalName((String) data.get("original_id"));
        }
        if (data.containsKey("organization")) {
            userInformation.setOrganization((String) data.get("organization"));
        }
        if (data.containsKey("email")) {
            String emails = (String) data.get("email");
            if (emails != null) {
                for (String email : emails.split(";")) {
                    if (!email.isEmpty()) {
                        userInformation.addEmail(email);
                    }
                }
            }
        }

        return userInformation;
    }

    @SuppressWarnings("unchecked")
    Class<? extends OwnedPersistentObject>[] PUBLIC_ENTITIES = (Class<? extends OwnedPersistentObject>[]) new Class[]{
            Resource.class,
            AbstractReservationRequest.class,
            Reservation.class,
            Executable.class,
    };

    /**
     * @param userId of user for which the ACL should be fetched
     * @return fetched {@link cz.cesnet.shongo.controller.authorization.AclUserState} for given {@code userId}
     */
    public AclUserState fetchAclUserState(String userId)
    {
        AclUserState aclUserState = new AclUserState();

        // TODO: Fetch ACL from authorization server

        EntityManager entityManager = Controller.getInstance().getEntityManagerFactory().createEntityManager();
        try {
            for (Class<? extends OwnedPersistentObject> type : PUBLIC_ENTITIES) {
                List entities = entityManager.createQuery(
                        "SELECT entity FROM " + type.getSimpleName() + " entity WHERE entity.userId = :userId")
                        .setParameter("userId", userId)
                        .getResultList();
                for (Object entity : entities) {
                    OwnedPersistentObject ownedPersistentObject = type.cast(entity);
                    EntityIdentifier entityId = new EntityIdentifier(ownedPersistentObject);
                    AclRecord aclRecord = new AclRecord(userId, entityId, Role.OWNER);
                    aclUserState.addAclRecord(aclRecord);
                    cache.putAclRecordById(aclRecord);
                }
            }
        }
        finally {
            entityManager.close();
        }
        return aclUserState;
    }

    private AclEntityState fetchAclEntityState(EntityIdentifier entityId)
    {
        AclEntityState aclEntityState = new AclEntityState();

        // TODO: Fetch ACL from authorization server

        EntityManager entityManager = Controller.getInstance().getEntityManagerFactory().createEntityManager();
        try {
            Class type = entityId.getEntityClass();

            Object entity = entityManager.createQuery(
                    "SELECT entity FROM " + type.getSimpleName() + " entity WHERE entity.id = :entityId")
                    .setParameter("entityId", entityId.getPersistenceId())
                    .getSingleResult();
            OwnedPersistentObject ownedPersistentObject = (OwnedPersistentObject) entity;
            AclRecord aclRecord = new AclRecord(ownedPersistentObject.getUserId(), entityId, Role.OWNER);
            aclEntityState.addAclRecord(aclRecord);
            cache.putAclRecordById(aclRecord);
        }
        finally {
            entityManager.close();
        }
        return aclEntityState;
    }

    public AclRecord createAclRecord(String userId, EntityIdentifier entityId, Role role) throws SecurityException
    {
        EntityType entityType = entityId.getEntityType();
        if (!entityType.allowsRole(role)) {
            throw new SecurityException("Role is not allowed to specified entity");
        }

        // TODO: create ACL in authorization server

        AclRecord newAclRecord = new AclRecord(userId, entityId, role);

        // Update AclUserState cache
        AclUserState aclUserState = cache.getAclUserStateByUserId(userId);
        if (aclUserState == null) {
            aclUserState = fetchAclUserState(userId);
            cache.putAclUserStateByUserId(userId, aclUserState);
        }
        AclRecord addedAclRecord = aclUserState.addAclRecord(newAclRecord);
        // ACL already exists so return it
        if (addedAclRecord != newAclRecord) {
            return addedAclRecord;
        }

        // Update AclRecord cache
        cache.putAclRecordById(newAclRecord);

        // Update AclEntityState cache
        AclEntityState aclEntityState = cache.getAclEntityStateByEntityId(entityId);
        if (aclEntityState == null) {
            aclEntityState = fetchAclEntityState(entityId);
            cache.putAclEntityStateByEntityId(entityId, aclEntityState);
        }
        aclEntityState.addAclRecord(newAclRecord);

        return newAclRecord;
    }

    public void deleteAclRecord(AclRecord aclRecord) throws FaultException
    {
        // TODO: delete ACL in authorization server

        // Update AclRecord cache
        cache.removeAclRecordById(aclRecord);

        // Update AclUserState cache
        String userId = aclRecord.getUserId();
        AclUserState aclUserState = cache.getAclUserStateByUserId(userId);
        if (aclUserState == null) {
            aclUserState = fetchAclUserState(userId);
            cache.putAclUserStateByUserId(userId, aclUserState);
        }
        aclUserState.removeAclRecord(aclRecord);

        // Update AclEntityState cache
        EntityIdentifier entityId = aclRecord.getEntityId();
        AclEntityState aclEntityState = cache.getAclEntityStateByEntityId(entityId);
        if (aclEntityState == null) {
            aclEntityState = fetchAclEntityState(entityId);
            cache.putAclEntityStateByEntityId(entityId, aclEntityState);
        }
        aclEntityState.removeAclRecord(aclRecord);
    }

    public AclRecord getAclRecord(String aclRecordId) throws SecurityException
    {
        AclRecord aclRecord = cache.getAclRecordById(aclRecordId);
        if (aclRecord == null) {
            // TODO: get ACL from authorization server
        }
        if (aclRecord == null) {
            throw new SecurityException("ACL not found.");
        }
        return aclRecord;
    }

    public Collection<AclRecord> getAclRecords(String userId, EntityIdentifier entityId)
    {
        AclUserState aclUserState = cache.getAclUserStateByUserId(userId);
        if (aclUserState == null) {
            aclUserState = fetchAclUserState(userId);
            cache.putAclUserStateByUserId(userId, aclUserState);
        }
        Set<AclRecord> aclRecords = aclUserState.getAclRecords(entityId);
        if (aclRecords == null) {
            return Collections.emptySet();
        }
        return aclRecords;
    }

    public Collection<AclRecord> getAclRecords(EntityIdentifier entityId)
    {
        AclEntityState aclEntityState = cache.getAclEntityStateByEntityId(entityId);
        if (aclEntityState == null) {
            aclEntityState = fetchAclEntityState(entityId);
            cache.putAclEntityStateByEntityId(entityId, aclEntityState);
        }
        Set<AclRecord> aclRecords = aclEntityState.getAclRecords();
        if (aclRecords == null) {
            return Collections.emptySet();
        }
        return aclRecords;
    }

    public Collection<AclRecord> getAclRecords(String userId, EntityIdentifier entityId, Role role)
    {
        // ToDO: get ACL from authorization server

        List<AclRecord> aclRecords = new LinkedList<AclRecord>();
        for (AclRecord aclRecord : cache.getAclRecords()) {
            if (userId != null && !userId.equals(aclRecord.getUserId())) {
                continue;
            }
            if (entityId != null && !entityId.equals(aclRecord.getEntityId())) {
                continue;
            }
            if (role != null && !role.equals(aclRecord.getRole())) {
                continue;
            }
            aclRecords.add(aclRecord);
        }
        return aclRecords;
    }

    public Set<Permission> getPermissions(String userId, EntityIdentifier entityId)
    {
        if (userId.equals(ROOT_USER_ID)) {
            // Root user has all possible permissions
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

    public void checkPermission(String userId, EntityIdentifier entityId, Permission permission)
            throws SecurityException
    {
        Set<Permission> permissions = getPermissions(userId, entityId);
        if (!permissions.contains(permission)) {
            throw new SecurityException("User with id '%s' doesn't have '%s' permission for the '%s'",
                    userId, permission.getCode(), entityId);
        }
    }

    /**
     * Single instance of {@link Authorization} which is used for retrieving {@link UserInformation}.
     */
    private static Authorization authorization;

    /**
     * @return new instance of {@link Authorization}
     * @throws IllegalStateException when other {@link Authorization} already exists
     */
    public static Authorization createInstance(Configuration configuration) throws IllegalStateException
    {
        if (authorization != null) {
            throw new IllegalStateException("Another instance of " + Authorization.class.getSimpleName()
                    + "has been created and wasn't destroyed.");
        }
        authorization = new Authorization(configuration);
        return authorization;
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
     * Class for verifying user permissions.
     */
    public static class PermissionHelper
    {
        /**
         * @param userId
         * @param resource
         * @return true if user with given {@code userId} is onwer of given {@code resource},
         *         false otherwise
         */
        public static boolean isUserOwner(String userId, Resource resource)
        {
            return resource.getUserId().equals(userId);
        }

        /**
         * @param executable
         * @return list of {@link UserInformation}s for owners of given {@code executable}
         */
        public static List<UserInformation> getExecutableOwners(Executable executable)
        {
            List<UserInformation> executableOwners = new LinkedList<UserInformation>();
            executableOwners.add(getInstance().getUserInformation(executable.getUserId()));
            return executableOwners;
        }
    }


}
