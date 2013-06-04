package cz.cesnet.shongo.client.web;

import org.apache.commons.configuration.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

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

    @RequestMapping(value = "/changelog", method = RequestMethod.GET)
    public String getChangelog(Model model)
    {
        model.addAttribute("changelog", Changelog.getInstance());
        return "changelog";
    }
}
