package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.fault.SecurityException;

import java.util.Map;

/**
 * Provides methods for performing authentication and authorization.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Authorization
{
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
        // Always allow testing security token
        if (securityToken.equals(SecurityToken.TESTING)) {
            return;
        }
        // Always allow testing access token
        if (testingAccessToken != null && securityToken.getAccessToken().equals(testingAccessToken)) {
            return;
        }

        throw new SecurityException("TODO: verify access token " + (securityToken != null ? securityToken.toString() : "null"));
    }

    /**
     * @param securityToken of an user
     * @return user info for user with given {@code securityToken}
     */
    public Map<String, String> getUserInfo(SecurityToken securityToken)
    {
        return null;
    }
}
