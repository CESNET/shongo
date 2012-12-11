package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.fault.SecurityException;
import cz.cesnet.shongo.fault.TodoImplementException;
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
     * URL to authorization server.
     */
    private String authorizationServer;

    /**
     * Access token which won't be verified and can be used for testing purposes.
     */
    private String testingAccessToken;

    /**
     * Constructor.
     *
     * @param configuration to load authorization configuration from
     */
    public Authorization(Configuration configuration)
    {
        authorizationServer = configuration.getString(Configuration.SECURITY_AUTHORIZATION_SERVER);
        if (authorizationServer == null) {
            throw new IllegalStateException("Authorization server is not set in the configuration.");
        }
        testingAccessToken = configuration.getString(Configuration.SECURITY_TESTING_ACCESS_TOKEN);
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
            Map<String, Object> userInfo = getUserInfo(securityToken);
            logger.debug("Access token '{}' is valid for {} <{}>.",
                    new Object[]{securityToken.getAccessToken(), userInfo.get("name"), userInfo.get("email")});
        }
        catch (Exception exception) {
            throw new SecurityException("Access token '" + securityToken.getAccessToken()
                    + "' cannot be validated. " + exception.getMessage(), exception);
        }
    }

    /**
     * @param securityToken of an user
     * @return user-id of an user with given {@code securityToken}
     */
    public String getUserId(SecurityToken securityToken)
    {
        // Testing security token represents root user
        if (testingAccessToken != null && securityToken.getAccessToken().equals(testingAccessToken)) {
            return ROOT_USER_ID;
        }

        try {
            return (String) getUserInfo(securityToken).get("id");
        }
        catch (Exception exception) {
            throw new SecurityException("User id cannot be retrieved from the access token '"
                    + securityToken.getAccessToken()
                    + "'. " + exception.getMessage(), exception);
        }
    }

    /**
     * @param securityToken of an user
     * @return user info for user with given {@code securityToken}
     */
    public Map<String, Object> getUserInfo(SecurityToken securityToken) throws Exception
    {
        HttpClient httpClient = new DefaultHttpClient();

        // Build url
        URIBuilder uriBuilder = new URIBuilder(authorizationServer + "userinfo");
        uriBuilder.setParameter("schema", "openid");
        String url = uriBuilder.build().toString();

        // Perform request
        HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader("Authorization", "Bearer " + securityToken.getAccessToken());
        HttpResponse response = httpClient.execute(httpGet);
        HttpEntity entity = response.getEntity();
        Map<String, Object> content = null;
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
        return content;
    }
}
