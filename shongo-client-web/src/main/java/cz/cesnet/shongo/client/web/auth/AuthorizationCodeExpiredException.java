package cz.cesnet.shongo.client.web.auth;

import org.springframework.security.authentication.AuthenticationServiceException;

/**
 * Authorization code expired.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AuthorizationCodeExpiredException extends AuthenticationServiceException
{
    public AuthorizationCodeExpiredException(String authorizationCode)
    {
        super("Authorization code '" + authorizationCode + "' expired.");
    }
}
