package cz.cesnet.shongo.controller.domains;

import com.google.common.base.Strings;
import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.*;
import cz.cesnet.shongo.controller.ControllerReportSet;
import cz.cesnet.shongo.controller.ObjectType;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.RoomSpecification;
import cz.cesnet.shongo.controller.api.domains.InterDomainAction;
import cz.cesnet.shongo.controller.api.domains.InterDomainProtocol;
import cz.cesnet.shongo.controller.api.domains.response.*;
import cz.cesnet.shongo.controller.api.domains.response.Reservation;
import cz.cesnet.shongo.controller.api.request.DomainCapabilityListRequest;
import cz.cesnet.shongo.controller.api.request.ReservationListRequest;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.booking.request.ReservationRequest;
import cz.cesnet.shongo.controller.booking.request.ReservationRequestManager;
import cz.cesnet.shongo.controller.booking.resource.Resource;
import cz.cesnet.shongo.controller.booking.resource.ResourceManager;
import cz.cesnet.shongo.controller.booking.resource.ResourceReservation;
import cz.cesnet.shongo.controller.booking.resource.ResourceSpecification;
import cz.cesnet.shongo.controller.booking.room.*;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Controller for common wizard actions.
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
@Controller
public class InterDomainController implements InterDomainProtocol
{

    @InitBinder
    public void initBinder(WebDataBinder binder)
    {
        binder.registerCustomEditor(DomainCapabilityListRequest.Type.class, new TypeEditor());
        binder.registerCustomEditor(Technology.class, new TechnologyEditor());
        binder.registerCustomEditor(AdobeConnectPermissions.class, new AdobeConnectPermissionsEditor());
        binder.registerCustomEditor(Interval.class, new IntervalEditor());
    }

    @Override
    @RequestMapping(value = InterDomainAction.DOMAIN_LOGIN, method = RequestMethod.GET)
    @ResponseBody
    public DomainLogin handleLogin(HttpServletRequest request)
    {
        String[] credentials = SSLCommunication.getBasicAuthCredentials(request);
        Domain domain = getDomainService().findDomainByName(credentials[0]);
        return new DomainLogin(getAuthentication().generateAccessToken(domain));
    }

    @Override
    @RequestMapping(value = InterDomainAction.DOMAIN_STATUS, method = RequestMethod.GET)
    @ResponseBody
    public DomainStatus handleDomainStatus(HttpServletRequest request)
    {
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
            @RequestParam(value = "licenseCount", required = false) Integer licenseCount,
            @RequestParam(value = "technologies", required = false) List<Technology> technologies) throws NotAuthorizedException
    {
        DomainCapabilityListRequest listRequest = new DomainCapabilityListRequest(getDomain(request));
        listRequest.setCapabilityType(type);
        listRequest.setInterval(interval);
        listRequest.setLicenseCount(licenseCount);
        if (technologies != null && !technologies.isEmpty()) {
            listRequest.setTechnologyVariants(new ArrayList<Set<Technology>>());
            listRequest.getTechnologyVariants().add(new HashSet<>(technologies));
        }
        List<DomainCapability> capabilities = getDomainService().listLocalResourcesByDomain(listRequest);
        if (DomainCapabilityListRequest.Type.VIRTUAL_ROOM.equals(type)) {
            // Erase resource ID's for virtual rooms.
            for (DomainCapability capability : capabilities) {
                capability.setId(null);
            }
        }
        return capabilities;
    }

    @Override
    @RequestMapping(value = InterDomainAction.DOMAIN_ALLOCATE_RESOURCE, method = RequestMethod.GET)
    @ResponseBody
    public Reservation handleAllocateResource(HttpServletRequest request,
                                              @RequestParam(value = "slot", required = true) Interval slot,
                                              @RequestParam(value = "resourceId", required = true) String resourceId,
                                              @RequestParam(value = "userId", required = true) String userId,
                                              @RequestParam(value = "description", required = false) String description,
                                              @RequestParam(value = "reservationRequestId", required = false) String reservationRequestId)
            throws NotAuthorizedException, ForbiddenException
    {
        EntityManager entityManager = InterDomainAgent.getInstance().createEntityManager();
        try {
            ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
            ResourceManager resourceManager = new ResourceManager(entityManager);

            Reservation reservation = new Reservation();

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
            //TODO: when deleting, send anotation to foreign domain
//                    newReservationRequest.setSchedulerDeleteState(AbstractReservationRequest.SchedulerDeleteState.DELETE);
            if (previousReservationRequest == null) {
                reservationRequestManager.create(newReservationRequest);
            } else {
                previousReservationRequest.setUpdatedBy(UserInformation.formatForeignUserId(userId, domainId));
                reservationRequestManager.modify(previousReservationRequest, newReservationRequest);
            }
            //TODO: create ACL for some purpose?
            entityManager.getTransaction().commit();

            reservation.setForeignReservationRequestId(ObjectIdentifier.formatId(newReservationRequest));

            return reservation;
        } finally {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            entityManager.close();
        }
    }

