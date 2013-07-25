package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.client.web.Changelog;
import cz.cesnet.shongo.client.web.ClientWebUrl;
import cz.cesnet.shongo.client.web.interceptors.IgnoreDateTimeZone;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

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
    @IgnoreDateTimeZone
    public String handleShongoIcon()
    {
        return "forward:/img/shongo.ico";
    }

    /**
     * Handle shongo PNG image.
     */
    @RequestMapping("**/apple-touch-icon*.png")
    @IgnoreDateTimeZone
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

    /**
     * Handle report problem.
     */
    @RequestMapping(value = ClientWebUrl.REPORT)
    public String handleReport()
    {
        return "report";
    }

    /**
     * Raise test error.
     */
    @RequestMapping(value = "/test-error")
    public String handleTestError(Model model)
    {
        throw new RuntimeException("Test error");
    }
}
