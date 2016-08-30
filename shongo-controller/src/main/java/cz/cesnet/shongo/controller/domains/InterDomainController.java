package cz.cesnet.shongo.controller.domains;

import com.google.common.base.Strings;
import cz.cesnet.shongo.*;
import cz.cesnet.shongo.api.*;
import cz.cesnet.shongo.api.Room;
import cz.cesnet.shongo.connector.api.jade.ConnectorCommand;
import cz.cesnet.shongo.connector.api.jade.multipoint.*;
import cz.cesnet.shongo.connector.api.jade.multipoint.DisconnectRoomParticipant;
import cz.cesnet.shongo.connector.api.jade.multipoint.GetRoom;
import cz.cesnet.shongo.connector.api.jade.multipoint.ListRoomParticipants;
import cz.cesnet.shongo.connector.api.jade.multipoint.ModifyRoomParticipant;
import cz.cesnet.shongo.controller.*;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.Executable;
import cz.cesnet.shongo.controller.api.RecordingServiceSpecification;
import cz.cesnet.shongo.controller.api.RoomSpecification;
import cz.cesnet.shongo.controller.api.domains.InterDomainAction;
import cz.cesnet.shongo.controller.api.domains.InterDomainProtocol;
import cz.cesnet.shongo.controller.api.domains.request.*;
import cz.cesnet.shongo.controller.api.domains.response.RoomParticipant;
import cz.cesnet.shongo.controller.api.domains.request.ForeignRoomParticipantRole;
import cz.cesnet.shongo.controller.api.domains.response.*;
import cz.cesnet.shongo.controller.api.domains.response.Reservation;
import cz.cesnet.shongo.controller.api.request.ReservationListRequest;
import cz.cesnet.shongo.controller.booking.Allocation;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.alias.Alias;
import cz.cesnet.shongo.controller.booking.executable.*;
import cz.cesnet.shongo.controller.booking.participant.PersonParticipant;
import cz.cesnet.shongo.controller.booking.recording.*;
import cz.cesnet.shongo.controller.booking.recording.RecordingService;
import cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.booking.request.ReservationRequest;
import cz.cesnet.shongo.controller.booking.request.ReservationRequestManager;
import cz.cesnet.shongo.controller.booking.resource.*;
import cz.cesnet.shongo.controller.booking.resource.Resource;
import cz.cesnet.shongo.controller.booking.resource.ResourceReservation;
import cz.cesnet.shongo.controller.booking.room.RoomEndpoint;
import cz.cesnet.shongo.controller.booking.room.RoomReservation;
import cz.cesnet.shongo.controller.booking.specification.Specification;
import cz.cesnet.shongo.controller.api.domains.response.RoomSpecification.RoomState;

