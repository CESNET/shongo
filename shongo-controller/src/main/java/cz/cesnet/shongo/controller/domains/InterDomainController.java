package cz.cesnet.shongo.controller.domains;

import cz.cesnet.shongo.controller.api.Domain;
import cz.cesnet.shongo.controller.api.ResourceSummary;
import cz.cesnet.shongo.controller.api.domains.InterDomainAction;
import cz.cesnet.shongo.controller.api.domains.InterDomainProtocol;
import cz.cesnet.shongo.controller.api.domains.response.DomainLogin;
import cz.cesnet.shongo.controller.api.domains.response.DomainResource;
import cz.cesnet.shongo.controller.api.domains.response.DomainStatus;
import cz.cesnet.shongo.controller.api.request.DomainResourceListRequest;
import cz.cesnet.shongo.ssl.SSLCommunication;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.naming.AuthenticationException;
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
        String[] credentials = SSLCommunication.getBasicAuthCredentials(request);
        Domain domain = getDomainService().findDomainByCode(credentials[0]);
        return new DomainLogin(getAuthentication().generateAccessToken(domain));
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
    public List<DomainResource> handleListResources(HttpServletRequest request)
    {
        DomainResourceListRequest listRequest = new DomainResourceListRequest(getDomain(request).getId());
        listRequest.setResourceType(DomainResourceListRequest.ResourceType.RESOURCE);
        return getDomainService().listLocalResourcesByDomain(listRequest);
    }

    @RequestMapping(value = InterDomainAction.DOMAIN_CAPABILITY_LIST, method = RequestMethod.GET)
    @ResponseBody
    public List<DomainResource> handleListCapabilities(
            HttpServletRequest request,
            @RequestParam(value = "type", required = true) String type)
    {
        DomainResourceListRequest listRequest = new DomainResourceListRequest(getDomain(request).getId());
        listRequest.setResourceType(DomainResourceListRequest.ResourceType.RESOURCE);
        return getDomainService().listLocalResourcesByDomain(listRequest);
    }

    @ResponseStatus(value = HttpStatus.UNAUTHORIZED)
    @ExceptionHandler({NotAuthorizedException.class})
    public ResponseEntity<String> workflowExceptionCaught(NotAuthorizedException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    /**
     * Returns domain for given {@code request} with appropriate security header or throws {@link NotAuthorizedException}
     * @param request
     * @return {@link Domain}
     */
    protected Domain getDomain(HttpServletRequest request) {
        Domain domain = getDomainByCert(request);
        if (domain == null) {
            domain = getDomainByAccessToken(request);
        }
        if (domain == null) {
            throw new NotAuthorizedException("Invalid authentication. Missing certificate or expired access token.");
        }
        return domain;
    }
    /**
     * Returns {@link Domain} for given {@code request} with certificate, if exists. Returns null otherwise.
     * @param request {@link HttpServletRequest}
     * @return {@link Domain} or {@value null}
     */
    protected Domain getDomainByCert(HttpServletRequest request) {
        X509Certificate[] certs = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");
        return (certs == null ? null : getAuthentication().getDomain(certs[0]));
    }

    /**
     * Returns {@link Domain} for given {@code request} with basic auth header, if exists. Returns null otherwise.
     * @param request {@link HttpServletRequest}
     * @return {@link Domain} or {@value null}
     */
    protected Domain getDomainByAccessToken(HttpServletRequest request) {
        String[] credentials = SSLCommunication.getBasicAuthCredentials(request);
        String accessToken = credentials[0];
        return getAuthentication().getDomain(accessToken);
    }

    protected DomainService getDomainService() {
        return InterDomainAgent.getInstance().getDomainService();
    }

    protected DomainAuthentication getAuthentication() {
        return InterDomainAgent.getInstance().getAuthentication();
    }

    private class NotAuthorizedException extends RuntimeException {
        public NotAuthorizedException(String message) {
            super(message);
        }
    }
}