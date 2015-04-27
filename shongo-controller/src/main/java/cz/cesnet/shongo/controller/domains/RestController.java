package cz.cesnet.shongo.controller.domains;

import cz.cesnet.shongo.controller.LocalDomain;
import cz.cesnet.shongo.controller.api.Domain;
import cz.cesnet.shongo.controller.api.rpc.ResourceService;
import org.json.JSONObject;
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

    @RequestMapping(value = InterDomainAction.DOMAIN_STATUS, method = RequestMethod.GET)
    @ResponseBody
    public InterDomainResponse handleDomainStatus() {
        boolean foreignDomainRunning = cz.cesnet.shongo.controller.Controller.isInterDomainAgentRunning();
        InterDomainResponse response = new InterDomainResponse();
        response.setStatus(foreignDomainRunning ? Domain.Status.AVAILABLE : Domain.Status.NOT_AVAILABLE);
        response.setResponseType(LocalDomain.getLocalDomain().getClass().getSimpleName());
        response.setResponse(LocalDomain.getLocalDomain());
        return response;
    }

    /**
     * TODO
     */
    public static class InterDomainResponse {
        private Domain.Status status;

        private String responseType;

        private Object response;

        public Domain.Status getStatus() {
            return status;
        }

        public void setStatus(Domain.Status status) {
            this.status = status;
        }

        public String getResponseType() {
            return responseType;
        }

        public void setResponseType(String responseType) {
            this.responseType = responseType;
        }

        public Object getResponse() {
            return response;
        }

        public void setResponse(Object response) {
            this.response = response;
        }
    }
}