package cz.cesnet.shongo.client.web.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.WebAttributes;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;

/**
 * Error controller.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Controller
public class ErrorController
{
    private static Logger logger = LoggerFactory.getLogger(ErrorController.class);

    @RequestMapping("/error")
    public String getError(HttpServletRequest request, Model model)
    {
        String requestUri = (String) request.getAttribute("javax.servlet.error.request_uri");
        String message = (String) request.getAttribute("javax.servlet.error.message");
        Integer statusCode = (Integer) request.getAttribute("javax.servlet.error.status_code");
        Throwable throwable = (Throwable) request.getAttribute("javax.servlet.error.exception");

        logger.error("Error " + statusCode + ": " + message, throwable);

        if (throwable != null) {
            message = throwable.getMessage();
        }
        else if (message == null) {
            message = HttpStatus.valueOf(statusCode).getReasonPhrase();
        }
        model.addAttribute("url", requestUri);
        model.addAttribute("code", statusCode);
        model.addAttribute("message", message);
        return "error";
    }

    @RequestMapping(value = "/controller-error", method = RequestMethod.GET)
    public String getControllerError()
    {
        return "errorController";
    }

    @RequestMapping("/login-error")
    public String getLoginError(HttpServletRequest request, Model model)
    {
        Exception exception = (Exception) request.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);

        logger.error("Login failed.", exception);

        model.addAttribute("exception", exception);
        return "errorLogin";
    }
}
