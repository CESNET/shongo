package cz.cesnet.shongo.client.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * Main controller.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Controller
public class MainController
{
    @RequestMapping(value = {"/", "/index"}, method = RequestMethod.GET)
    public String getIndex(HttpServletRequest request, RedirectAttributes redirectAttributes)
    {
        // Redirect authentication requests until the "redirect_uri" is fixed
        if (request.getParameter("code") != null || request.getParameter("error") != null) {
            redirectAttributes.addAllAttributes(request.getParameterMap());
            return "redirect:/login";
        }
        return "index";
    }
}
