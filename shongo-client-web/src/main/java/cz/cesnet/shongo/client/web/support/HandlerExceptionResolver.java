package cz.cesnet.shongo.client.web.support;

import cz.cesnet.shongo.client.web.ErrorHandler;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Implementation of {@link org.springframework.web.servlet.HandlerExceptionResolver}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class HandlerExceptionResolver implements org.springframework.web.servlet.HandlerExceptionResolver
{
    @Resource
    private ErrorHandler errorHandler;

    @Override
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler,
            Exception exception)
    {
        return errorHandler.handleError(request, response, request.getRequestURI(),
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR, null, exception);
    }
}
