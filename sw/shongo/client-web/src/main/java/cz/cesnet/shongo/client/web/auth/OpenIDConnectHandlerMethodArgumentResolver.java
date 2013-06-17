package cz.cesnet.shongo.client.web.auth;

import cz.cesnet.shongo.client.web.annotations.AccessToken;
import cz.cesnet.shongo.controller.api.SecurityToken;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * {@link HandlerMethodArgumentResolver} for OpenID Connect access token.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class OpenIDConnectHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver
{
    @Override
    public boolean supportsParameter(MethodParameter methodParameter)
    {
        return methodParameter.getParameterType().equals(SecurityToken.class);
    }

    @Override
    public Object resolveArgument(MethodParameter methodParameter, ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception
    {
        OpenIDConnectAuthenticationToken authenticationToken =
                (OpenIDConnectAuthenticationToken) webRequest.getUserPrincipal();
        if (authenticationToken != null) {
            return authenticationToken.getSecurityToken();
        }
        else {
            return null;
        }
    }
}
