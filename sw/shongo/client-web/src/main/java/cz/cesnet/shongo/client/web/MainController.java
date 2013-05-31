package cz.cesnet.shongo.client.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Main controller.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Controller
public class MainController
{
    @RequestMapping(value = {"/", "/index"}, method = RequestMethod.GET)
    public String getIndex()
    {
        return "index";
    }
}
