package cz.cesnet.shongo.client.web;

import cz.cesnet.shongo.client.web.controllers.ErrorController;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * {@link HandlerExceptionResolver} for client web.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ClientWebHandlerExceptionResolver implements HandlerExceptionResolver
{
    @Resource
    private ClientWebConfiguration configuration;

    @Override
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler,
            Exception exception)
    {
        return ErrorController.handleError(request.getRequestURI(), null, null, exception, request, configuration);
    }
}
