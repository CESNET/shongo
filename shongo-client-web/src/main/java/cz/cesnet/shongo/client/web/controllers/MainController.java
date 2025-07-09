package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.client.web.Changelog;
import cz.cesnet.shongo.client.web.ClientWebUrl;
import cz.cesnet.shongo.client.web.Design;
import cz.cesnet.shongo.client.web.support.interceptors.IgnoreDateTimeZone;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.annotation.Resource;

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
    @RequestMapping("/favicon.ico")
    @IgnoreDateTimeZone
    public String handleIcon()
    {

        return "forward:/design/img/icon.ico";
    }

    /**
     * Handle shongo PNG image.
     */
    @RequestMapping("/apple-touch-icon*.png")
    @IgnoreDateTimeZone
    public String handleAppleTouchIcon()
    {
        return "forward:/design/img/apple-touch-icon.png";
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
