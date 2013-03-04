package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.PersonInformation;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.common.UserPerson;
import cz.cesnet.shongo.controller.executor.Executable;
import cz.cesnet.shongo.controller.resource.Resource;
import cz.cesnet.shongo.fault.SecurityException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.codehaus.jackson.map.ObjectMapper;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
     * Specifies expiration for the {@link #userIdCache}.
     */
    private Duration userIdCacheExpiration;

    /**
     * Specifies expiration for the {@link #userInformationCache}.
     */
    private Duration userInformationCacheExpiration;

    /**
     * Access token which won't be verified and can be used for testing purposes.
     */
    private String testingAccessToken;

    /**
     * Single instance of {@link Authorization} which is used for retrieving {@link UserInformation}.
     */
    private static Authorization authorization;

    /**
     * Constructor.
     *
     * @param configuration to load authorization configuration from
     */
    private Authorization(Configuration configuration)
    {
        authorizationServer = configuration.getString(Configuration.SECURITY_AUTHORIZATION_SERVER);
        if (authorizationServer == null) {
            throw new IllegalStateException("Authorization server is not set in the configuration.");
        }
        userIdCacheExpiration = configuration.getDuration(Configuration.SECURITY_USER_ID_CACHE_EXPIRATION);
        userInformationCacheExpiration =
                configuration.getDuration(Configuration.SECURITY_USER_INFORMATION_CACHE_EXPIRATION);
        testingAccessToken = configuration.getString(Configuration.SECURITY_TESTING_ACCESS_TOKEN);
    }

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
     * Validate that user with given {@code securityToken} can access the {@link cz.cesnet.shongo.controller.api.Controller}.
     *
     * @param securityToken
     */
    public void validate(SecurityToken securityToken)
    {
        // Check not empty
        if (securityToken == null || securityToken.getAccessToken() == null) {
            throw new cz.cesnet.shongo.fault.SecurityException(
                    SecurityToken.class.getSimpleName() + " should not be empty.");
        }
        // Always allow testing access token
        if (testingAccessToken != null && securityToken.getAccessToken().equals(testingAccessToken)) {
            logger.debug("Access token '{}' is valid for testing.", securityToken.getAccessToken());
            return;
        }

        // Validate access token by getting user info
        try {
            UserInformation person = getUserInformation(securityToken);
            logger.debug("Access token '{}' is valid for {} (id: {}).",
                    new Object[]{securityToken.getAccessToken(), person.getFullName(), person.getUserId()});
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
        String userId = getCachedUserIdByAccessToken(accessToken);
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
                HttpClient httpClient = new DefaultHttpClient();
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
            putCachedUserIdByAccessToken(accessToken, userId);
            putCachedUserInformationByUserId(userId, userInformation);

            // Store the user information inside the security token
            securityToken.setCachedPersonInformation(userInformation);

            return userInformation;
        }
    }

    /**
     * @param securityToken of an user
     * @return user-id of an user with given {@code securityToken}
     */
    public String getUserId(SecurityToken securityToken)
    {
        try {
            return getUserInformation(securityToken).getUserId();
        }
        catch (Exception exception) {
            throw new SecurityException("User id cannot be retrieved from the access token '"
                    + securityToken.getAccessToken()
                    + "'. " + exception.getMessage(), exception);
        }
    }

    private static final String USER_WEB_SERVICE = "https://hroch.cesnet.cz/perun-ws/resource/user";

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
        UserInformation userInformation = getCachedUserInformationByUserId(userId);
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
                HttpClient httpClient = new DefaultHttpClient();
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
            putCachedUserInformationByUserId(userId, userInformation);
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
            HttpClient httpClient = new DefaultHttpClient();
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

        if (data.containsKey("eppn")) {
            userInformation.setEduPersonPrincipalName((String) data.get("eppn"));
        }
        if (data.containsKey("organization")) {
            userInformation.setOrganization((String) data.get("organization"));
        }
        if (data.containsKey("email")) {
            userInformation.setEmail((String) data.get("email"));
        }

        return userInformation;
    }

    /**
     * Class for verifying user permissions.
     */
    public static class Permission
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

    /**
     * Entry for {@link Authorization#userIdCache} and {@link Authorization#userInformationCache}.
     */
    private class CacheEntry<T>
    {
        private DateTime expirationDateTime;

        private T cachedObject;

    }

    /**
     * Cache of user-id by access token.
     */
    private Map<String, CacheEntry<String>> userIdCache = new HashMap<String, CacheEntry<String>>();

    /**
     * Cache of {@link UserInformation} by user-id.
     */
    private Map<String, CacheEntry<UserInformation>> userInformationCache =
            new HashMap<String, CacheEntry<UserInformation>>();

    /**
     * @param accessToken
     * @return user-id by given {@code accessToken}
     */
    private String getCachedUserIdByAccessToken(String accessToken)
    {
        CacheEntry<String> entry = userIdCache.get(accessToken);
        if (entry != null) {
            if (entry.expirationDateTime == null || entry.expirationDateTime.isAfter(DateTime.now())) {
                return entry.cachedObject;
            }
            else {
                userIdCache.remove(accessToken);
            }
        }
        return null;
    }

    /**
     * Put given {@code userId} to the cache by the given {@code accessToken}.
     *
     * @param accessToken
     * @param userId
     */
    private void putCachedUserIdByAccessToken(String accessToken, String userId)
    {
        CacheEntry<String> entry = userIdCache.get(accessToken);
        if (entry == null) {
            entry = new CacheEntry<String>();
            userIdCache.put(accessToken, entry);
        }
        if (userIdCacheExpiration != null) {
            entry.expirationDateTime = DateTime.now().plus(userIdCacheExpiration);
        }
        else {
            entry.expirationDateTime = null;
        }
        entry.cachedObject = userId;
    }

    /**
     * @param userId
     * @return {@link UserInformation} by given {@code userId}
     */
    public UserInformation getCachedUserInformationByUserId(String userId)
    {
        CacheEntry<UserInformation> entry = userInformationCache.get(userId);
        if (entry != null) {
            if (entry.expirationDateTime == null || entry.expirationDateTime.isAfter(DateTime.now())) {
                return entry.cachedObject;
            }
            else {
                userIdCache.remove(userId);
            }
        }
        return null;
    }

    /**
     * Put given {@code userInformation} to the cache by the given {@code userId}.
     *
     * @param userId
     * @param userInformation
     */
    public void putCachedUserInformationByUserId(String userId, UserInformation userInformation)
    {
        CacheEntry<UserInformation> entry = userInformationCache.get(userId);
        if (entry == null) {
            entry = new CacheEntry<UserInformation>();
            userInformationCache.put(userId, entry);
        }
        if (userInformationCacheExpiration != null) {
            entry.expirationDateTime = DateTime.now().plus(userInformationCacheExpiration);
        }
        else {
            entry.expirationDateTime = null;
        }
        entry.cachedObject = userInformation;
    }
}
