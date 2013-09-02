package cz.cesnet.shongo.client.web.interceptors;

import cz.cesnet.shongo.client.web.auth.OpenIDConnectAuthenticationToken;
import cz.cesnet.shongo.controller.api.SecurityToken;
import org.joda.time.DateTimeZone;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import javax.servlet.http.HttpServletRequest;

/**
 * {@link org.springframework.web.method.support.HandlerMethodArgumentResolver} for {@link DateTimeZone}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class TimeZoneHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver
{
    @Override
    public boolean supportsParameter(MethodParameter methodParameter)
    {
        return methodParameter.getParameterType().equals(DateTimeZone.class);
    }

    @Override
    public Object resolveArgument(MethodParameter methodParameter, ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception
    {
        HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();
        return TimeZoneInterceptor.getDateTimeZone(request.getSession());
    }
}
