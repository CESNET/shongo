package cz.cesnet.shongo.client.web.auth;

import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.client.web.ClientWebConfiguration;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.UserSettings;
import cz.cesnet.shongo.controller.api.rpc.AuthorizationService;
import cz.cesnet.shongo.ssl.ConfiguredSSLContext;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.io.InputStream;

/**
 * Authentication provider for OpenID Connect.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class OpenIDConnectAuthenticationProvider implements AuthenticationProvider
{
    private static Logger logger = LoggerFactory.getLogger(OpenIDConnectAuthenticationProvider.class);

    /**
     * @see ClientWebConfiguration
     */
    private ClientWebConfiguration configuration;

    /**
     * Constructor.
     *
     * @param configuration sets the {@link #configuration}
     */
    public OpenIDConnectAuthenticationProvider(ClientWebConfiguration configuration)
    {
        this.configuration = configuration;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException
    {
        if (authentication.isAuthenticated()) {
            return authentication;
        }
        else {
            OpenIDConnectAuthenticationToken authenticationToken = (OpenIDConnectAuthenticationToken) authentication;
            SecurityToken securityToken = authenticationToken.getSecurityToken();
            if (securityToken == null || securityToken.getAccessToken() == null) {
                throw new AuthenticationServiceException("Access token is not set.");
            }
            String accessToken = securityToken.getAccessToken();

            logger.debug("Retrieving user information for access token {}...", accessToken);

            JsonNode userInfoResponse;
            try {
                HttpGet httpGet = new HttpGet(getUserInfoEndpointUrl());
                httpGet.setHeader("Authorization", "Bearer " + accessToken);

                HttpClient httpClient = ConfiguredSSLContext.getInstance().createHttpClient();
                HttpResponse httpResponse = httpClient.execute(httpGet);
                InputStream inputStream = httpResponse.getEntity().getContent();

                try {
                    ObjectMapper jsonMapper = new ObjectMapper();
                    userInfoResponse = jsonMapper.readTree(inputStream);
                }
                finally {
                    inputStream.close();
                }
            }
            catch (IOException exception) {
                throw new AuthenticationServiceException("Unable to obtain user information.", exception);
            }
            // Handle error
            if (userInfoResponse.has("error")) {
                String error = userInfoResponse.get("error").getTextValue();
                String description = userInfoResponse.get("error_description").getTextValue();
                throw new AuthenticationServiceException(
                        "Unable to obtain user information. " + error + ": " + description);
            }
            // Handle success
            if (!userInfoResponse.has("id")) {
                throw new AuthenticationServiceException("Token endpoint did not return an access_token.");
            }

            // Build user info
            UserInformation userInformation = new UserInformation();
            userInformation.setUserId(userInfoResponse.get("id").asText());
            userInformation.setFirstName(userInfoResponse.get("first_name").getTextValue());
            userInformation.setLastName(userInfoResponse.get("last_name").getTextValue());
            if (userInfoResponse.has("original_id")) {
                // TODO: set current principal name
            }
            if (userInfoResponse.has("organization")) {
                userInformation.setOrganization(userInfoResponse.get("organization").getTextValue());
            }
            if (userInfoResponse.has("mail")) {
                userInformation.setEmail(userInfoResponse.get("mail").getTextValue());
            }
            securityToken.setUserInformation(userInformation);

            logger.info("{} authenticated.", userInformation);

            authentication = new OpenIDConnectAuthenticationToken(securityToken, userInformation);
            authentication.setAuthenticated(true);
            return authentication;
        }
    }

    @Override
    public boolean supports(Class<?> authentication)
    {
        return OpenIDConnectAuthenticationToken.class.isAssignableFrom(authentication);
    }

    /**
     * @return url for token endpoint
     */
    public String getUserInfoEndpointUrl()
    {
        UriComponentsBuilder requestUrlBuilder =
                UriComponentsBuilder.fromHttpUrl(configuration.getAuthenticationServerUrl())
                        .pathSegment("userinfo");
        return requestUrlBuilder.build().toUriString();
    }
}
