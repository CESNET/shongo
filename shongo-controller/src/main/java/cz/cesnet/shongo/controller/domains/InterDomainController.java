package cz.cesnet.shongo.controller.domains;

import com.sun.xml.internal.bind.v2.TODO;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.api.Domain;
import cz.cesnet.shongo.controller.api.ResourceSummary;
import cz.cesnet.shongo.controller.api.domains.InterDomainAction;
import cz.cesnet.shongo.controller.api.domains.InterDomainProtocol;
import cz.cesnet.shongo.controller.api.domains.response.DomainLogin;
import cz.cesnet.shongo.controller.api.domains.response.DomainStatus;
import cz.cesnet.shongo.controller.api.request.DomainResourceListRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * Controller for common wizard actions.
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
@Controller
public class InterDomainController implements InterDomainProtocol{

    @Override
    @RequestMapping(value = InterDomainAction.DOMAIN_LOGIN, method = RequestMethod.GET)
    @ResponseBody
    public DomainLogin handleLogin(HttpServletRequest request) {
        throw new TodoImplementException();
//        return new Object(String.valueOf(foreignDomainRunning ? Domain.Status.AVAILABLE : Domain.Status.NOT_AVAILABLE));
    }

    @Override
    @RequestMapping(value = InterDomainAction.DOMAIN_STATUS, method = RequestMethod.GET)
    @ResponseBody
    public DomainStatus handleDomainStatus(HttpServletRequest request) {
        boolean foreignDomainRunning = cz.cesnet.shongo.controller.Controller.isInterDomainAgentRunning();
        return new DomainStatus(String.valueOf(foreignDomainRunning ? Domain.Status.AVAILABLE : Domain.Status.NOT_AVAILABLE));
    }

    @Override
    @RequestMapping(value = InterDomainAction.DOMAIN_RESOURCES_LIST, method = RequestMethod.GET)
    @ResponseBody
    public List<ResourceSummary> handleListResources(HttpServletRequest request) {
        DomainResourceListRequest listRequest= new DomainResourceListRequest(getDomainId(request));
        listRequest.setResourceType(DomainResourceListRequest.ResourceType.RESOURCE);
        return getAgent().getDomainService().listLocalResourcesByDomain(listRequest);
    }

    protected InterDomainAgent getAgent() {
        return InterDomainAgent.getInstance();
    }

    /**
     * Returns {@link Domain} for given {@code request}, if exists. Returns null otherwise.
     * @param request {@link HttpServletRequest}
     * @return {@link Domain} or {@value null}
     */
    protected Domain getDomain(HttpServletRequest request) {
        X509Certificate[] certs = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");
        return getAgent().getDomain(certs[0]);
    }

    protected String getDomainId(HttpServletRequest request) {
        return getDomain(request).getId();
    }
}