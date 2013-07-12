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
 * Controller for other views and resources.
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
     * Handle changelog view.
     */
    @RequestMapping(value = ClientWebUrl.CHANGELOG, method = RequestMethod.GET)
    public String handleChangelogView(Model model)
    {
        model.addAttribute("changelog", Changelog.getInstance());
        return "changelog";
    }
}
