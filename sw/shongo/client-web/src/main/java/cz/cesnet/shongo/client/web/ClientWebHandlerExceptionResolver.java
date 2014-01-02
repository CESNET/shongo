package cz.cesnet.shongo.client.web;

import cz.cesnet.shongo.client.web.controllers.ErrorController;
import cz.cesnet.shongo.client.web.models.ErrorModel;
import cz.cesnet.shongo.controller.ControllerConnectException;
import cz.cesnet.shongo.controller.api.rpc.CommonService;
import net.tanesha.recaptcha.ReCaptcha;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.net.ConnectException;
import java.util.Map;

/**
 * {@link HandlerExceptionResolver} for client web.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ClientWebHandlerExceptionResolver implements HandlerExceptionResolver
{
    private static Logger logger = LoggerFactory.getLogger(ClientWebHandlerExceptionResolver.class);

    @Resource
    private ClientWebConfiguration configuration;

    @Resource
    private CommonService commonService;

    @Resource
    private ReCaptcha reCaptcha;

    @Override
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler,
            Exception exception)
    {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        if (exception instanceof ControllerConnectException || exception instanceof ConnectException) {
            return new ModelAndView("controllerNotAvailable");
        }
        else if (exception instanceof org.eclipse.jetty.io.EofException) {
            // Just log that exceptions and do not report it
            logger.warn("Not reported exception.", exception);
            return null;
        }
        ErrorModel errorModel = new ErrorModel(request.getRequestURI(), null, null, exception, request);
        ModelAndView modelAndView = ErrorController.handleError(errorModel, configuration, reCaptcha, commonService);
        HttpSession httpSession = request.getSession();
        for (Map.Entry<String, Object> entry : modelAndView.getModel().entrySet()) {
            httpSession.setAttribute(entry.getKey(), entry.getValue());
        }
        return modelAndView;
    }
}
