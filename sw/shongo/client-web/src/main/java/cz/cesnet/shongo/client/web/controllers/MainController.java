package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.client.web.Changelog;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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
    @RequestMapping("**/favicon.ico")
    public String getShongoIcon()
    {
        return "forward:/img/shongo.ico";
    }

    @RequestMapping("**/apple-touch-icon*.png")
    public String getShongoPng()
    {
        return "forward:/img/shongo.png";
    }

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

    @RequestMapping(value = "/changelog", method = RequestMethod.GET)
    public String getChangelog(Model model)
    {
        model.addAttribute("changelog", Changelog.getInstance());
        return "changelog";
    }
}