    @Override
    @RequestMapping(value = InterDomainAction.DOMAIN_ALLOCATE_ROOM, method = RequestMethod.GET)
    @ResponseBody
    public Reservation handleAllocateRoom(HttpServletRequest request,
                                          @RequestParam(value = "slot", required = true) Interval slot,
                                          @RequestParam(value = "participantCount", required = true) int participantCount,
                                          @RequestParam(value = "technologies", required = true) List<Technology> technologies,
                                          @RequestParam(value = "userId", required = true) String userId,
                                          @RequestParam(value = "description", required = false) String description,
                                          @RequestParam(value = "roomPin", required = false) String roomPin,
                                          @RequestParam(value = "roomAccessMode", required = false) AdobeConnectPermissions roomAccessMode,
                                          @RequestParam(value = "roomRecorded", required = false) Boolean roomRecorded,
                                          @RequestParam(value = "reservationRequestId", required = false) String reservationRequestId)
            throws NotAuthorizedException, ForbiddenException
    {
        EntityManager entityManager = InterDomainAgent.getInstance().createEntityManager();
        try {
            ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);

            Reservation reservation = new Reservation();

            Long domainId = ObjectIdentifier.parseLocalId(getDomain(request).getId(), ObjectType.DOMAIN);
            ObjectIdentifier reservationRequestIdentifier = null;
            if (!Strings.isNullOrEmpty(reservationRequestId)) {
                reservationRequestIdentifier = ObjectIdentifier.parseTypedId(reservationRequestId, ObjectType.RESERVATION_REQUEST);
            }

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

            RoomSpecification roomSpecification = new RoomSpecification(participantCount, technologies.toArray(new Technology[technologies.size()]));
            RoomAvailability roomAvailability = roomSpecification.getAvailability();
            roomAvailability.setMeetingDescription(description);
            //TODO: recording
            if (Boolean.TRUE.equals(roomRecorded) && !technologies.contains(Technology.ADOBE_CONNECT)) {
                roomAvailability.addServiceSpecification(new RecordingServiceSpecification(true));
            }

            //TODO: participants
//            for (ParticipantModel participant : roomParticipants) {
//                if (participant.getId() == null) {
//                    continue;
//                }
//                roomSpecification.addParticipant(participant.toApi());
//            }
            if (technologies.contains(Technology.H323) && !Strings.isNullOrEmpty(roomPin)) {
                H323RoomSetting h323RoomSetting = new H323RoomSetting();
                h323RoomSetting.setPin(roomPin);
                roomSpecification.addRoomSetting(h323RoomSetting);
            }
            if (technologies.contains(Technology.ADOBE_CONNECT)) {
                AdobeConnectRoomSetting adobeConnectRoomSetting = new AdobeConnectRoomSetting();
                if (!Strings.isNullOrEmpty(roomPin)) {
                    adobeConnectRoomSetting.setPin(roomPin);
                }
                adobeConnectRoomSetting.setAccessMode(roomAccessMode);
                roomSpecification.addRoomSetting(adobeConnectRoomSetting);
            }

            ReservationRequest newReservationRequest = new ReservationRequest();
            newReservationRequest.setSlot(slot);
            newReservationRequest.setSpecification(cz.cesnet.shongo.controller.booking.room.RoomSpecification.createFromApi(roomSpecification, entityManager));
            newReservationRequest.setPurpose(ReservationRequestPurpose.USER);
            newReservationRequest.setDescription(description);
            newReservationRequest.setCreatedBy(UserInformation.formatForeignUserId(userId, domainId));
            newReservationRequest.setUpdatedBy(UserInformation.formatForeignUserId(userId, domainId));
            //TODO: when deleting, send anotation to foreign domain
//                    newReservationRequest.setSchedulerDeleteState(AbstractReservationRequest.SchedulerDeleteState.DELETE);
            if (previousReservationRequest == null) {
                reservationRequestManager.create(newReservationRequest);
            } else {
                previousReservationRequest.setUpdatedBy(UserInformation.formatForeignUserId(userId, domainId));
                reservationRequestManager.modify(previousReservationRequest, newReservationRequest);
            }
            //TODO: create ACL for some purpose?
            entityManager.getTransaction().commit();

            reservation.setForeignReservationRequestId(ObjectIdentifier.formatId(newReservationRequest));

            return reservation;
        } finally {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            entityManager.close();
        }
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
        try {
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
            else if (specification instanceof cz.cesnet.shongo.controller.booking.room.RoomSpecification) {
                // RoomSpecification supported
            } else {
                throw new ForbiddenException("Unsupported specification");
            }

            if (!domainId.equals(UserInformation.parseDomainId(createdByUserId)) || !requestIdentifier.isLocal()) {
                // Throw {@code ForbiddenException} for error 403 to return
                throw new ForbiddenException("Cannot get reservation");
            }

            Reservation reservation = new Reservation();

            //TODO get report: reservationRequest.getReportDescription()
            reservation.setForeignReservationRequestId(reservationRequestId);
            switch (reservationRequest.getAllocationState()) {
                case ALLOCATION_FAILED:
                    reservation.setStatus(AbstractResponse.Status.FAILED);
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

                            reservation.setForeignResourceId(resourceId);
                        }
                    }
                    break;
                //TODO: rozlisovat mistnosti
                default:
                    break;
            }

            return reservation;
        } finally {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            entityManager.close();
        }
    }

    @Override
    @RequestMapping(value = InterDomainAction.DOMAIN_RESERVATION_REQUEST_DELETE, method = RequestMethod.GET)
    @ResponseBody
    public AbstractResponse handleDeleteReservationRequest(HttpServletRequest request,
                                                           @RequestParam(value = "reservationRequestId", required = true) String reservationRequestId)
            throws NotAuthorizedException, ForbiddenException
    {
        Long domainId = ObjectIdentifier.parseLocalId(getDomain(request).getId(), ObjectType.DOMAIN);
        ObjectIdentifier requestIdentifier = ObjectIdentifier.parseTypedId(reservationRequestId, ObjectType.RESERVATION_REQUEST);

        EntityManager entityManager = InterDomainAgent.getInstance().createEntityManager();
        try {
            ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
            ResourceManager resourceManager = new ResourceManager(entityManager);
            ReservationRequest reservationRequest;
            try {
                reservationRequest = (ReservationRequest) reservationRequestManager.get(requestIdentifier.getPersistenceId());
            } catch (CommonReportSet.ObjectNotExistsException ex) {
                //TODO
                // Return success - reservation request is already deleted?
                return new AbstractResponse()
                {
                };
            }

            String createdByUserId = reservationRequest.getCreatedBy();

            Specification specification = reservationRequest.getSpecification();
            if (specification instanceof ResourceSpecification) {
                ResourceSpecification resourceSpecification = (ResourceSpecification) specification;
                Resource resource = resourceSpecification.getResource();
                if (resource != null) {
                    // Throw {@code CommonReportSet.ObjectNotExistsException} if resource is not assigned to this domain for error 403 to return
                    resourceManager.getDomainResource(domainId, resource.getId());
                }
            } else if (specification instanceof cz.cesnet.shongo.controller.booking.room.RoomSpecification) {
                // RoomSpecification supported
            } else {
                throw new ForbiddenException("Unsupported specification");
            }

            if (!domainId.equals(UserInformation.parseDomainId(createdByUserId)) || !requestIdentifier.isLocal()) {
                // Throw {@code ForbiddenException} for error 403 to return
                throw new ForbiddenException("Cannot delete reservation request");
            }

            //Delete local reservation request
            //TODO: nefunguje, nesmaze rezervaci
            try {
                getDomainService().deleteReservationRequest(reservationRequest);
            } catch (ControllerReportSet.ReservationRequestDeletedException ex) {
                // continue - ReservationRequest already deleted
            }

            return new AbstractResponse()
            {
            };
        } finally {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            entityManager.close();
        }
    }

    @Override
    @RequestMapping(value = InterDomainAction.DOMAIN_RESOURCE_RESERVATION_LIST, method = RequestMethod.GET)
    @ResponseBody
    public List<Reservation> handleListReservations(HttpServletRequest request,
                                                    @RequestParam(value = "resourceId", required = false) String resourceId,
                                                    @RequestParam(value = "slot", required = false) Interval slot)
            throws NotAuthorizedException, ForbiddenException
    {
        Domain domain = getDomain(request);
        Long domainId = ObjectIdentifier.parseLocalId(domain.getId(), ObjectType.DOMAIN);
        EntityManager entityManager = InterDomainAgent.getInstance().createEntityManager();
        try {
            ResourceManager resourceManager = new ResourceManager(entityManager);
//        AuthorizationManager authorizationManager = new AuthorizationManager(entityManager, null);
            Set<String> resourceIds = new HashSet<>();

            if (resourceId != null) {
                ObjectIdentifier resourceIdentifier = ObjectIdentifier.parseTypedId(resourceId, ObjectType.RESOURCE);

                // Return 403 if resource Id or reservationRequest Id is not local
                if (!resourceIdentifier.isLocal()) {
                    // Throw {@code ForbiddenException} for error 403 to return
                    throw new ForbiddenException("Cannot allocate");
                }
                // Throw {@code CommonReportSet.ObjectNotExistsException} if resource is not assigned to this domain for error 403 to return
                resourceManager.getDomainResource(domainId, resourceIdentifier.getPersistenceId());

                resourceIds.add(resourceId);
            } else {
                DomainCapabilityListRequest listRequest = new DomainCapabilityListRequest(domain);
                listRequest.setCapabilityType(DomainCapabilityListRequest.Type.RESOURCE);
                listRequest.addResourcesIds(resourceManager.listResourceIdsByDomain(domainId));
                listRequest.setDomain(domain);
                List<DomainCapability> capabilities = getDomainService().listLocalResourcesByDomain(listRequest);
                for (DomainCapability resource : capabilities) {
                    resourceIds.add(resource.getId());
                }
            }

            ReservationListRequest reservationListRequest = new ReservationListRequest();
            reservationListRequest.setResourceIds(resourceIds);
            reservationListRequest.setInterval(slot);
            List<Reservation> reservations = getDomainService().listPublicReservations(reservationListRequest);

            return reservations;
        } finally {
            entityManager.close();
        }
    }

    @ResponseStatus(value = HttpStatus.UNAUTHORIZED)
    @ExceptionHandler({NotAuthorizedException.class})
    public ResponseEntity<String> unauthorizedExceptionHandler(NotAuthorizedException ex)
    {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @ResponseStatus(value = HttpStatus.FORBIDDEN)
    @ExceptionHandler({ForbiddenException.class})
    public ResponseEntity<String> forbiddenExceptionHandler(ForbiddenException ex)
    {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.FORBIDDEN);
    }

    @ResponseStatus(value = HttpStatus.FORBIDDEN)
    @ExceptionHandler({CommonReportSet.ObjectNotExistsException.class})
    public ResponseEntity<String> notFoundExceptionHandler(CommonReportSet.ObjectNotExistsException ex)
    {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.FORBIDDEN);
    }

    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler({RuntimeException.class})
    public ResponseEntity<String> internalExceptionHandler(RuntimeException ex)
    {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Returns domain for given {@code request} with appropriate security header or throws {@link NotAuthorizedException}
     *
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
     *
     * @param request {@link HttpServletRequest}
     * @return {@link Domain} or {@value null}
     */
    protected Domain getDomainByCert(HttpServletRequest request)
    {
        X509Certificate[] certs = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");
        return (certs == null ? null : getAuthentication().getDomain(certs[0]));
    }

    /**
     * Returns {@link Domain} for given {@code request} with basic auth header, if exists. Returns null otherwise.
     *
     * @param request {@link HttpServletRequest}
     * @return {@link Domain} or {@value null}
     */
    protected Domain getDomainByAccessToken(HttpServletRequest request)
    {
        String[] credentials = SSLCommunication.getBasicAuthCredentials(request);
        String accessToken = credentials[0];
        return getAuthentication().getDomain(accessToken);
    }

    protected DomainService getDomainService()
    {
        return InterDomainAgent.getInstance().getDomainService();
    }

    protected DomainAuthentication getAuthentication()
    {
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
            } else {
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
            } else {
                setValue(DomainCapabilityListRequest.Type.valueOf(text));
            }
        }
    }

    public class TechnologyEditor extends PropertyEditorSupport
    {
        public TechnologyEditor()
        {
        }

        @Override
        public String getAsText()
        {
            if (getValue() == null) {
                return "";
            }
            Technology value = (Technology) getValue();
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
            } else {
                setValue(Technology.valueOf(text));
            }
        }
    }

    public class AdobeConnectPermissionsEditor extends PropertyEditorSupport
    {
        public AdobeConnectPermissionsEditor()
        {
        }

        @Override
        public String getAsText()
        {
            if (getValue() == null) {
                return "";
            }
            AdobeConnectPermissions value = (AdobeConnectPermissions) getValue();
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
            } else {
                setValue(AdobeConnectPermissions.valueOf(text));
            }
        }
    }
}