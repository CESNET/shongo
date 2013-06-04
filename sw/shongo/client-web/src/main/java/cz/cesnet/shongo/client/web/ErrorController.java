package cz.cesnet.shongo.client.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.WebAttributes;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

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
        Integer statusCode = (Integer) request.getAttribute("javax.servlet.error.status_code");
        Throwable throwable = (Throwable) request.getAttribute("javax.servlet.error.exception");
        String message;
        if (throwable != null) {
            message = throwable.getMessage();
        }
        else {
            message = HttpStatus.valueOf(statusCode).getReasonPhrase();
        }
        model.addAttribute("code", statusCode);
        model.addAttribute("message", message);
        return "error";
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