import cz.cesnet.shongo.controller.scheduler.SchedulerReport;
import cz.cesnet.shongo.jade.SendLocalCommand;
import cz.cesnet.shongo.report.Report;
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
import java.util.*;

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
        binder.registerCustomEditor(Interval.class, new IntervalEditor());

        binder.registerCustomEditor(DomainCapability.Type.class, new EnumEditor<>(DomainCapability.Type.class));
        binder.registerCustomEditor(Technology.class, new EnumEditor<>(Technology.class));
        binder.registerCustomEditor(AdobeConnectPermissions.class, new EnumEditor<>(AdobeConnectPermissions.class));
        binder.registerCustomEditor(AliasType.class, new EnumEditor<>(AliasType.class));
        binder.registerCustomEditor(ExecutableState.class, new EnumEditor<>(ExecutableState.class));
        binder.registerCustomEditor(RoomParticipantValue.Type.class, new EnumEditor<>(RoomParticipantValue.Type.class));
        binder.registerCustomEditor(ParticipantRole.class, new EnumEditor<>(ParticipantRole.class));
        binder.registerCustomEditor(RoomLayout.class, new EnumEditor<>(RoomLayout.class));
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
    @RequestMapping(value = InterDomainAction.DOMAIN_CAPABILITY_LIST, method = RequestMethod.POST)
    @ResponseBody
    public List<DomainCapability> handleListCapabilities(
            HttpServletRequest request,
            @RequestParam(value = "slot", required = false) Interval slot,
            @RequestBody List<CapabilitySpecificationRequest> capabilitySpecificationRequests
    ) throws NotAuthorizedException
    {
        List<DomainCapability> capabilities = new ArrayList<>();
        for (CapabilitySpecificationRequest capabilitySpecificationRequest : capabilitySpecificationRequests) {
            Long domainId = ObjectIdentifier.parseLocalId(getDomain(request).getId(), ObjectType.DOMAIN);
            DomainCapability.Type type = capabilitySpecificationRequest.getCapabilityType();
            Integer licenses = capabilitySpecificationRequest.getLicenseCount();
            List<Set<Technology>> technologyVariants = capabilitySpecificationRequest.getTechnologyVariants();

            List<DomainCapability> resultList = getDomainService().listLocalResourcesByDomain(domainId, type, licenses, technologyVariants);
            if (!DomainCapability.Type.RESOURCE.equals(capabilitySpecificationRequest.getCapabilityType())) {
                // Erase resource ID's for every capability type except {@link Type.RESOURCE}.
                for (DomainCapability capability : resultList) {
                    capability.setId(null);
                }
            }
            if (!resultList.isEmpty()) {
                capabilities.addAll(resultList);
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
            entityManager.getTransaction().begin();

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
                        throw new ControllerReportSet.ReservationRequestAlreadyModifiedException(reservationRequestIdentifier.formatGlobalId());
                    case DELETED:
                        throw new ControllerReportSet.ReservationRequestDeletedException(reservationRequestIdentifier.formatGlobalId());
                }
            }

            cz.cesnet.shongo.controller.api.ResourceSpecification resourceSpecification = new cz.cesnet.shongo.controller.api.ResourceSpecification(resourceId);

            ReservationRequest newReservationRequest = new ReservationRequest();
            newReservationRequest.setSlot(slot);
            Specification specification = cz.cesnet.shongo.controller.booking.resource.ResourceSpecification.createFromApi(resourceSpecification, entityManager);
            newReservationRequest.setSpecification(specification);
            newReservationRequest.setPurpose(ReservationRequestPurpose.USER);
            newReservationRequest.setDescription(description);
            newReservationRequest.setCreatedBy(UserInformation.formatForeignUserId(userId, domainId));
            newReservationRequest.setUpdatedBy(UserInformation.formatForeignUserId(userId, domainId));
            if (previousReservationRequest == null) {
                reservationRequestManager.create(newReservationRequest);
                newReservationRequest.getSpecification().updateSpecificationSummary(entityManager, false);
            } else {
                previousReservationRequest.setUpdatedBy(UserInformation.formatForeignUserId(userId, domainId));
                reservationRequestManager.modify(previousReservationRequest, newReservationRequest);
            }
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
    @RequestMapping(value = InterDomainAction.DOMAIN_ALLOCATE_ROOM, method = RequestMethod.POST)
    @ResponseBody
    public Reservation handleAllocateRoom(HttpServletRequest request,
                                          @RequestParam(value = "slot", required = true) Interval slot,
                                          @RequestParam(value = "participantCount", required = true) int participantCount,
                                          @RequestParam(value = "technologies", required = true) List<Technology> technologies,
                                          @RequestParam(value = "userId", required = true) String userId,
                                          @RequestParam(value = "description", required = false) String description,
                                          @RequestParam(value = "roomPin", required = false) String roomPin,
                                          @RequestParam(value = "acRoomAccessMode", required = false) AdobeConnectPermissions acRoomAccessMode,
                                          @RequestParam(value = "roomRecorded", required = false) Boolean roomRecorded,
                                          @RequestParam(value = "reservationRequestId", required = false) String reservationRequestId,
                                          @RequestBody List<ForeignRoomParticipantRole> participants)
            throws NotAuthorizedException, ForbiddenException
    {
        EntityManager entityManager = InterDomainAgent.getInstance().createEntityManager();
        try {
            entityManager.getTransaction().begin();

            ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);

            Reservation reservation = new Reservation();

            Domain domain = getDomain(request);
            Long domainId = ObjectIdentifier.parseLocalId(domain.getId(), ObjectType.DOMAIN);
            ObjectIdentifier reservationRequestIdentifier = null;
            if (!Strings.isNullOrEmpty(reservationRequestId)) {
                reservationRequestIdentifier = ObjectIdentifier.parseTypedId(reservationRequestId, ObjectType.RESERVATION_REQUEST);
            }

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
                        throw new ControllerReportSet.ReservationRequestAlreadyModifiedException(reservationRequestIdentifier.formatGlobalId());
                    case DELETED:
                        throw new ControllerReportSet.ReservationRequestDeletedException(reservationRequestIdentifier.formatGlobalId());
                }
            }

            RoomSpecification roomSpecification = new RoomSpecification(participantCount, technologies.toArray(new Technology[technologies.size()]));
            RoomAvailability roomAvailability = roomSpecification.getAvailability();
            roomAvailability.setMeetingDescription(description);

            // Settings for specific technologies
            // H.323: room pin, recording
            if (technologies.contains(Technology.H323)) {
                roomAvailability.addServiceSpecification(new RecordingServiceSpecification(true));
                if (!Strings.isNullOrEmpty(roomPin)) {
                    H323RoomSetting h323RoomSetting = new H323RoomSetting();
                    h323RoomSetting.setPin(roomPin);
                    roomSpecification.addRoomSetting(h323RoomSetting);
                }
            }
            // Adobe Connect: access mode, room pin, participants,
            if (technologies.contains(Technology.ADOBE_CONNECT)) {
                AdobeConnectRoomSetting adobeConnectRoomSetting = new AdobeConnectRoomSetting();
                if (!Strings.isNullOrEmpty(roomPin)) {
                    adobeConnectRoomSetting.setPin(roomPin);
                }
                adobeConnectRoomSetting.setAccessMode(acRoomAccessMode);
                roomSpecification.addRoomSetting(adobeConnectRoomSetting);

                for (ForeignRoomParticipantRole participant : participants) {
                    if (participant.getUserId() == null) {
                        continue;
                    }
                    cz.cesnet.shongo.controller.api.PersonParticipant roomParticipant = participant.toApi(domainId);
                    if (roomParticipant.getPerson() instanceof ForeignPerson) {
                        ForeignPerson foreignPerson = (ForeignPerson) roomParticipant.getPerson();
                        // Add participants only if users are shared between domains
                        if (domain.isShareAuthorizationServer() && !foreignPerson.getUserInformation().getPrincipalNames().isEmpty()) {
                            roomSpecification.addParticipant(roomParticipant);
                        }
                    }
                    else {
                        throw new TodoImplementException("Unsupported person type: " + roomParticipant.getPerson().getClassName());
                    }
                }
            }

            ReservationRequest newReservationRequest = new ReservationRequest();
            newReservationRequest.setSlot(slot);
            newReservationRequest.setSpecification(cz.cesnet.shongo.controller.booking.room.RoomSpecification.createFromApi(roomSpecification, entityManager));
            newReservationRequest.setPurpose(ReservationRequestPurpose.USER);
            newReservationRequest.setDescription(description);
            newReservationRequest.setCreatedBy(UserInformation.formatForeignUserId(userId, domainId));
            newReservationRequest.setUpdatedBy(UserInformation.formatForeignUserId(userId, domainId));
            if (previousReservationRequest == null) {
                reservationRequestManager.create(newReservationRequest);
                newReservationRequest.getSpecification().updateSpecificationSummary(entityManager, false);
            } else {
                previousReservationRequest.setUpdatedBy(UserInformation.formatForeignUserId(userId, domainId));
                reservationRequestManager.modify(previousReservationRequest, newReservationRequest);
            }
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
            entityManager.getTransaction().begin();

            ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
            ResourceManager resourceManager = new ResourceManager(entityManager);
            ReservationRequest reservationRequest = (ReservationRequest) reservationRequestManager.get(requestIdentifier.getPersistenceId());
            cz.cesnet.shongo.controller.booking.reservation.Reservation currentReservation = reservationRequest.getAllocation().getCurrentReservation();

            String createdByUserId = reservationRequest.getCreatedBy();

            Specification specification = reservationRequest.getSpecification();
            if (specification instanceof cz.cesnet.shongo.controller.booking.resource.ResourceSpecification) {
                cz.cesnet.shongo.controller.booking.resource.ResourceSpecification resourceSpecification;
                resourceSpecification = (cz.cesnet.shongo.controller.booking.resource.ResourceSpecification) specification;
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

            // Authorize domain
            if (!domainId.equals(UserInformation.parseDomainId(createdByUserId)) || !requestIdentifier.isLocal()) {
                // Throw {@code ForbiddenException} for error 403 to return
                throw new ForbiddenException("Cannot get reservation");
            }

            Reservation reservation = new Reservation();

            reservation.setForeignReservationRequestId(reservationRequestId);
            if (currentReservation != null) {
                reservation.setSlot(currentReservation.getSlot());
                if (currentReservation instanceof ResourceReservation) {
                    ResourceReservation resourceReservation = (ResourceReservation) currentReservation;
                    String resourceId = ObjectIdentifier.formatId(resourceReservation.getResource());

                    cz.cesnet.shongo.controller.api.domains.response.ResourceSpecification resourceSpecification;
                    resourceSpecification = new cz.cesnet.shongo.controller.api.domains.response.ResourceSpecification(resourceId);
                    reservation.setSpecification(resourceSpecification);
                }
                else if (currentReservation instanceof RoomReservation) {
                    RoomReservation roomReservation = (RoomReservation) currentReservation;

                    cz.cesnet.shongo.controller.booking.room.RoomSpecification roomSpecification;
                    roomSpecification = (cz.cesnet.shongo.controller.booking.room.RoomSpecification) specification;

                    cz.cesnet.shongo.controller.api.domains.response.RoomSpecification foreignRoomSpecification;
                    foreignRoomSpecification = new cz.cesnet.shongo.controller.api.domains.response.RoomSpecification();
                    foreignRoomSpecification.setLicenseCount(roomReservation.getLicenseCount());
                    foreignRoomSpecification.setTechnologies(new HashSet<>(roomSpecification.getTechnologies()));
                    ExecutableState state = roomReservation.getEndpoint().getState().toApi();
                    foreignRoomSpecification.setState(RoomState.fromApi(state));

                    String roomName = null;
                    for (Alias alias : roomReservation.getEndpoint().getAliases()) {
                        if (alias.getType() == AliasType.ROOM_NAME) {
                            roomName = alias.getValue();
                        }
                    }
                    foreignRoomSpecification.setRoomName(roomName);
                    for (cz.cesnet.shongo.controller.booking.alias.Alias alias : roomReservation.getEndpoint().getAliases()) {
                        foreignRoomSpecification.addAlias(alias.getType(), alias.getValue());
                    }

                    //TODO:
                    for (cz.cesnet.shongo.controller.booking.executable.ExecutableService service : roomReservation.getEndpoint().getServices()) {
                        if (service instanceof cz.cesnet.shongo.controller.booking.recording.RecordingService) {
                            RecordingService recordingService = (RecordingService) service;
                            foreignRoomSpecification.setRecorded(true);
                            foreignRoomSpecification.setRecordingActive(recordingService.isActive());
                        } else {
                            throw new TodoImplementException("Unsupported service" + service.getClass());
                        }
                    }

                    reservation.setSpecification(foreignRoomSpecification);
                }
            }

            switch (reservationRequest.getAllocationState()) {
                case ALLOCATION_FAILED:
                    reservation.setStatus(AbstractResponse.Status.FAILED);

                    SchedulerReport report = reservationRequest.getReports().get(reservationRequest.getReports().size() - 1);
                    reservation.setMessage("TODO: " + report.toString());
                    break;
                case ALLOCATED:
                    String reservationId = ObjectIdentifier.formatId(currentReservation);
                    reservation.setForeignReservationId(reservationId);
                    break;
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
            entityManager.getTransaction().begin();

            ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
            ResourceManager resourceManager = new ResourceManager(entityManager);
            ReservationRequest reservationRequest;
            try {
                reservationRequest = (ReservationRequest) reservationRequestManager.get(requestIdentifier.getPersistenceId());
            } catch (CommonReportSet.ObjectNotExistsException ex) {
                return new AbstractResponse()
                {
                };
            }

            String createdByUserId = reservationRequest.getCreatedBy();

            Specification specification = reservationRequest.getSpecification();
            if (specification instanceof cz.cesnet.shongo.controller.booking.resource.ResourceSpecification) {
                cz.cesnet.shongo.controller.booking.resource.ResourceSpecification resourceSpecification;
                resourceSpecification = (cz.cesnet.shongo.controller.booking.resource.ResourceSpecification) specification;
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
            try {
                getDomainService().deleteReservationRequest(reservationRequest);
            } catch (ControllerReportSet.ReservationRequestDeletedException ex) {
                // continue - ReservationRequest already deleted
            }

            return returnOk();
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
            entityManager.getTransaction().begin();

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
                DomainCapability.Type type = DomainCapability.Type.RESOURCE;
                List<DomainCapability> capabilities = getDomainService().listLocalResourcesByDomain(domainId, type, null, null);
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

    @Override
    @RequestMapping(value = InterDomainAction.DOMAIN_VIRTUAL_ROOM_PARTICIPANT_UPDATE, method = RequestMethod.POST)
    @ResponseBody
    public AbstractResponse handleSetParticipants(HttpServletRequest request,
                                                  @RequestParam(value = "reservationRequestId", required = true) String reservationRequestId,
                                                  @RequestBody List<ForeignRoomParticipantRole> participants)
            throws NotAuthorizedException, ForbiddenException
    {
        EntityManager entityManager = InterDomainAgent.getInstance().createEntityManager();
        try {
            entityManager.getTransaction().begin();
            AbstractReservationRequest reservationRequest = validateReservationRequestsDomain(request, reservationRequestId, entityManager);
            Long domainId = ObjectIdentifier.parseLocalId(getDomain(request).getId(), ObjectType.DOMAIN);

            ExecutableManager executableManager = new ExecutableManager(entityManager);

            cz.cesnet.shongo.controller.booking.executable.Executable executable = getExecutable(reservationRequest);
            RoomEndpoint roomEndpoint;
            if (executable instanceof RoomEndpoint) {
                roomEndpoint = (RoomEndpoint) executable;
            } else {
                throw new TodoImplementException();
            }

            List<cz.cesnet.shongo.controller.booking.participant.AbstractParticipant> abstractParticipants = new ArrayList<>();
            for (ForeignRoomParticipantRole participant : participants) {
                cz.cesnet.shongo.controller.booking.participant.PersonParticipant personParticipant;
                personParticipant = (PersonParticipant) PersonParticipant.createFromApi(participant.toApi(domainId), entityManager);
                abstractParticipants.add(personParticipant);
            }
            roomEndpoint.setParticipants(abstractParticipants);

            executableManager.update(roomEndpoint);

            return returnOk();
        } finally {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            entityManager.close();
        }
    }

    @Override
    @RequestMapping(value = InterDomainAction.DOMAIN_VIRTUAL_ROOM_PARTICIPANT_LIST, method = RequestMethod.GET)
    @ResponseBody
    public List<RoomParticipant> handleGetParticipants(HttpServletRequest request,
                                                       @RequestParam(value = "reservationRequestId", required = true) String reservationRequestId)
            throws NotAuthorizedException, ForbiddenException
    {
        EntityManager entityManager = InterDomainAgent.getInstance().createEntityManager();

        try {
            AbstractReservationRequest reservationRequest = validateReservationRequestsDomain(request, reservationRequestId, entityManager);

            RoomExecutable roomExecutable = getRoomExecutable(reservationRequest, entityManager);
            String deviceResourceId = roomExecutable.getResourceId();
            String roomId = roomExecutable.getRoomId();
            String agentName = validateRoom(deviceResourceId, roomId, entityManager);

            Object participants = performDeviceCommand(deviceResourceId, agentName, new ListRoomParticipants(roomId));

            List<RoomParticipant> roomParticipants = new ArrayList<>();
            for (cz.cesnet.shongo.api.RoomParticipant participant : (List<cz.cesnet.shongo.api.RoomParticipant>) participants) {
                RoomParticipant roomParticipant = RoomParticipant.createFromApi(participant);
                String userId = roomParticipant.getUserId();
                // Return user-id only for users from that domain
                // TODO: return from other domains too
                if (UserInformation.isLocal(userId)) {
                    roomParticipant.setUserId(null);
                } else {
                    Long participantDomainId = UserInformation.parseDomainId(userId);
                    Long requestDomainId = ObjectIdentifier.parseLocalId(getDomain(request).getId(), ObjectType.DOMAIN);

                    if (requestDomainId.equals(participantDomainId)) {
                        roomParticipant.setUserId(UserInformation.parseUserId(userId));
                    } else {
                        roomParticipant.setUserId(null);
                    }
                }
                roomParticipants.add(roomParticipant);
            }

            return roomParticipants;
        } finally {
            entityManager.close();
        }
    }

    @Override
    @RequestMapping(value = InterDomainAction.DOMAIN_VIRTUAL_ROOM_ACTION, method = RequestMethod.POST)
    @ResponseBody
    public AbstractResponse handleRoomAction(HttpServletRequest request,
                                             @RequestParam(value = "reservationRequestId", required = true) String reservationRequestId,
                                             @RequestBody AbstractDomainRoomAction action)
            throws NotAuthorizedException, ForbiddenException
    {
        EntityManager entityManager = InterDomainAgent.getInstance().createEntityManager();
        try {
            AbstractReservationRequest reservationRequest = validateReservationRequestsDomain(request, reservationRequestId, entityManager);

            RoomExecutable roomExecutable = getRoomExecutable(reservationRequest, entityManager);
            String deviceResourceId = roomExecutable.getResourceId();
            String roomId = roomExecutable.getRoomId();
            String agentName = validateRoom(deviceResourceId, roomId, entityManager);

            ConnectorCommand command = action.toApi();
            if (command instanceof GetRoom) {
                ((GetRoom) command).setRoomId(roomId);
            } else if (command instanceof DisconnectRoomParticipant) {
                ((DisconnectRoomParticipant) command).setRoomId(roomId);
            } else if (command instanceof ModifyRoomParticipant) {
                ((ModifyRoomParticipant) command).getRoomParticipant().setRoomId(roomId);
            } else {
                throw new TodoImplementException("Unsupported command.");
            }

            Object commandResult = performDeviceCommand(deviceResourceId, agentName, command);

            if (commandResult instanceof Room) {
                Room roomApi = (Room) commandResult;
                cz.cesnet.shongo.controller.api.domains.response.Room room;
                room = cz.cesnet.shongo.controller.api.domains.response.Room.createFromApi(roomApi);
                return room;
            } else {
                // Return ok status when command is void
                // classes: DisconnectRoomParticipant, ModifyRoomParticipant
                return returnOk();
            }
        } finally {
            entityManager.close();
        }
    }

    @Override
    @RequestMapping(value = InterDomainAction.DOMAIN_RESERVATION_DELETED, method = RequestMethod.GET)
    @ResponseBody
    public AbstractResponse handleDeletedReservation(HttpServletRequest request,
                                                     @RequestParam(value = "reservationRequestId", required = true) String foreignReservationRequestId,
                                                     @RequestParam(value = "reason", required = true) String reason)
            throws NotAuthorizedException, ForbiddenException
    {
        EntityManager entityManager = InterDomainAgent.getInstance().createEntityManager();
        try {
            entityManager.getTransaction().begin();
            AbstractReservationRequest reservationRequest = validateReservationRequestsDomain(request, foreignReservationRequestId, entityManager);
            ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
            reservationRequestManager.delete(reservationRequest, false);

            InterDomainAgent.getInstance().logAndNotifyDomainAdmins("Foreign reservation has been deleted (reservation_request_id: " + foreignReservationRequestId + ").");

            return returnOk();
        } finally {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            entityManager.close();
        }
    }

    private AbstractResponse returnOk()
    {
        return new AbstractResponse()
        {
        };
    }

    private void throwForbiddenException(Long domainId) throws ForbiddenException
    {
        throw new ForbiddenException("Domain (id: " + domainId + ") doesn't have sufficient permissions or object doesn't exist." );
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
        InterDomainAgent.getInstance().getLogger().debug("Internal error ocured during interdomain communication: ",ex);
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

    public class EnumEditor<T extends Enum<T>> extends PropertyEditorSupport
    {
        Class<T> enumType;

        public EnumEditor(Class<T> enumType)
        {
            this.enumType = enumType;
        }

        @Override
        public String getAsText()
        {
            if (getValue() == null) {
                return "";
            }
            T value = (T) getValue();
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
                setValue(T.valueOf(enumType, text));
            }
        }
    }


    private AbstractReservationRequest validateReservationRequestsDomain(HttpServletRequest request, String reservationRequestId, EntityManager entityManager)
            throws NotAuthorizedException, ForbiddenException
    {
        Long domainId = ObjectIdentifier.parseLocalId(getDomain(request).getId(), ObjectType.DOMAIN);
        ObjectIdentifier requestIdentifier = ObjectIdentifier.parseTypedId(reservationRequestId, ObjectType.RESERVATION_REQUEST);

        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        AbstractReservationRequest reservationRequest = null;
        try {
            reservationRequest = reservationRequestManager.get(requestIdentifier.getPersistenceId());
        } catch (CommonReportSet.ObjectNotExistsException ex) {
            throwForbiddenException(domainId);
        }

        if (reservationRequest == null) {
            throwForbiddenException(domainId);
        }
        String createdByUserId = reservationRequest.getCreatedBy();

        if (!domainId.equals(UserInformation.parseDomainId(createdByUserId)) || !requestIdentifier.isLocal()) {
            // Throw {@code ForbiddenException} for error 403 to return
            throwForbiddenException(domainId);
        }

        return reservationRequest;
    }

    /**
     * @param deviceResourceId
     * @return agent name
     */
    private String validateRoom(String deviceResourceId, String roomId, EntityManager entityManager)
    {
        checkNotNull("deviceResourceId", deviceResourceId);
        checkNotNull("roomId", roomId);

        ResourceManager resourceManager = new ResourceManager(entityManager);
        ObjectIdentifier deviceResourceIdentifier = ObjectIdentifier.parse(deviceResourceId, ObjectType.RESOURCE);
        cz.cesnet.shongo.controller.booking.resource.DeviceResource deviceResource = resourceManager.getDevice(deviceResourceIdentifier.getPersistenceId());
        String agentName = getAgentName(deviceResource);
        return agentName;
    }

    /**
     * Check whether argumetn is not null.
     *
     * @param argumentName
     * @param argumentValue
     */
    protected void checkNotNull(String argumentName, Object argumentValue)
    {
        if (argumentValue == null) {
            throw new IllegalArgumentException("Argument " + argumentName + " must be not null.");
        }
    }

    /**
     * Gets name of agent managing a given device.
     *
     * @param deviceResource of which the agent name should be get
     * @return agent name of managed resource with given {@code deviceResourceId}
     */
    protected String getAgentName(cz.cesnet.shongo.controller.booking.resource.DeviceResource deviceResource)
    {
        Mode mode = deviceResource.getMode();
        if (mode instanceof cz.cesnet.shongo.controller.booking.resource.ManagedMode) {
            cz.cesnet.shongo.controller.booking.resource.ManagedMode managedMode = (cz.cesnet.shongo.controller.booking.resource.ManagedMode) mode;
            return managedMode.getConnectorAgentName();
        }
        throw new RuntimeException(String.format("Resource '%s' is not managed!",
                ObjectIdentifier.formatId(deviceResource)));
    }

    private RoomExecutable getRoomExecutable(AbstractReservationRequest reservationRequest, EntityManager entityManager)
    {

        Executable executableApi = getExecutable(reservationRequest).toApi(entityManager, Report.UserType.DOMAIN_ADMIN);
        if (executableApi instanceof RoomExecutable) {
            return (RoomExecutable) executableApi;
        }
        else {
            throw new TodoImplementException();
        }

    }

    private cz.cesnet.shongo.controller.booking.executable.Executable getExecutable(AbstractReservationRequest reservationRequest)
    {

        Allocation allocation = reservationRequest.getAllocation();
        if (allocation == null) {
            throw new RuntimeException("Failed to execute command for this room.");
        }
        cz.cesnet.shongo.controller.booking.reservation.Reservation reservation = allocation.getCurrentReservation();
        if (reservation == null) {
            throw new RuntimeException("Failed to execute command for this room.");
        }
        return reservation.getExecutable();
    }

    /**
     * Asks the local controller agent to send a command to be performed by a device.
     *
     * @param agentName on which the command should be performed
     * @param command    command to be performed by the device
     */
    private Object performDeviceCommand(String deviceResourceId, String agentName, ConnectorCommand command)
    {
        SendLocalCommand sendLocalCommand = cz.cesnet.shongo.controller.Controller.getInstance().getAgent().sendCommand(agentName, command);
        if (sendLocalCommand.getState() == SendLocalCommand.State.SUCCESSFUL) {
            return sendLocalCommand.getResult();
        }
        throw new ControllerReportSet.DeviceCommandFailedException(
                deviceResourceId, command.toString(), sendLocalCommand.getJadeReport());
    }
}