package cz.cesnet.shongo.controller.domains;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.api.Domain;
import cz.cesnet.shongo.controller.api.domains.InterDomainAction;
import cz.cesnet.shongo.controller.api.domains.InterDomainProtocol;
import cz.cesnet.shongo.controller.api.domains.response.DomainLogin;
import cz.cesnet.shongo.controller.api.domains.response.DomainCapability;
import cz.cesnet.shongo.controller.api.domains.response.DomainStatus;
import cz.cesnet.shongo.controller.api.domains.response.Reservation;
import cz.cesnet.shongo.controller.api.request.DomainCapabilityListRequest;
import cz.cesnet.shongo.controller.booking.reservation.ReservationManager;
import cz.cesnet.shongo.ssl.SSLCommunication;
import org.joda.time.Interval;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

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
        Domain domain = getDomainService().findDomainByName(credentials[0]);
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
    @RequestMapping(value = InterDomainAction.DOMAIN_CAPABILITY_LIST, method = RequestMethod.GET)
    @ResponseBody
    public List<DomainCapability> handleListCapabilities(
            HttpServletRequest request,
            @RequestParam(value = "type", required = true) String type,
            @RequestParam(value = "interval", required = false) Interval interval,
            @RequestParam(value = "technology", required = false) Technology technology)
    {
        DomainCapabilityListRequest listRequest = new DomainCapabilityListRequest(getDomain(request));
        listRequest.setCapabilityType(DomainCapabilityListRequest.Type.valueOf(type));
        listRequest.setInterval(interval);
        listRequest.setTechnology(technology);
        List<DomainCapability> capabilities = getDomainService().listLocalResourcesByDomain(listRequest);
        return capabilities;
    }

    @Override
    @RequestMapping(value = InterDomainAction.DOMAIN_ALLOCATE, method = RequestMethod.GET)
    @ResponseBody
    public Reservation handleAllocate(HttpServletRequest request,
            @RequestParam(value = "type", required = true) String type,
            @RequestParam(value = "slot", required = false) Interval slot,
            @RequestParam(value = "resourceId", required = false) String resourceId,
            @RequestParam(value = "technology", required = false) Technology technology,
            @RequestParam(value = "userId", required = false) String userId)
    {
        //TODO alokovat
        //TODO: vytvorit reservation request s 0:ID domeny?
        return null;
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