package cz.cesnet.shongo.client.web.support.resolvers;

import cz.cesnet.shongo.client.web.models.UserSession;
import org.joda.time.DateTimeZone;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import javax.servlet.http.HttpServletRequest;

/**
 * {@link HandlerMethodArgumentResolver} for {@link UserSession}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class UserSessionHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver
{
    @Override
    public boolean supportsParameter(MethodParameter methodParameter)
    {
        return methodParameter.getParameterType().equals(UserSession.class);
    }

    @Override
    public Object resolveArgument(MethodParameter methodParameter, ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception
    {
        HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();
        return UserSession.getInstance(request);
    }
}
