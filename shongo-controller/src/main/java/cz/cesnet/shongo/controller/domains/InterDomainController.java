package cz.cesnet.shongo.controller.domains;

import com.google.common.base.Strings;
import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.Converter;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.ControllerReportSet;
import cz.cesnet.shongo.controller.ObjectType;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.ReservationRequestReusement;
import cz.cesnet.shongo.controller.api.Domain;
import cz.cesnet.shongo.controller.api.domains.InterDomainAction;
import cz.cesnet.shongo.controller.api.domains.InterDomainProtocol;
import cz.cesnet.shongo.controller.api.domains.response.*;
import cz.cesnet.shongo.controller.api.request.DomainCapabilityListRequest;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.booking.Allocation;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.booking.request.ReservationRequest;
import cz.cesnet.shongo.controller.booking.request.ReservationRequestManager;
import cz.cesnet.shongo.controller.booking.resource.Resource;
import cz.cesnet.shongo.controller.booking.resource.ResourceManager;
import cz.cesnet.shongo.controller.booking.resource.ResourceReservation;
import cz.cesnet.shongo.controller.booking.resource.ResourceSpecification;
import cz.cesnet.shongo.controller.booking.specification.Specification;

import cz.cesnet.shongo.ssl.SSLCommunication;
import org.joda.time.Interval;
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
            @RequestParam(value = "technology", required = false) Technology technology) throws NotAuthorizedException
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
            @RequestParam(value = "technology", required = false) Technology technology,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "reservationRequestId", required = false) String reservationRequestId)
            throws NotAuthorizedException, ForbiddenException
    {
        EntityManager entityManager = InterDomainAgent.getInstance().createEntityManager();
        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        ResourceManager resourceManager = new ResourceManager(entityManager);

        Reservation reservation = new Reservation();
        switch (type) {
            case RESOURCE:
                Long domainId = ObjectIdentifier.parseLocalId(getDomain(request).getId(), ObjectType.DOMAIN);
                ObjectIdentifier resourceIdentifier = ObjectIdentifier.parseTypedId(resourceId, ObjectType.RESOURCE);
                ObjectIdentifier reservationRequestIdentifier = null;
                if (!Strings.isNullOrEmpty(reservationRequestId)) {
                    reservationRequestIdentifier = ObjectIdentifier.parseTypedId(reservationRequestId, ObjectType.RESERVATION_REQUEST);
                }

                // Return 403 if resource Id or reservationRequest Id is not local
                if (!resourceIdentifier.isLocal()) {
                    // Throw {@code ForbiddenException} for error 403 to return
                    throw new ForbiddenException("Cannot allocate");
                }
                // Throw {@code CommonReportSet.ObjectNotExistsException} if resource is not assigned to this domain for error 403 to return
                resourceManager.getDomainResource(domainId, resourceIdentifier.getPersistenceId());

                entityManager.getTransaction().begin();

                AbstractReservationRequest previousReservationRequest = null;
                if (reservationRequestIdentifier != null) {
                    previousReservationRequest = reservationRequestManager.get(reservationRequestIdentifier.getPersistenceId());
                    String createdByUserId = previousReservationRequest.getCreatedBy();

                    if (!domainId.equals(UserInformation.parseDomainId(createdByUserId)) || !reservationRequestIdentifier.isLocal()) {
                        // Throw {@code ForbiddenException} for error 403 to return
                        throw new ForbiddenException("Cannot get reservation");
                    }

                    switch (previousReservationRequest.getState()) {
                        case MODIFIED:
                            //TODO: jak reportovat?
                            throw new ControllerReportSet.ReservationRequestAlreadyModifiedException(reservationRequestIdentifier.formatGlobalId());
                        case DELETED:
                            //TODO: vyhazovat 404?
                            throw new ControllerReportSet.ReservationRequestDeletedException(reservationRequestIdentifier.formatGlobalId());
                    }
                }

                cz.cesnet.shongo.controller.api.ResourceSpecification resourceSpecification = new cz.cesnet.shongo.controller.api.ResourceSpecification(resourceId);

                ReservationRequest newReservationRequest = new ReservationRequest();
                newReservationRequest.setSlot(slot);
                newReservationRequest.setSpecification(ResourceSpecification.createFromApi(resourceSpecification, entityManager));
                newReservationRequest.setPurpose(ReservationRequestPurpose.USER);
                newReservationRequest.setDescription(description);
                newReservationRequest.setCreatedBy(UserInformation.formatForeignUserId(userId, domainId));
                newReservationRequest.setUpdatedBy(UserInformation.formatForeignUserId(userId, domainId));
                if (previousReservationRequest == null) {
                    reservationRequestManager.create(newReservationRequest);
                }
                else {
                    previousReservationRequest.setUpdatedBy(UserInformation.formatForeignUserId(userId, domainId));
                    reservationRequestManager.modify(previousReservationRequest, newReservationRequest);
                }
                //TODO: create ACL for some purpose?
                entityManager.getTransaction().commit();

                reservation.setReservationRequestId(ObjectIdentifier.formatId(newReservationRequest));
                break;
            case VIRTUAL_ROOM:
            default:
                throw new TodoImplementException("Allocating another type of reservation");
        }

        return reservation;
    }

    @Override
    @RequestMapping(value = InterDomainAction.DOMAIN_RESERVATION_DATA, method = RequestMethod.GET)
    @ResponseBody
    public Reservation handleGetReservation(HttpServletRequest request,
                                      @RequestParam(value = "reservationRequestId", required = true) String reservationRequestId) throws NotAuthorizedException, ForbiddenException
    {
        Domain domain = getDomain(request);
        Long domainId = ObjectIdentifier.parseLocalId(domain.getId(), ObjectType.DOMAIN);
        ObjectIdentifier requestIdentifier = ObjectIdentifier.parseTypedId(reservationRequestId, ObjectType.RESERVATION_REQUEST);

        EntityManager entityManager = InterDomainAgent.getInstance().createEntityManager();
        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        ResourceManager resourceManager = new ResourceManager(entityManager);
        ReservationRequest reservationRequest = (ReservationRequest) reservationRequestManager.get(requestIdentifier.getPersistenceId());
        cz.cesnet.shongo.controller.booking.reservation.Reservation currentReservation = reservationRequest.getAllocation().getCurrentReservation();

        String createdByUserId = reservationRequest.getCreatedBy();

        Specification specification = reservationRequest.getSpecification();
        if (specification instanceof ResourceSpecification) {
            ResourceSpecification resourceSpecification = (ResourceSpecification) specification;
            Resource resource = resourceSpecification.getResource();
            if (resource != null) {
                // Throw {@code CommonReportSet.ObjectNotExistsException} if resource is not assigned to this domain for error 403 to return
                resourceManager.getDomainResource(domainId, resource.getId());
            }
        }

        if (!domainId.equals(UserInformation.parseDomainId(createdByUserId)) || !requestIdentifier.isLocal()) {
            // Throw {@code ForbiddenException} for error 403 to return
            throw new ForbiddenException("Cannot get reservation");
        }

        Reservation reservation = new Reservation();

        //TODO get report: reservationRequest.getReportDescription()
        switch (reservationRequest.getAllocationState()) {
            case ALLOCATION_FAILED:
                reservation.setReservationRequestId(reservationRequestId);
                reservation.setStatus(AbstractResponse.Status.ERROR);
//                SchedulerReport report = reservationRequest.getReports().get(reservationRequest.getReports().size() - 1);
                reservation.setMessage("TODO ERROR");
                break;
            case ALLOCATED:
                if (currentReservation != null) {
                    String reservationId = ObjectIdentifier.formatId(currentReservation);
                    reservation.setSlot(currentReservation.getSlot());
                    reservation.setForeignReservationId(reservationId);
                    if (currentReservation instanceof ResourceReservation) {
                        ResourceReservation resourceReservation = (ResourceReservation) currentReservation;
                        String resourceId = ObjectIdentifier.formatId(resourceReservation.getResource());

                        reservation.setResourceId(resourceId);
                    }
                }
                else {
                    reservation.setReservationRequestId(reservationRequestId);
                }
                break;
            default:
                reservation.setReservationRequestId(reservationRequestId);
                break;
        }


        return reservation;
    }

    @ResponseStatus(value = HttpStatus.UNAUTHORIZED)
    @ExceptionHandler({NotAuthorizedException.class})
    public ResponseEntity<String> unauthorizedExceptionHandler(NotAuthorizedException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @ResponseStatus(value = HttpStatus.FORBIDDEN)
    @ExceptionHandler({ForbiddenException.class})
    public ResponseEntity<String> forbiddenExceptionHandler(ForbiddenException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.FORBIDDEN);
    }

    @ResponseStatus(value = HttpStatus.FORBIDDEN)
    @ExceptionHandler({CommonReportSet.ObjectNotExistsException.class})
    public ResponseEntity<String> notFoundExceptionHandler(CommonReportSet.ObjectNotExistsException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.FORBIDDEN);
    }

    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler({RuntimeException.class})
    public ResponseEntity<String> internalExceptionHandler(RuntimeException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Returns domain for given {@code request} with appropriate security header or throws {@link NotAuthorizedException}
     * @param request
     * @return {@link Domain}
     */
    protected Domain getDomain(HttpServletRequest request) throws NotAuthorizedException
    {
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