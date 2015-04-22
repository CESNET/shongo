package cz.cesnet.shongo.controller.domains;

import cz.cesnet.shongo.controller.api.rpc.ResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller for common wizard actions.
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
@Controller
public class RestController {

    @RequestMapping(value = "/test", method = RequestMethod.GET)
    @ResponseBody
    public List<String> handle() {
        InterDomainAgent idp = InterDomainAgent.getInstance();
        idp.performRequest(InterDomainAgent.HttpMethod.GET, "https://localhost:8433/rest/any" , null);
        //resourceService.listResources(null);
        return new ArrayList<String>();
    }

    @RequestMapping(value = "/any", method = RequestMethod.GET)
    @ResponseBody
    public List<String> handlea() {
        InterDomainAgent idp = InterDomainAgent.getInstance();
        //resourceService.listResources(null);
        return new ArrayList<String>();
    }

    @RequestMapping(value = "/sec", method = RequestMethod.GET)
    @ResponseBody
    public List<String> handle2() {
        return new ArrayList<String>();
    }

}