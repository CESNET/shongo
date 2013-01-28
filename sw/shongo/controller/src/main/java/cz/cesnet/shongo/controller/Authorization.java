package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.common.Person;
import cz.cesnet.shongo.controller.common.UserPerson;
import cz.cesnet.shongo.fault.SecurityException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

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
    public static final UserInformation ROOT_USER_PERSON = new UserInformation(null)
    {
        @Override
        public String getUserId()
        {
            return ROOT_USER_ID;
        }

        @Override
        public String getFullName()
        {
            return "root";
        }
    };

    /**
     * URL to authorization server.
     */
    private String authorizationServer;

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
    private static Authorization getInstance() throws IllegalStateException
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
            throw new cz.cesnet.shongo.fault.SecurityException(SecurityToken.class.getSimpleName()
                    + " should not be empty.");
        }
        // Always allow testing access token
        if (testingAccessToken != null && securityToken.getAccessToken().equals(testingAccessToken)) {
            logger.debug("Access token '{}' is valid for testing.", securityToken.getAccessToken());
            return;
        }

        // Validate access token by getting user info
        try {
            UserInformation person = getUserInformation(securityToken);
            logger.debug("Access token '{}' is valid for {} <{}>.",
                    new Object[]{securityToken.getAccessToken(), person.getFullName(), person.getPrimaryEmail()});
        }
        catch (Exception exception) {
            throw new SecurityException("Access token '" + securityToken.getAccessToken()
                    + "' cannot be validated. " + exception.getMessage(), exception);
        }
    }

    /**
     * @param securityToken of an user
     * @return {@link cz.cesnet.shongo.controller.common.Person} for user with given {@code securityToken}
     */
    public UserInformation getUserInformation(SecurityToken securityToken) throws IllegalStateException
    {
        // Testing security token represents root user
        if (testingAccessToken != null && securityToken.getAccessToken().equals(testingAccessToken)) {
            return ROOT_USER_PERSON;
        }

        logger.debug("Retrieving user information by access token '{}'...", securityToken.getAccessToken());

        Map<String, Object> content = null;
        try {
            // Build url
            URIBuilder uriBuilder = new URIBuilder(authorizationServer + "userinfo");
            uriBuilder.setParameter("schema", "openid");
            String url = uriBuilder.build().toString();

            // Perform request
            HttpGet httpGet = new HttpGet(url);
            httpGet.setHeader("Authorization", "Bearer " + securityToken.getAccessToken());
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
        return new UserInformation(content);
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

    /**
     * @param userId
     * @return {@link cz.cesnet.shongo.controller.common.Person} for user with given {@code userId}
     * @throws Exception
     */
    public UserInformation getUserInformation(String userId) throws IllegalStateException
    {
        // Root user
        if (userId.equals(ROOT_USER_ID)) {
            return ROOT_USER_PERSON;
        }

        logger.debug("Retrieving user information by user-id '{}'...", userId);

        Map<String, Object> content = null;
        try {
            // Build url
            URIBuilder uriBuilder = new URIBuilder("https://hroch.cesnet.cz/perun-ws/resource/user/" + userId);
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
                throw new Exception("Error while retrieving user info by id.");
            }
        }
        catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
        return new UserInformation(content);
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
     * Represents an information about user.
     */
    public static class UserInformation implements Person.Information
    {
        /**
         * Data returned from the authorization server.
         */
        private Map<String, String> data = new HashMap<String, String>();

        /**
         * Constructor.
         *
         * @param data sets the {@link #data}
         */
        public UserInformation(Map<String, Object> data)
        {
            // Add all not null entries
            if (data != null) {
                for (Map.Entry<String, Object> entry : data.entrySet()) {
                    if (entry.getValue() != null) {
                        this.data.put(entry.getKey(), (String) entry.getValue());
                    }
                }
            }
        }

        /**
         * @return user-id
         */
        public String getUserId()
        {
            if (!data.containsKey("id")) {
                throw new IllegalStateException("User information must contains identifier.");
            }
            return data.get("id");
        }

        @Override
        public String getFullName()
        {
            StringBuilder name = new StringBuilder();
            name.append(data.get("given_name"));
            name.append(" ");
            name.append(data.get("family_name"));
            return name.toString();
        }

        @Override
        public String getRootOrganization()
        {
            return data.get("organization");
        }

        @Override
        public String getPrimaryEmail()
        {
            return data.get("email");
        }

        public static UserInformation getInstance(SecurityToken securityToken)
        {
            return Authorization.getInstance().getUserInformation(securityToken);
        }

        public static UserInformation getInstance(String userId)
        {
            return Authorization.getInstance().getUserInformation(userId);
        }
    }
}
