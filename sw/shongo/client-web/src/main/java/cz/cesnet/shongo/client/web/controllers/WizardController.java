package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.client.web.ClientWebUrl;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Controller for displaying wizard interface.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Controller
public class WizardController
{
    /**
     * Handle wizard view.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD, method = RequestMethod.GET)
    public String handleWizardView()
    {
        return "wizard";
    }
}
