package cz.cesnet.shongo.client.web.auth;

import org.springframework.web.util.UriComponentsBuilder;

/**
 * Authentication configuration for OpenID Connect.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class OpenIDConnectAuthenticationConfiguration
{
    /**
     * @return url of authentication server
     */
    public String getAuthenticationServerUrl()
    {
        return "https://shongo-auth-dev.cesnet.cz/authn/oic";
    }

    /**
     * @return authentication client id
     */
    public String getAuthenticationClientId()
    {
        return "shongo-client-web";
    }

    /**
     * @return authentication client redirect url
     */
    public String getAuthenticationRedirectUri()
    {
        return "http://127.0.0.1:8182/login";
    }

    /**
     * @return authentication client secret
     */
    public String getAuthenticationSecret()
    {
        return "testclientsecret";
    }

    /**
     * @param state
     * @return url for authorize endpoint
     */
    public String getAuthorizeEndpointUrl(String state)
    {
        UriComponentsBuilder redirectUrlBuilder = UriComponentsBuilder.fromHttpUrl(getAuthenticationServerUrl())
                .pathSegment("authorize")
                .queryParam("client_id", getAuthenticationClientId())
                .queryParam("redirect_uri", getAuthenticationRedirectUri())
                .queryParam("state", state)
                .queryParam("scope", "openid")
                .queryParam("response_type", "code")
                .queryParam("prompt", "login");
        return redirectUrlBuilder.build().toUriString();
    }

    /**
     * @return url for token endpoint
     */
    public String getTokenEndpointUrl()
    {
        UriComponentsBuilder requestUrlBuilder = UriComponentsBuilder.fromHttpUrl(getAuthenticationServerUrl())
                .pathSegment("token");
        return requestUrlBuilder.build().toUriString();
    }

    /**
     * @return url for token endpoint
     */
    public String getUserInfoEndpointUrl()
    {
        UriComponentsBuilder requestUrlBuilder = UriComponentsBuilder.fromHttpUrl(getAuthenticationServerUrl())
                .pathSegment("userinfo")
                .queryParam("schema", "openid");
        return requestUrlBuilder.build().toUriString();
    }
}
