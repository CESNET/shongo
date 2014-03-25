package cz.cesnet.shongo.client.web;

import cz.cesnet.shongo.client.web.controllers.ErrorController;
import cz.cesnet.shongo.client.web.models.ErrorModel;
import cz.cesnet.shongo.controller.ControllerConnectException;
import cz.cesnet.shongo.controller.ControllerReportSet;
import cz.cesnet.shongo.controller.api.rpc.CommonService;
import net.tanesha.recaptcha.ReCaptcha;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        Throwable cause = exception;
        while (cause != null) {
            if (exception instanceof ControllerConnectException) {
                return new ModelAndView("errorControllerNotAvailable");
            }
            else if (exception instanceof ControllerReportSet.SecurityNotAuthorizedException) {
                ControllerReportSet.SecurityNotAuthorizedException securityNotAuthorizedException =
                        (ControllerReportSet.SecurityNotAuthorizedException) exception;
                String action = securityNotAuthorizedException.getReport().getAction();
                Pattern pattern = Pattern.compile("read .+ (shongo(:[-\\.a-zA-Z0-9]+)+:[0-9]+)");
                Matcher matcher = pattern.matcher(action);
                if (matcher.find()) {
                    ModelAndView modelAndView = new ModelAndView("errorObjectInaccessible");
                    modelAndView.addObject("objectId", matcher.group(1));
                    return modelAndView;
                }
            }
            else if (exception instanceof ObjectInaccessibleException) {
                ObjectInaccessibleException objectInaccessibleException = (ObjectInaccessibleException) exception;
                ModelAndView modelAndView = new ModelAndView("errorObjectInaccessible");
                modelAndView.addObject("objectId", objectInaccessibleException.getObjectId());
                return modelAndView;
            }
            else if (exception instanceof org.eclipse.jetty.io.EofException) {
                // Just log that exceptions and do not report it
                logger.warn("Not reported exception.", exception);
                return null;
            }
            cause = cause.getCause();
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
