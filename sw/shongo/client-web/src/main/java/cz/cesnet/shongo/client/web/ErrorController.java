package cz.cesnet.shongo.client.web;

import com.google.common.base.Throwables;
import org.springframework.http.HttpStatus;
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
    @RequestMapping("error")
    public String getError(HttpServletRequest request, Model model)
    {
        Integer statusCode = (Integer) request.getAttribute("javax.servlet.error.status_code");
        Throwable throwable = (Throwable) request.getAttribute("javax.servlet.error.exception");
        String message;
        if (throwable != null) {
            message = Throwables.getRootCause(throwable).getMessage();
        }
        else {
            message = HttpStatus.valueOf(statusCode).getReasonPhrase();
        }
        model.addAttribute("code", statusCode);
        model.addAttribute("message", message);
        return "error";
    }
}
