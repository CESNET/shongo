package cz.cesnet.shongo.client.web.support.resolvers;

import cz.cesnet.shongo.client.web.support.MessageProvider;
import cz.cesnet.shongo.client.web.support.MessageProviderImpl;
import org.springframework.context.MessageSource;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * {@link HandlerMethodArgumentResolver} for {@link MessageSource}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class MessageProviderHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver
{
    @Resource
    private MessageSource messageSource;

    @Override
    public boolean supportsParameter(MethodParameter methodParameter)
    {
        return methodParameter.getParameterType().equals(MessageProvider.class);
    }

    @Override
    public Object resolveArgument(MethodParameter methodParameter, ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception
    {
        HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();
        return MessageProviderImpl.fromRequest(messageSource, request);
    }
}
