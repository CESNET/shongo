package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.client.web.ClientWebUrl;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Controller for displaying dashboard.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Controller
public class DashboardController
{
    /**
     * Handle dashboard view.
     */
    @RequestMapping(value = ClientWebUrl.DASHBOARD, method = RequestMethod.GET)
    public String handleDashboardView()
    {
        return "dashboard";
    }
}
