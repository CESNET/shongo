package cz.cesnet.shongo.client.web;

import cz.cesnet.shongo.client.web.controllers.ErrorController;
import cz.cesnet.shongo.client.web.models.ErrorModel;
import cz.cesnet.shongo.controller.ControllerConnectException;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Map;

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
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        if (exception instanceof ControllerConnectException) {
            return new ModelAndView("controllerNotAvailable");
        }
        ErrorModel errorModel = new ErrorModel(request.getRequestURI(), null, null, exception, request);
        ModelAndView modelAndView = ErrorController.handleError(errorModel, configuration);
        HttpSession httpSession = request.getSession();
        for (Map.Entry<String, Object> entry : modelAndView.getModel().entrySet()) {
            httpSession.setAttribute(entry.getKey(), entry.getValue());
        }
        return modelAndView;
    }
}
