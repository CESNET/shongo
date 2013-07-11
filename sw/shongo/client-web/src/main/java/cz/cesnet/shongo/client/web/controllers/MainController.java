package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.client.web.Changelog;
import cz.cesnet.shongo.client.web.ClientWebNavigation;
import cz.cesnet.shongo.client.web.ClientWebUrl;
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
    /**
     * Handle shongo ICO image.
     */
    @RequestMapping("**/favicon.ico")
    public String handleShongoIcon()
    {
        return "forward:/img/shongo.ico";
    }

    /**
     * Handle shongo PNG image.
     */
    @RequestMapping("**/apple-touch-icon*.png")
    public String handleShongoPng()
    {
        return "forward:/img/shongo.png";
    }


    /**
     * Handle main (index) view.
     */
    @RequestMapping(value = ClientWebUrl.HOME, method = RequestMethod.GET)
    public String handleIndexView(HttpServletRequest request, RedirectAttributes redirectAttributes)
    {
        // Redirect authentication requests until the "redirect_uri" is fixed
        if (request.getParameter("code") != null || request.getParameter("error") != null) {
            redirectAttributes.addAllAttributes(request.getParameterMap());
            return "redirect:" + ClientWebUrl.LOGIN;
        }
        return "index";
    }

    /**
     * Handle changelog view.
     */
    @RequestMapping(value = ClientWebUrl.CHANGELOG, method = RequestMethod.GET)
    public String handleChangelogView(Model model)
    {
        model.addAttribute("changelog", Changelog.getInstance());
        return "changelog";
    }
}
