package cz.cesnet.shongo.controller.domains;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.Converter;
import cz.cesnet.shongo.controller.ObjectType;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.api.Domain;
import cz.cesnet.shongo.controller.api.domains.InterDomainAction;
import cz.cesnet.shongo.controller.api.domains.InterDomainProtocol;
import cz.cesnet.shongo.controller.api.domains.response.DomainLogin;
import cz.cesnet.shongo.controller.api.domains.response.DomainCapability;
import cz.cesnet.shongo.controller.api.domains.response.DomainStatus;
import cz.cesnet.shongo.controller.api.domains.response.Reservation;
import cz.cesnet.shongo.controller.api.request.DomainCapabilityListRequest;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.booking.request.ReservationRequest;
import cz.cesnet.shongo.controller.booking.request.ReservationRequestManager;
import cz.cesnet.shongo.controller.booking.reservation.ReservationManager;
import cz.cesnet.shongo.controller.booking.resource.ResourceSpecification;
import cz.cesnet.shongo.ssl.SSLCommunication;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import java.beans.PropertyEditorSupport;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * Controller for common wizard actions.
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
@Controller
public class InterDomainController implements InterDomainProtocol{

    @InitBinder
    public void initBinder(WebDataBinder binder)
    {
        binder.registerCustomEditor(DomainCapabilityListRequest.Type.class, new TypeEditor());
        binder.registerCustomEditor(Interval.class, new IntervalEditor());
    }

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
            @RequestParam(value = "type", required = true) DomainCapabilityListRequest.Type type,
            @RequestParam(value = "interval", required = false) Interval interval,
            @RequestParam(value = "technology", required = false) Technology technology)
    {
        DomainCapabilityListRequest listRequest = new DomainCapabilityListRequest(getDomain(request));
        listRequest.setCapabilityType(type);
        listRequest.setInterval(interval);
        listRequest.setTechnology(technology);
        List<DomainCapability> capabilities = getDomainService().listLocalResourcesByDomain(listRequest);
        return capabilities;
    }

    @Override
    @RequestMapping(value = InterDomainAction.DOMAIN_ALLOCATE, method = RequestMethod.GET)
    @ResponseBody
    public Reservation handleAllocate(HttpServletRequest request,
            @RequestParam(value = "type", required = true) DomainCapabilityListRequest.Type type,
            @RequestParam(value = "slot", required = false) Interval slot,
            @RequestParam(value = "resourceId", required = false) String resourceId,
            @RequestParam(value = "userId", required = true) String userId,
            @RequestParam(value = "technology", required = false) Technology technology)
    {
        //TODO alokovat
        //TODO: vytvorit reservation request s 0:ID domeny?
        EntityManager entityManager = InterDomainAgent.getInstance().getEntityManagerFactory().createEntityManager();
        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        ReservationManager reservationManager = new ReservationManager(entityManager);

        switch (type) {
            case RESOURCE:
                Long domainId = ObjectIdentifier.parseLocalId(getDomain(request).getId(), ObjectType.DOMAIN);

                cz.cesnet.shongo.controller.api.ResourceSpecification resourceSpecification = new cz.cesnet.shongo.controller.api.ResourceSpecification(resourceId);

                ReservationRequest reservationRequest = new ReservationRequest();
                reservationRequest.setSlot(slot);
                reservationRequest.setSpecification(ResourceSpecification.createFromApi(resourceSpecification, entityManager));
                reservationRequest.setPurpose(ReservationRequestPurpose.USER);
                reservationRequest.setDescription("TODO");
                reservationRequest.setCreatedBy(domainId + ":" + userId);
                reservationRequest.setUpdatedBy(domainId + ":" + userId);
                reservationRequestManager.create(reservationRequest);
                break;
            case VIRTUAL_ROOM:
            default:
                throw new TodoImplementException("!!zz!!");
        }

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

    public class IntervalEditor extends PropertyEditorSupport
    {
        public IntervalEditor()
        {
        }

        @Override
        public String getAsText()
        {
            if (getValue() == null) {
                return "";
            }
            Interval value = (Interval) getValue();
            if (value == null) {
                return "";
            }
            return value.toString();
        }

        @Override
        public void setAsText(String text) throws IllegalArgumentException
        {
            if (!StringUtils.hasText(text)) {
                setValue(null);
            }
            else {
                setValue(Converter.convertStringToInterval(text));
            }
        }
    }

    public class TypeEditor extends PropertyEditorSupport
    {
        public TypeEditor()
        {
        }

        @Override
        public String getAsText()
        {
            if (getValue() == null) {
                return "";
            }
            DomainCapabilityListRequest.Type value = (DomainCapabilityListRequest.Type) getValue();
            if (value == null) {
                return "";
            }
            return value.toString();
        }

        @Override
        public void setAsText(String text) throws IllegalArgumentException
        {
            if (!StringUtils.hasText(text)) {
                setValue(null);
            }
            else {
                setValue(DomainCapabilityListRequest.Type.valueOf(text));
            }
        }
    }
}