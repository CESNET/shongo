package cz.cesnet.shongo.client.web.auth;

import com.google.common.base.Strings;
import cz.cesnet.shongo.client.web.ClientWebConfiguration;
import cz.cesnet.shongo.client.web.ClientWebUrl;
import cz.cesnet.shongo.ssl.ConfiguredSSLContext;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.LinkedList;
import java.util.List;

/**
 * Authentication filter for OpenID Connect.
 * <p/>
 * Based on https://github.com/mitreid-connect/OpenID-Connect-Java-Spring-Server/tree/master/openid-connect-client.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class OpenIDConnectAuthenticationFilter extends AbstractAuthenticationProcessingFilter
{
    private static Logger logger = LoggerFactory.getLogger(OpenIDConnectAuthenticationFilter.class);

    private final static String SESSION_STATE_VARIABLE = "state";

    /**
     * @see ClientWebConfiguration
     */
    private ClientWebConfiguration configuration;

    /**
     * Constructor.
     *
     * @param configuration sets the {@link #configuration}
     */
    protected OpenIDConnectAuthenticationFilter(ClientWebConfiguration configuration)
    {
        super(ClientWebUrl.LOGIN);
        this.configuration = configuration;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException, IOException, ServletException
    {
        if (!Strings.isNullOrEmpty(request.getParameter("error"))) {
            handleError(request, response);
            return null;
        }
        else if (!Strings.isNullOrEmpty(request.getParameter("code"))) {
            Authentication authentication = handleAuthorizationCodeResponse(request, response);
            return authentication;
        }
        else {
            handleAuthorizationRequest(request, response);
            return null;
        }
    }

    /**
     * Initiate an authorization request.
     *
     * @param request
     * @param response
     * @throws IOException
     */
    protected void handleAuthorizationRequest(HttpServletRequest request, HttpServletResponse response)
            throws IOException
    {
        HttpSession session = request.getSession();
        String state = createState(session);

        logger.debug("Redirecting to authentication server...");

        response.sendRedirect(getAuthorizeEndpointUrl(state));
    }

    /**
     * @param request The request from which to extract parameters and perform the
     *                authentication
     * @return The authenticated user token, or null if authentication is
     *         incomplete.
     */
    protected Authentication handleAuthorizationCodeResponse(HttpServletRequest request, HttpServletResponse response)
    {
        HttpSession session = request.getSession();
        String authorizationCode = request.getParameter("code");

        String storedState = getStoredState(session);
        if (storedState != null) {
            String state = request.getParameter("state");
            if (!storedState.equals(state)) {
                throw new AuthenticationServiceException(
                        "State parameter mismatch. Expected " + storedState + " but got " + state + ".");
            }
        }

        logger.debug("Retrieving access token for for authorization code {}...", authorizationCode);

        // Get access token from authorization code
        JsonNode tokenResponse;
        try {
            List<NameValuePair> content = new LinkedList<NameValuePair>();
            content.add(new BasicNameValuePair("client_id", configuration.getAuthenticationClientId()));
            content.add(new BasicNameValuePair("redirect_uri", configuration.getAuthenticationRedirectUri()));
            content.add(new BasicNameValuePair("grant_type", "authorization_code"));
            content.add(new BasicNameValuePair("code", authorizationCode));

            HttpPost httpPost = new HttpPost(getTokenEndpointUrl());
            httpPost.setHeader("Authorization", "secret auth=" + configuration.getAuthenticationSecret());
            httpPost.setEntity(new UrlEncodedFormEntity(content));

            HttpClient httpClient = ConfiguredSSLContext.getInstance().createHttpClient();
            HttpResponse httpResponse = httpClient.execute(httpPost);
            InputStream inputStream = httpResponse.getEntity().getContent();

            try {
                ObjectMapper jsonMapper = new ObjectMapper();
                tokenResponse = jsonMapper.readTree(inputStream);
            }
            finally {
                inputStream.close();
            }
        }
        catch (IOException exception) {
            throw new AuthenticationServiceException("Unable to obtain access token.", exception);
        }
        // Handle error
        if (tokenResponse.has("error")) {
            String error = tokenResponse.get("error").getTextValue();
            String description = tokenResponse.get("error_description").getTextValue();
            throw new AuthenticationServiceException("Unable to obtain access token. " + error + ": " + description);
        }
        // Handle success
        if (!tokenResponse.has("access_token")) {
            throw new AuthenticationServiceException("Token endpoint did not return an access_token.");
        }
        String accessToken = tokenResponse.get("access_token").getTextValue();

        logger.debug("Authenticating access token {}...", accessToken);

        OpenIDConnectAuthenticationToken authenticationToken = new OpenIDConnectAuthenticationToken(accessToken);
        AuthenticationManager authenticationManager = this.getAuthenticationManager();
        return authenticationManager.authenticate(authenticationToken);
    }

    /**
     * Handle authorization error.
     *
     * @param request
     * @param response
     */
    protected void handleError(HttpServletRequest request, HttpServletResponse response)
    {
        String error = request.getParameter("error");
        String errorDescription = request.getParameter("error_description");
        throw new AuthenticationServiceException(error + ": " + errorDescription);
    }

    /**
     * @param state
     * @return url for authorize endpoint
     */
    private String getAuthorizeEndpointUrl(String state)
    {
        UriComponentsBuilder redirectUrlBuilder =
                UriComponentsBuilder.fromHttpUrl(configuration.getAuthenticationServerUrl())
                        .pathSegment("authorize")
                        .queryParam("client_id", configuration.getAuthenticationClientId())
                        .queryParam("redirect_uri", configuration.getAuthenticationRedirectUri())
                        .queryParam("state", state)
                        .queryParam("scope", "openid")
                        .queryParam("response_type", "code")
                        .queryParam("prompt", "login");
        return redirectUrlBuilder.build().toUriString();
    }

    /**
     * @return url for token endpoint
     */
    private String getTokenEndpointUrl()
    {
        UriComponentsBuilder requestUrlBuilder =
                UriComponentsBuilder.fromHttpUrl(configuration.getAuthenticationServerUrl())
                        .pathSegment("token");
        return requestUrlBuilder.build().toUriString();
    }

    /**
     * Create a cryptographically random state and store it in the session.
     *
     * @param session to which the state should be stored
     * @return created state
     */
    private static String createState(HttpSession session)
    {
        String state = new BigInteger(50, new SecureRandom()).toString(16);
        session.setAttribute(SESSION_STATE_VARIABLE, state);
        return state;
    }

    /**
     * @param session from which the state should be read
     * @return the state we stored in the session
     */
    protected static String getStoredState(HttpSession session)
    {
        Object value = session.getAttribute(SESSION_STATE_VARIABLE);
        if (value != null && value instanceof String) {
            return (String) value;
        }
        else {
            return null;
        }
    }
}
