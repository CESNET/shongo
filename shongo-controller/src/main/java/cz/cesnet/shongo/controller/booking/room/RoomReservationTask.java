package cz.cesnet.shongo.controller.booking.room;

import com.google.common.base.Strings;
import cz.cesnet.shongo.*;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.Controller;
import cz.cesnet.shongo.controller.ObjectType;
import cz.cesnet.shongo.controller.api.domains.request.CapabilitySpecificationRequest;
import cz.cesnet.shongo.controller.api.domains.request.RoomSettings;
import cz.cesnet.shongo.controller.api.domains.response.*;
import cz.cesnet.shongo.controller.api.domains.response.RoomSpecification;
import cz.cesnet.shongo.controller.api.request.DomainCapabilityListRequest;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.booking.Allocation;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.TechnologySet;
import cz.cesnet.shongo.controller.booking.alias.Alias;
import cz.cesnet.shongo.controller.booking.alias.AliasReservation;
import cz.cesnet.shongo.controller.booking.alias.AliasReservationTask;
import cz.cesnet.shongo.controller.booking.alias.AliasSpecification;
import cz.cesnet.shongo.controller.booking.domain.Domain;
import cz.cesnet.shongo.controller.booking.domain.DomainResource;
import cz.cesnet.shongo.controller.booking.executable.Executable;
import cz.cesnet.shongo.controller.booking.executable.ExecutableManager;
import cz.cesnet.shongo.controller.booking.executable.ExecutableService;
import cz.cesnet.shongo.controller.booking.participant.AbstractParticipant;
import cz.cesnet.shongo.controller.booking.participant.PersonParticipant;
import cz.cesnet.shongo.controller.booking.recording.RecordingCapability;
import cz.cesnet.shongo.controller.booking.recording.RecordingService;
import cz.cesnet.shongo.controller.booking.recording.RecordingServiceReservationTask;
import cz.cesnet.shongo.controller.booking.recording.RecordingServiceSpecification;
import cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.booking.request.ReservationRequest;
import cz.cesnet.shongo.controller.booking.reservation.*;
import cz.cesnet.shongo.controller.booking.reservation.Reservation;
import cz.cesnet.shongo.controller.booking.resource.DeviceResource;
import cz.cesnet.shongo.controller.booking.resource.ResourceManager;
import cz.cesnet.shongo.controller.booking.room.settting.AdobeConnectRoomSetting;
import cz.cesnet.shongo.controller.booking.room.settting.H323RoomSetting;
import cz.cesnet.shongo.controller.booking.room.settting.RoomSetting;
import cz.cesnet.shongo.controller.booking.specification.ExecutableServiceSpecification;
import cz.cesnet.shongo.controller.cache.Cache;
import cz.cesnet.shongo.controller.cache.ResourceCache;
import cz.cesnet.shongo.controller.domains.InterDomainAgent;
import cz.cesnet.shongo.controller.notification.NotificationState;
import cz.cesnet.shongo.controller.notification.RoomNotification;
import cz.cesnet.shongo.controller.scheduler.*;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import javax.persistence.EntityManager;
import java.util.*;

/**
 * Represents {@link ReservationTask} for a {@link RoomReservation}.<
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class RoomReservationTask extends ReservationTask
{
    /**
     * Specifies the name of the meeting which will take place in the room.
     */
    private String meetingName;

    /**
     * Specifies the description of the meeting which will take place in the room.
     */
    private String meetingDescription;

    /**
     * Number of minutes which the room shall be available before requested time slot.
     */
    private int slotMinutesBefore;

    /**
     * Number of minutes which the room shall be available after requested time slot.
     */
    private int slotMinutesAfter;

    /**
     * Specifies whether {@link RoomEndpoint} should be allocated.
     */
    private boolean allocateRoomEndpoint = true;

    /**
     * {@link RoomEndpoint} which should be reused.
     */
    private RoomEndpoint reusedRoomEndpoint;

    /**
     * Number of participants in the virtual room.
     */
    private Integer participantCount;

    /**
     * Specifies whether configured participants should  be notified about the room.
     */
    private boolean participantNotificationEnabled;

    /**
     * Collection of {@link Technology} set variants where at least one must be supported by
     * allocated {@link RoomReservation}. If empty no specific technologies are requested and
     * the all device technologies are used.
     */
    private List<Set<Technology>> technologyVariants = new LinkedList<Set<Technology>>();

    /**
     * Collection of {@link RoomSetting} for allocated {@link RoomReservation}.
     */
    private List<RoomSetting> roomSettings = new LinkedList<RoomSetting>();

    /**
     * Collection of {@link AliasSpecification} for {@link Alias}es which should be allocated for the room.
     */
    private List<AliasSpecification> aliasSpecifications = new LinkedList<AliasSpecification>();

    /**
     * {@link RoomProviderCapability} for which the {@link RoomReservation}
     * should be allocated.
     */
    private RoomProviderCapability roomProviderCapability = null;

    /**
     * List of {@link AbstractParticipant}s for the permanent room.
     */
    private List<AbstractParticipant> participants = new LinkedList<AbstractParticipant>();

    /**
     * Collection of {@link ExecutableServiceSpecification} which should be allocated for the room.
     */
    private List<ExecutableServiceSpecification> serviceSpecifications = new LinkedList<ExecutableServiceSpecification>();

    /**
     * Constructor.
     *
     * @param schedulerContext sets the {@link #schedulerContext}
     * @param slot             sets the {@link #slot}
     */
    public RoomReservationTask(SchedulerContext schedulerContext, Interval slot)
    {
        super(schedulerContext, slot);
    }

    /**
     * Constructor.
     *
     * @param schedulerContext  sets the {@link #schedulerContext}
     * @param slot              sets the {@link #slot}
     * @param slotMinutesBefore sets the {@link #slotMinutesBefore}
     * @param slotMinutesAfter  sets the {@link #slotMinutesAfter}
     */
    public RoomReservationTask(SchedulerContext schedulerContext, Interval slot, int slotMinutesBefore,
            int slotMinutesAfter)
    {
        super(schedulerContext, new Interval(
                slot.getStart().minusMinutes(slotMinutesBefore), slot.getEnd().plusMinutes(slotMinutesAfter)));
        this.slotMinutesBefore = slotMinutesBefore;
        this.slotMinutesAfter = slotMinutesAfter;
    }

    /**
     * @param meetingName sets the {@link #meetingName}
     */
    public void setMeetingName(String meetingName)
    {
        this.meetingName = meetingName;
    }

    /**
     * @param meetingDescription sets the {@link #meetingDescription}
     */
    public void setMeetingDescription(String meetingDescription)
    {
        this.meetingDescription = meetingDescription;
    }

    /**
     * @param participantCount sets the {@link #participantCount}
     */
    public void setParticipantCount(Integer participantCount)
    {
        this.participantCount = participantCount;
    }

    /**
     * @param participantNotificationEnabled sets the {@link #participantNotificationEnabled}
     */
    public void setParticipantNotificationEnabled(boolean participantNotificationEnabled)
    {
        this.participantNotificationEnabled = participantNotificationEnabled;
    }

    /**
     * @param allocateRoomEndpoint sets the {@link #allocateRoomEndpoint}
     */
    public void setAllocateRoomEndpoint(boolean allocateRoomEndpoint)
    {
        this.allocateRoomEndpoint = allocateRoomEndpoint;
    }

    /**
     * @param reusedRoomEndpoint sets the {@link #reusedRoomEndpoint}
     */
    public void setReusedRoomEndpoint(RoomEndpoint reusedRoomEndpoint)
    {
        this.reusedRoomEndpoint = reusedRoomEndpoint;
    }

    /**
     * @param technologies to be added to the {@link #technologyVariants}
     */
    public void addTechnologyVariant(Set<Technology> technologies)
    {
        if (technologies.isEmpty()) {
            if (roomProviderCapability != null) {
                technologies = roomProviderCapability.getDeviceResource().getTechnologies();
            } else {
                throw new IllegalArgumentException("Technologies cannot be empty.");
            }
        }
        technologyVariants.add(technologies);
    }

    /**
     * @param roomSettings to be added to the {@link #roomSettings}
     */
    public void addRoomSettings(Collection<RoomSetting> roomSettings)
    {
        this.roomSettings.addAll(roomSettings);
    }

    /**
     * @param aliasSpecifications to be added to the {@link #aliasSpecifications}
     */
    public void addAliasSpecifications(Collection<AliasSpecification> aliasSpecifications)
    {
        this.aliasSpecifications.addAll(aliasSpecifications);
    }

    /**
     * @param roomProviderCapability sets the {@link #roomProviderCapability}
     */
    public void setRoomProviderCapability(RoomProviderCapability roomProviderCapability)
    {
        this.roomProviderCapability = roomProviderCapability;
    }

    /**
     * @param participants to be added to the {@link #participants}
     */
    public void addParticipants(List<AbstractParticipant> participants)
    {
        this.participants.addAll(participants);
    }

    /**
     * @param serviceSpecifications to be added to the {@link #serviceSpecifications}
     */
    public void addServiceSpecifications(List<ExecutableServiceSpecification> serviceSpecifications)
    {
        this.serviceSpecifications.addAll(serviceSpecifications);
    }

    @Override
    protected SchedulerReport createMainReport()
    {
        List<TechnologySet> technologySets = new LinkedList<TechnologySet>();
        for (Set<Technology> technologies : technologyVariants) {
            technologySets.add(new TechnologySet(technologies));
        }
        return new SchedulerReportSet.AllocatingRoomReport(technologySets, participantCount,
                (roomProviderCapability != null ? roomProviderCapability.getResource() : null));
    }

    @Override
    protected Reservation allocateReservation(Reservation currentReservation) throws SchedulerException
    {
        // Update specification by reused room endpoint
        if (reusedRoomEndpoint != null) {
            // Room provider from reused room endpoint must be used
            DeviceResource deviceResource = reusedRoomEndpoint.getResource();
            roomProviderCapability = deviceResource.getCapabilityRequired(RoomProviderCapability.class);

            // Technologies from reused room endpoint must be used
            technologyVariants.clear();
            technologyVariants.add(reusedRoomEndpoint.getTechnologies());
        }

        // Check maximum duration
        if (participantCount != null && schedulerContext.isMaximumFutureAndDurationRestricted()) {
            checkMaximumDuration(slot, getCache().getRoomReservationMaximumDuration());
        }

        // Find room provider variants
        Map<cz.cesnet.shongo.controller.api.Domain, List<DomainCapability>> domainsRoomCapabilities = null;
        List<RoomProviderVariant> roomProviderVariants = new ArrayList<>();
        try {
            roomProviderVariants = getRoomProviderVariants(currentReservation);
        }
        catch (SchedulerReportSet.ResourceNotFoundException ex) {
            if (schedulerContext.isLocalByUser()) {
                // TODO more thorough check in foreign domain
                domainsRoomCapabilities = getForeignDomainsWithRoomProvider();
                if (domainsRoomCapabilities.isEmpty()) {
                    throw ex;
                }
                else if (schedulerContext.isAvailabilityCheck()) {
                    // Skip allocation when availability check, when only available resources are in foreign domain
                    return null;
                }
            }
            else {
                throw ex;
            }
        }

        // Sort room provider variants
        sortRoomProviderVariants(roomProviderVariants);

        // Check for pending foreign reservations
        boolean modifyForeign = false;
        if (currentReservation instanceof ForeignRoomReservation) {
            if (!((ForeignRoomReservation) currentReservation).isComplete()) {
                return checkPendingForeignAllocations((ForeignRoomReservation) currentReservation);
            }
            else {
                modifyForeign = true;
            }
        }

        // Try to allocate room reservation in room provider variants
        if (!modifyForeign) {
            for (RoomProviderVariant roomProviderVariant : roomProviderVariants) {
                SchedulerContextState.Savepoint schedulerContextSavepoint = schedulerContextState.createSavepoint();
                try {
                    Long allocateRoomTime = DateTime.now().getMillis();
                    Reservation reservation = allocateVariant(roomProviderVariant);
                    if (reservation != null) {
                        return reservation;
                    }
                } catch (SchedulerException exception) {
                    schedulerContextSavepoint.revert();
                } finally {
                    schedulerContextSavepoint.destroy();
                }
            }
        }

        if (domainsRoomCapabilities == null || !domainsRoomCapabilities.isEmpty()) {
            ForeignRoomReservation foreignReservation = null;
            if (modifyForeign && currentReservation instanceof ForeignRoomReservation) {
                foreignReservation = (ForeignRoomReservation) currentReservation;
            }
            Reservation reservation = tryAllocateInForeignDomains(domainsRoomCapabilities, foreignReservation);
            if (reservation != null) {
                return reservation;
            }
        }

        // TODO: zapisovat i cizi domeny do reports
        throw new SchedulerException(getCurrentReport());
    }

    private Map<cz.cesnet.shongo.controller.api.Domain, List<DomainCapability>> getForeignDomainsWithRoomProvider()
    {
        Map<cz.cesnet.shongo.controller.api.Domain, List<DomainCapability>> domainCapabilities = new HashMap<>();
        if (Controller.isInterDomainInitialized()) {
            if (technologyVariants.isEmpty()) {
                throw new IllegalStateException("Technologies must be set for room reservation.");
            }
            boolean recordingService = false;
            // Add recording capability for H.323/SIP
            for (ExecutableServiceSpecification service : serviceSpecifications) {
                if (service instanceof RecordingServiceSpecification) {
                    for (Set<Technology> technologies : technologyVariants) {
                        if (technologies.contains(Technology.H323) || technologies.contains(Technology.SIP)) {
                            recordingService = true;
                        }
                    }
                }
                else {
                    return domainCapabilities;
                }
            }

            DomainCapabilityListRequest listRequest = new DomainCapabilityListRequest();
            listRequest.setSlot(slot);
            listRequest.setOnlyAllocatable(Boolean.TRUE);
            CapabilitySpecificationRequest capabilitySpecificationRequest = new CapabilitySpecificationRequest(DomainCapability.Type.VIRTUAL_ROOM, technologyVariants, participantCount);
            listRequest.addCapabilityListRequest(capabilitySpecificationRequest);
            if (recordingService) {
                CapabilitySpecificationRequest recordingCapabilityRequest = new CapabilitySpecificationRequest(DomainCapability.Type.RECORDING_SERVICE, technologyVariants, 1);
                listRequest.addCapabilityListRequest(recordingCapabilityRequest);
            }

            Map<String, List<DomainCapability>> capabilities = InterDomainAgent.getInstance().getConnector().listForeignCapabilities(listRequest);
            // Add domains with all requested capabilities
            for (String domainName : capabilities.keySet()) {
                if (!capabilities.get(domainName).isEmpty()) {
                    cz.cesnet.shongo.controller.api.Domain domain = InterDomainAgent.getInstance().getDomainService().findDomainByName(domainName);
                    domainCapabilities.put(domain, capabilities.get(domainName));
                }
            }
            return domainCapabilities;
        }
        return domainCapabilities;
    }

    private ForeignRoomReservation tryAllocateInForeignDomains(Map<cz.cesnet.shongo.controller.api.Domain, List<DomainCapability>> domainsRoomCapabilities,
                                                               ForeignRoomReservation currentReservation)
    {
        // Skip foreign allocation if no domain is specified or reservation task is not valid for it
        if (!isValidForForeignReservation(domainsRoomCapabilities, currentReservation)) {
            return null;
        }

        schedulerContext.setRequestWantedState(ReservationRequest.AllocationState.COMPLETE);

        ForeignRoomReservation reservation = new ForeignRoomReservation();
        cz.cesnet.shongo.controller.api.domains.response.Reservation bestReservation = null;

        DateTime start = this.slot.getStart().minusMinutes(slotMinutesBefore);
        DateTime end = this.slot.getEnd().plusMinutes(slotMinutesAfter);
        Interval slot = new Interval(start, end);

        List<cz.cesnet.shongo.controller.api.domains.response.Reservation> result;
        String previousReservationRequestId = null;

        // Set domain and foreign reservation request for modification
        if (currentReservation != null) {
            cz.cesnet.shongo.controller.api.Domain domain = currentReservation.getDomain().toApi();
            previousReservationRequestId = currentReservation.getForeignReservationRequestId();
            List<DomainCapability> capabilities = domainsRoomCapabilities.get(domain);
            domainsRoomCapabilities.clear();
            domainsRoomCapabilities.put(domain, capabilities);
        }
        RoomSettings allocateRoomSettings = new RoomSettings();
        allocateRoomSettings.setParticipantCount(participantCount);
        allocateRoomSettings.setSlot(slot);
        allocateRoomSettings.setTechnologyVariants(technologyVariants);
        allocateRoomSettings.setDescription(schedulerContext.getDescription());
        allocateRoomSettings.setUserId(schedulerContext.getUserId());
        //TODO: prochazet alliasSpecificaions
        for (ExecutableServiceSpecification service : serviceSpecifications) {
            if (service instanceof RecordingServiceSpecification) {
                allocateRoomSettings.setRecordRoom(true);
            }
            else {
                throw new TodoImplementException("Unsupported service: " + service);
            }
        }
        for (RoomSetting roomSetting : roomSettings) {
            if (roomSetting instanceof H323RoomSetting) {
                H323RoomSetting h323RoomSetting = (H323RoomSetting) roomSetting;
                if (!Strings.isNullOrEmpty(h323RoomSetting.getPin())) {
                    allocateRoomSettings.setRoomPin(((H323RoomSetting) roomSetting).getPin());
                }
           }
            else if (roomSetting instanceof AdobeConnectRoomSetting) {
                AdobeConnectRoomSetting adobeConnectRoomSetting = (AdobeConnectRoomSetting) roomSetting;
                if (!Strings.isNullOrEmpty(adobeConnectRoomSetting.getPin())) {
                    allocateRoomSettings.setRoomPin(adobeConnectRoomSetting.getPin());
                }
                if (adobeConnectRoomSetting.getAccessMode() != null) {
                    allocateRoomSettings.setAcAccessMode(adobeConnectRoomSetting.getAccessMode());
                }
            }
            else {
                throw new TodoImplementException("Unsupported room settings: " + roomSetting);
            }
        }
        if (participants != null) {
            for (AbstractParticipant participant : participants) {
                if (participant instanceof PersonParticipant) {
                    PersonParticipant personParticipant = (PersonParticipant) participant;
                    PersonInformation personInformation = personParticipant.getPersonInformation();
                    if (personInformation instanceof UserInformation) {
                        allocateRoomSettings.addParticipant((UserInformation) personInformation, personParticipant.getRole());
                    } else {
                        throw new TodoImplementException("Unsupported person information: " + personInformation.getClass());
                    }
                } else {
                    throw new TodoImplementException("Unsupported participant type: " + participant.getClass());
                }
            }
        }

        result = InterDomainAgent.getInstance().getConnector().allocateRoom(domainsRoomCapabilities, allocateRoomSettings, previousReservationRequestId);

        SortedSet<cz.cesnet.shongo.controller.api.domains.response.Reservation> reservations = new TreeSet<>(result);
        if (!reservationsPending(reservations) && !reservations.isEmpty()) {
            bestReservation = reservations.first();
        }
        for (cz.cesnet.shongo.controller.api.domains.response.Reservation candidateReservation : result) {
            // Add all created foreign reservation requests ids
            reservation.addForeignReservationRequestId(candidateReservation.getForeignReservationRequestId());
        }

        if (bestReservation != null) {
            updateForeignRoomReservation(currentReservation, bestReservation);
        }
        return reservation;
    }

    /**
     * Check if {@link this} is configured for foreign reservations
     *
     * @param domainsRoomCapabilities specifies if there are available domains
     * @param currentReservation specifies if there is foreign reservation request id for modification
     * @return if foreign allocation can be used
     */
    private boolean isValidForForeignReservation(Map<cz.cesnet.shongo.controller.api.Domain, List<DomainCapability>> domainsRoomCapabilities, ForeignRoomReservation currentReservation)
    {
        if (!Controller.isInterDomainInitialized()) {
            return false;
        }
        // Skip when no domains to call and not modification neither
        if (domainsRoomCapabilities == null || domainsRoomCapabilities.isEmpty()) {
            if (currentReservation == null || Strings.isNullOrEmpty(currentReservation.getForeignReservationRequestId())) {
                return false;
            }
        }
        // Do not allow empty reservations or permanent rooms
        if (participantCount < 1) {
            return false;
        }
        // Exact resource is specified
        if (roomProviderCapability != null) {
            return false;
        }
        if (!schedulerContext.isLocalByUser()) {
            return false;
        }
        // Used only for local allocations
        if (!schedulerContext.isExecutableAllowed()) {
            return false;
        }
        if (!allocateRoomEndpoint) {
            return false;
        }
        if (reusedRoomEndpoint != null) {
            return false;
        }

        return true;
    }

    private ForeignRoomReservation checkPendingForeignAllocations(ForeignRoomReservation currentReservation) throws SchedulerReportSet.ResourceNotFoundException
    {
        if (!Controller.isInterDomainInitialized()) {
            throw new IllegalStateException("Inter domain controller must be running for foreign allocations.");
        }
        List<cz.cesnet.shongo.controller.api.domains.response.Reservation> result;
        result = InterDomainAgent.getInstance().getConnector().getReservationsByRequests(currentReservation.getForeignReservationRequestsIds());
        SortedSet<cz.cesnet.shongo.controller.api.domains.response.Reservation> reservations;
        reservations = new TreeSet<>(result);
        // Test if all any request is still pending (if request is processed it must be failed or success with reservation)
        if (!reservationsPending(reservations)) {
            cz.cesnet.shongo.controller.api.domains.response.Reservation bestReservation = reservations.first();

            updateForeignRoomReservation(currentReservation, bestReservation);

            EntityManager entityManager = schedulerContext.getBypassEntityManager();
            try {
                entityManager.getTransaction().begin();
                // Delete unwanted reservation request in foreign domains
                Iterator<String> iterator = currentReservation.getForeignReservationRequestsIds().iterator();
                while (iterator.hasNext()) {
                    String foreignReservationRequestId = iterator.next();
                    if (!foreignReservationRequestId.equals(currentReservation.getForeignReservationRequestId())) {
                        createReservationForDeletion(foreignReservationRequestId, entityManager);
                    }
                    iterator.remove();
                }

                boolean allocationFailed = false;
                Allocation allocation = currentReservation.getAllocation();
                // Delete reservation when it has no foreign reservation requests
                if (currentReservation.isEmpty()) {
                    // Detach from common entityManager
                    schedulerContext.getEntityManager().detach(allocation);
                    schedulerContext.getEntityManager().detach(currentReservation);

                    // Fetch allocation amd reservation for bypassEntityManager
                    Allocation allocationToUpdate = entityManager.find(allocation.getClass(), allocation.getId());
                    Reservation reservationToDelete = entityManager.find(currentReservation.getClass(), currentReservation.getId());

                    allocationToUpdate.removeReservation(reservationToDelete);
                    entityManager.merge(allocationToUpdate);
                    entityManager.remove(reservationToDelete);
                    allocationFailed = true;
                }
                // Delete temporal reservation when modification failed and set new foreign reservation request
                else if (!bestReservation.isAllocated()) {
                    // Detach from common entityManager
                    schedulerContext.getEntityManager().detach(allocation);
                    schedulerContext.getEntityManager().detach(currentReservation);

                    // Fetch allocation amd reservation for bypassEntityManager
                    Allocation allocationToUpdate = entityManager.find(allocation.getClass(), allocation.getId());
                    Reservation reservationToUpdate = entityManager.find(currentReservation.getClass(), currentReservation.getId());

                    allocationToUpdate.removeReservation(reservationToUpdate);
                    Reservation previousReservation = allocationToUpdate.getCurrentReservation();
                    if (previousReservation instanceof ForeignRoomReservation) {
                        ForeignRoomReservation foreignRoomReservation = (ForeignRoomReservation) previousReservation;
                        foreignRoomReservation.setForeignReservationRequestId(bestReservation.getForeignReservationRequestId());
                    }
                    entityManager.merge(allocationToUpdate);
                    entityManager.merge(reservationToUpdate);
                    allocationFailed = true;
                }
                entityManager.getTransaction().commit();

                if (allocationFailed) {
                    throw new SchedulerReportSet.ResourceNotFoundException();
                }
            }
            finally {
                if (entityManager.getTransaction().isActive()) {
                    entityManager.getTransaction().rollback();
                }
            }
        }
        else {
            // Not finish reservation request
            schedulerContext.setRequestWantedState(ReservationRequest.AllocationState.COMPLETE);
        }
        return  currentReservation;
    }

    private void assignForeignExecutable(ForeignRoomReservation currentReservation, cz.cesnet.shongo.controller.api.domains.response.Reservation bestReservation)
    {
        EntityManager entityManager = schedulerContext.getEntityManager();
        AuthorizationManager authorizationManager = schedulerContext.getAuthorizationManager();
        ExecutableManager executableManager = new ExecutableManager(entityManager);

        // Allocated room endpoint
        currentReservation.setSlotStart(bestReservation.getSlotStart());
        currentReservation.setSlotEnd(bestReservation.getSlotEnd());
        RoomSpecification roomSpecification = (RoomSpecification) bestReservation.getSpecification();
        // Room configuration
        RoomConfiguration roomConfiguration = new RoomConfiguration();
        roomConfiguration.setTechnologies(roomSpecification.getTechnologies());
        roomConfiguration.setLicenseCount(roomSpecification.getLicenseCount());
        for (RoomSetting roomSetting : roomSettings) {
            try {
                roomConfiguration.addRoomSetting(roomSetting.clone());
            }
            catch (CloneNotSupportedException exception) {
                throw new RuntimeException(exception);
            }
        }

//                    addReport(new SchedulerReportSet.AllocatingExecutableReport());
            // Allocate new ResourceRoomEndpoint
        ForeignRoomEndpoint resourceRoomEndpoint = new ForeignRoomEndpoint();
        resourceRoomEndpoint.setForeignReservationRequestId(bestReservation.getForeignReservationRequestId());
        resourceRoomEndpoint.setRoomConfiguration(roomConfiguration);
        resourceRoomEndpoint.setSlot(slot);
        resourceRoomEndpoint.setSlotMinutesBefore(slotMinutesBefore);
        resourceRoomEndpoint.setSlotMinutesAfter(slotMinutesAfter);
        resourceRoomEndpoint.setMeetingName(meetingName);
        resourceRoomEndpoint.setMeetingDescription(meetingDescription);
        resourceRoomEndpoint.setRoomDescription(schedulerContext.getDescription());
        resourceRoomEndpoint.setParticipants(participants);
        resourceRoomEndpoint.setParticipantNotificationEnabled(participantNotificationEnabled);
        String executableState = roomSpecification.getState().toApi().toString();
        resourceRoomEndpoint.setState(Executable.State.valueOf(executableState));
        //TODO: recording???
        if (resourceRoomEndpoint.getState().equals(Executable.State.STARTED)) {
            resourceRoomEndpoint.setModified(true);
        }
        for (cz.cesnet.shongo.controller.api.domains.response.Alias alias : roomSpecification.getAliases()) {
            Alias persistenceAlias = new Alias(alias.getType(), alias.getValue());
            try {
                resourceRoomEndpoint.addAssignedAlias(persistenceAlias);
            } catch (SchedulerException e) {
                throw new TodoImplementException("Should not happen.", e);
            }
        }

        // Set executable to room reservation
        currentReservation.setExecutable(resourceRoomEndpoint);

        executableManager.create(resourceRoomEndpoint);
        // Set ACL for executable
        authorizationManager.createAclEntriesForChildEntity(currentReservation, resourceRoomEndpoint);

        // Notify participants
        if (resourceRoomEndpoint != null && resourceRoomEndpoint.isParticipantNotificationEnabled()) {
            schedulerContextState.addNotification(new RoomNotification.RoomCreated(resourceRoomEndpoint));
        }

    }

   private void updateForeignRoomReservation(ForeignRoomReservation currentReservation,
                                             cz.cesnet.shongo.controller.api.domains.response.Reservation bestReservation)
    {
        if (bestReservation.hasForeignReservation()) {
            // Set foreign reservation request id and domain (foreign reservation request won't be deleted)
            updateCurrentReservation(currentReservation, bestReservation);

            // Set {@code currentReservation} and add {@link ForeignRoomEndpoint} to it
            if (bestReservation.isAllocated()) {
                assignForeignExecutable(currentReservation, bestReservation);
            }
        }
        // Sets {@code schedulerContext} wanted allocation state and {@code currentReservation} state
        currentReservation.setCompletedByState(schedulerContext, bestReservation);
    }

    private void updateCurrentReservation(ForeignRoomReservation reservationToUpdate,
                                          cz.cesnet.shongo.controller.api.domains.response.Reservation reservationResponse)
    {
        reservationToUpdate.setForeignReservationRequestId(reservationResponse.getForeignReservationRequestId());

        String domainName = ObjectIdentifier.parseForeignDomain(reservationResponse.getForeignReservationRequestId());
        ResourceManager resourceManager = new ResourceManager(schedulerContext.getEntityManager());
        Domain domain = resourceManager.getDomainByName(domainName);

        reservationToUpdate.setDomain(domain);
    }

    private void createReservationForDeletion(String foreignReservationRequestsId, EntityManager entityManager)
    {
        ResourceManager resourceManager = new ResourceManager(entityManager);
        ReservationManager reservationManager = new ReservationManager(entityManager);

        String domainName = ObjectIdentifier.parseForeignDomain(foreignReservationRequestsId);

        ForeignRoomReservation reservation = new ForeignRoomReservation();
        reservation.setUserId(Authorization.ROOT_USER_ID);
        reservation.setComplete(true);
        reservation.setDomain(resourceManager.getDomainByName(domainName));
        reservation.setForeignReservationRequestId(foreignReservationRequestsId);

        reservationManager.create(reservation);
    }


    private boolean reservationsPending(Set<cz.cesnet.shongo.controller.api.domains.response.Reservation> reservations)
    {
        for (cz.cesnet.shongo.controller.api.domains.response.Reservation reservation : reservations) {
            if (reservation.success() && !reservation.isAllocated()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void migrateReservation(Reservation oldReservation, Reservation newReservation, EntityManager entityManager)
            throws SchedulerException
    {
        if (oldReservation.getExecutable() == null) {
            return;
        }
        Executable oldExecutable = oldReservation.getExecutable();
        Executable newExecutable = newReservation.getExecutable();

        // Skip migration for pending foreign reservation
        if (oldExecutable instanceof ForeignRoomEndpoint && newExecutable == null) {
            return;
        }

        if (oldExecutable instanceof RoomEndpoint && newExecutable instanceof RoomEndpoint) {
            newExecutable.setMigrateFromExecutable(oldExecutable);

            RoomEndpoint oldRoomEndpoint = (RoomEndpoint) oldExecutable;
            RoomEndpoint newRoomEndpoint = (RoomEndpoint) newExecutable;

            // Migrate participant notification state
            NotificationState notificationState = newRoomEndpoint.getParticipantNotificationState();
            newRoomEndpoint.setParticipantNotificationState(oldRoomEndpoint.getParticipantNotificationState());
            entityManager.remove(notificationState);
        }

        if (oldExecutable instanceof ResourceRoomEndpoint && newExecutable instanceof ResourceRoomEndpoint) {
            ResourceRoomEndpoint oldRoomEndpoint = (ResourceRoomEndpoint) oldExecutable;
            ResourceRoomEndpoint newRoomEndpoint = (ResourceRoomEndpoint) newExecutable;

            // Migrate recording folders
            Map<RecordingCapability, String> recordingFolderIds =
                    new HashMap<RecordingCapability, String>(oldRoomEndpoint.getRecordingFolderIds());
            for(Map.Entry<RecordingCapability, String> entry : recordingFolderIds.entrySet()) {
                newRoomEndpoint.putRecordingFolderId(entry.getKey(), entry.getValue());
                oldRoomEndpoint.removeRecordingFolderId(entry.getKey());
            }
        }

        // Migrate services
        List<ExecutableService> newExecutableServices = new LinkedList<ExecutableService>(newExecutable.getServices());
        for (ExecutableService oldExecutableService : oldExecutable.getServices()) {
            oldExecutableService = PersistentObject.getLazyImplementation(oldExecutableService);
            if (!oldExecutableService.isActive()) {
                continue;
            }
            for (ExecutableService newExecutableService : newExecutableServices) {
                if (oldExecutableService.getClass().equals(newExecutableService.getClass())) {
                    if (newExecutableService.migrate(oldExecutableService)) {
                        newExecutableServices.remove(newExecutableService);
                        break;
                    }
                }
            }
        }
        super.migrateReservation(oldReservation, newReservation, entityManager);
    }

    /**
     * @return list of possible {@link RoomProviderVariant}s
     * @throws SchedulerException when none {@link RoomProviderVariant} is found
     */
    private List<RoomProviderVariant> getRoomProviderVariants(Reservation currentReservation)
            throws SchedulerException
    {
        Cache cache = getCache();
        ResourceCache resourceCache = cache.getResourceCache();

        // Get possible room providers
        Collection<RoomProviderCapability> roomProviderCapabilities;
        if (roomProviderCapability != null) {
            // Use only specified room provider
            roomProviderCapabilities = new LinkedList<RoomProviderCapability>();
            roomProviderCapabilities.add(roomProviderCapability);
        }
        else {
            // Use all room providers from the cache
            roomProviderCapabilities = resourceCache.getCapabilities(RoomProviderCapability.class);
        }

        // Filter capabilities for foreign domains
        filterCapabilitiesByUser(roomProviderCapabilities, schedulerContext.getUserId());

        // Available room endpoints
        Collection<AvailableExecutable<RoomEndpoint>> availableRoomEndpoints =
                schedulerContextState.getAvailableExecutables(RoomEndpoint.class);

        // Find all matching room provider variants
        beginReport(new SchedulerReportSet.FindingAvailableResourceReport());
        List<RoomProviderVariant> roomProviderVariants = new LinkedList<RoomProviderVariant>();
        for (RoomProviderCapability roomProviderCapability : roomProviderCapabilities) {
            DeviceResource deviceResource = roomProviderCapability.getDeviceResource();

            // Check technology
            List<Set<Technology>> technologyVariants = new LinkedList<Set<Technology>>(this.technologyVariants);
            if (technologyVariants.size() == 0) {
                technologyVariants.add(deviceResource.getTechnologies());
            }
            for (Iterator<Set<Technology>> iterator = technologyVariants.iterator(); iterator.hasNext(); ) {
                if (!deviceResource.hasTechnologies(iterator.next())) {
                    iterator.remove();
                }
            }
            if (technologyVariants.size() == 0) {
                continue;
            }

            // Check whether room provider can be allocated
            try {
                resourceCache.checkCapabilityAvailable(roomProviderCapability, slot, schedulerContext, this);
            }
            catch (SchedulerException exception) {
                addReport(exception.getReport());
                continue;
            }

            // Add matching technology variants
            RoomProvider roomProvider = null;
            for (Set<Technology> technologies : technologyVariants) {
                if (!deviceResource.hasTechnologies(technologies)) {
                    continue;
                }
                // Lazy initialization of room provider (only when some technology variant matches)
                if (roomProvider == null) {
                    roomProvider = new RoomProvider(roomProviderCapability,
                            schedulerContext.getAvailableRoom(roomProviderCapability, slot, this));
                }

                RoomProviderVariant roomProviderVariant;
                if (participantCount != null) {
                    // Create variant by participants
                    roomProviderVariant = new RoomProviderVariant(roomProvider, participantCount, technologies);
                    // Check available license count
                    AvailableRoom availableRoom = roomProvider.getAvailableRoom();
                    int availableLicenseCount = availableRoom.getAvailableLicenseCount();
                    int maxLicencesPerRoom = availableRoom.getMaxLicencesPerRoom();
                    int requestedLicenseCount = roomProviderVariant.getLicenseCount();
                    // Check allowed licence count for foreign requests
                    if (!schedulerContext.isLocalByUser()) {
                        int leftForeignLicenseCount = getRemainingLicenseCount(availableRoom.getDeviceResource(), schedulerContext.getUserId(), currentReservation);
                        if (leftForeignLicenseCount > -1) {
                            availableLicenseCount = leftForeignLicenseCount < availableLicenseCount ? leftForeignLicenseCount : availableLicenseCount;
                        }
                        // When recording is set for reservation request from foreign domain, add +1 license for H323/SIP
                        if (technologies.contains(Technology.H323) || technologies.contains(Technology.SIP)) {
                            for (ExecutableServiceSpecification specification : serviceSpecifications) {
                                if (specification instanceof RecordingServiceSpecification) {
                                    requestedLicenseCount++;
                                }
                            }
                        }
                    }
                    if (availableLicenseCount < requestedLicenseCount) {
                        addReport(new SchedulerReportSet.ResourceRoomCapacityExceededReport(
                                deviceResource, availableLicenseCount, availableRoom.getMaximumLicenseCount()));
                        continue;
                    }
                    // 0 means no restrictions
                    if (maxLicencesPerRoom > 0) {
                        if (maxLicencesPerRoom < requestedLicenseCount) {
                            addReport(new SchedulerReportSet.ResourceSingleRoomLimitExceededReport(
                                    deviceResource, maxLicencesPerRoom));
                            continue;
                        }
                    }
                }
                else {
                    // Create variant without participants
                    roomProviderVariant = new RoomProviderVariant(roomProvider, technologies);
                }
                roomProvider.addRoomProviderVariant(roomProviderVariant);
            }
            if (roomProvider == null || roomProvider.getRoomProviderVariants().size() == 0) {
                // No technology variant is available in the room provider
                continue;
            }

            addReport(new SchedulerReportSet.ResourceReport(deviceResource));

            // Add available rooms to room provider
            for (AvailableExecutable<RoomEndpoint> availableExecutable : availableRoomEndpoints) {
                RoomEndpoint roomEndpoint = availableExecutable.getExecutable();

                // Check whether available room is in current device resource
                Long roomEndpointResourceId = roomEndpoint.getResource().getId();
                if (!roomEndpointResourceId.equals(deviceResource.getId())) {
                    continue;
                }

                roomProvider.addAvailableRoomEndpoint(availableExecutable);
            }
            sortAvailableExecutables(roomProvider.getAvailableRoomEndpoints());

            // Add room provider
            for (RoomProviderVariant roomProviderVariant : roomProvider.getRoomProviderVariants()) {
                roomProviderVariants.add(roomProviderVariant);
            }
        }
        if (roomProviderVariants.size() == 0) {
            throw new SchedulerReportSet.ResourceNotFoundException();
        }
        endReport();
        return roomProviderVariants;
    }

    /**
     * @param roomProviderVariants to be sorted by preference
     */
    private void sortRoomProviderVariants(List<RoomProviderVariant> roomProviderVariants)
    {
        addReport(new SchedulerReportSet.SortingResourcesReport());
        Collections.sort(roomProviderVariants, new Comparator<RoomProviderVariant>()
        {
            @Override
            public int compare(RoomProviderVariant first, RoomProviderVariant second)
            {
                RoomProvider firstRoomProvider = first.getRoomProvider();
                RoomProvider secondRoomProvider = second.getRoomProvider();
                int result;
                if (firstRoomProvider != secondRoomProvider) {
                    // Prefer room providers which has some available room reservation(s)
                    boolean firstHasAvailableRoom = firstRoomProvider.getAvailableRoomEndpoints().size() > 0;
                    boolean secondHasAvailableRoom = secondRoomProvider.getAvailableRoomEndpoints().size() > 0;
                    if (!firstHasAvailableRoom && secondHasAvailableRoom) {
                        return 1;
                    }
                    else if (firstHasAvailableRoom && !secondHasAvailableRoom) {
                        return -1;
                    }

                    AvailableRoom firstRoom = firstRoomProvider.getAvailableRoom();
                    AvailableRoom secondRoom = secondRoomProvider.getAvailableRoom();

                    // Prefer already allocated room providers
                    result = -Double.compare(firstRoom.getFullnessRatio(), secondRoom.getFullnessRatio());
                    if (result != 0) {
                        return result;
                    }

                    // Prefer room providers with greater license capacity
                    result = -Double.compare(firstRoom.getMaximumLicenseCount(), secondRoom.getMaximumLicenseCount());
                    if (result != 0) {
                        return result;
                    }
                }

                // Prefer variant with smaller license count
                result = Double.compare(first.getLicenseCount(), second.getLicenseCount());
                if (result != 0) {
                    return result;
                }

                return 0;
            }
        });
    }

    /**
     * Try to allocate given {@code roomProviderVariant}.
     *
     * @param roomProviderVariant to be allocated
     * @return allocated {@link Reservation}
     * @throws SchedulerException when the allocation fails
     */
    private Reservation allocateVariant(RoomProviderVariant roomProviderVariant)
            throws SchedulerException
    {
        RoomProvider roomProvider = roomProviderVariant.getRoomProvider();
        RoomProviderCapability roomProviderCapability = roomProvider.getRoomProviderCapability();
        DeviceResource deviceResource = roomProviderCapability.getDeviceResource();

        beginReport(new SchedulerReportSet.AllocatingResourceReport(deviceResource));
        try {
            // Preferably use available room reservation
            for (AvailableExecutable<RoomEndpoint> availableRoomEndpoint : roomProvider.getAvailableRoomEndpoints()) {
                // Check available room endpoint
                Reservation originalReservation = availableRoomEndpoint.getOriginalReservation();
                RoomEndpoint roomEndpoint = availableRoomEndpoint.getExecutable();
                AvailableReservation<Reservation> availableReservation =
                        availableRoomEndpoint.getAvailableReservation();

                // Only reusable available reservations
                if (!availableReservation.isType(AvailableReservation.Type.REUSABLE)) {
                    continue;
                }

                // Original reservation slot must contain requested slot
                if (!originalReservation.getSlot().contains(slot)) {
                    continue;
                }

                // Check reusable
                if (reusedRoomEndpoint != null) {
                    if (!(roomEndpoint instanceof UsedRoomEndpoint)) {
                        continue;
                    }
                    UsedRoomEndpoint usedRoomEndpoint = (UsedRoomEndpoint) roomEndpoint;
                    if (!reusedRoomEndpoint.equals(usedRoomEndpoint.getReusedRoomEndpoint())) {
                        continue;
                    }
                }

                // Check room configuration
                RoomConfiguration roomConfiguration = roomEndpoint.getRoomConfiguration();
                if (!roomEndpoint.getTechnologies().containsAll(roomProviderVariant.getTechnologies())) {
                    // Room reservation doesn't allocate all technologies
                    continue;
                }
                if (roomConfiguration.getLicenseCount() < roomProviderVariant.getLicenseCount()) {
                    // Room reservation doesn't allocate enough licenses
                    if (availableRoomEndpoint.getType().equals(AvailableReservation.Type.REUSABLE)) {
                        // Reuse available reservation and allocate the missing capacity
                        roomProviderVariant.setReusableRoomEndpoint(availableRoomEndpoint);
                    }
                    continue;
                }

                // TODO: check room settings

                // Available reservation will be returned so remove it from context (to not be used again)
                schedulerContextState.removeAvailableReservation(availableReservation);

                // Create new existing room reservation
                addReport(new SchedulerReportSet.ReservationReusingReport(originalReservation));
                ExistingReservation existingValueReservation = new ExistingReservation();
                existingValueReservation.setSlot(slot);
                existingValueReservation.setReusedReservation(originalReservation);
                return existingValueReservation;
            }

            // Allocate room reservation
            int licenseCount = roomProviderVariant.getLicenseCount();
            Reservation reservation;
            if (licenseCount > 0) {
                // For non-zero license count we must create a RoomReservation
                RoomReservation roomReservation = new RoomReservation();
                roomReservation.setRoomProviderCapability(roomProviderCapability);
                roomReservation.setLicenseCount(licenseCount);
                reservation = roomReservation;
            }
            else {
                // For zero license count we can create just normal Reservation
                reservation = new Reservation();
            }
            reservation.setSlot(slot);

            // Allocated room endpoint
            RoomEndpoint roomEndpoint = null;
            if (allocateRoomEndpoint && schedulerContext.isExecutableAllowed()) {
                // Allocate room endpoint
                roomEndpoint = allocateRoomEndpoint(roomProviderVariant);
                roomEndpoint.setSlot(slot);
                roomEndpoint.setSlotMinutesBefore(slotMinutesBefore);
                roomEndpoint.setSlotMinutesAfter(slotMinutesAfter);
                roomEndpoint.setMeetingName(meetingName);
                roomEndpoint.setMeetingDescription(meetingDescription);
                roomEndpoint.setRoomDescription(schedulerContext.getDescription());
                roomEndpoint.setParticipants(participants);
                roomEndpoint.setParticipantNotificationEnabled(participantNotificationEnabled);

                // Allocate aliases for the room endpoint
                allocateAliases(roomProviderCapability, roomEndpoint);

                // Update room endpoint state
                if (roomEndpoint.getState() == null) {
                    roomEndpoint.setState(ResourceRoomEndpoint.State.NOT_STARTED);
                }
                else if (roomEndpoint.getState().equals(Executable.State.STARTED)) {
                    roomEndpoint.setModified(true);
                }

                // Allocate services (only for rooms which are accessible)
                if (licenseCount > 0) {
                    // Allocate automatic services
                    RecordingService automaticRecordingService = null;
                    if (roomProviderCapability.isRoomRecordable()) {
                        automaticRecordingService = new RecordingService();
                        RecordingCapability recordingCapability =
                                deviceResource.getCapabilityRequired(RecordingCapability.class);
                        automaticRecordingService.setRecordingCapability(recordingCapability);
                        automaticRecordingService.setSlot(slot);
                        automaticRecordingService.setState(ExecutableService.State.NOT_ACTIVE);
                        roomEndpoint.addService(automaticRecordingService);
                    }

                    // For allocating services we must add the room reservation as allocated
                    schedulerContext.getState().addAllocatedReservation(reservation);
                    try {
                        // Allocate requested services
                        for (ExecutableServiceSpecification serviceSpecification : serviceSpecifications) {
                            if (serviceSpecification instanceof RecordingServiceSpecification) {
                                RecordingServiceSpecification recordingServiceSpecification =
                                        (RecordingServiceSpecification) serviceSpecification;
                                if (automaticRecordingService != null) {
                                    // Recording don't have to be allocated
                                    if (recordingServiceSpecification.isEnabled()) {
                                        // Recording should be automatically started
                                        automaticRecordingService.setState(ExecutableService.State.PREPARED);
                                    }
                                }
                                else {
                                    // Recording must be allocated
                                    RecordingServiceReservationTask recordingServiceReservationTask =
                                            recordingServiceSpecification.createReservationTask(schedulerContext,
                                                    this.slot);
                                    recordingServiceReservationTask.setExecutable(roomEndpoint);
                                    addChildReservation(recordingServiceReservationTask);
                                }
                            }
                            else if (serviceSpecification instanceof ReservationTaskProvider) {
                                ReservationTaskProvider reservationTaskProvider =
                                        (ReservationTaskProvider) serviceSpecification;
                                ReservationTask serviceReservationTask =
                                        reservationTaskProvider.createReservationTask(schedulerContext, this.slot);
                                addChildReservation(serviceReservationTask);
                            }
                            else {
                                throw new SchedulerReportSet.SpecificationNotAllocatableException(serviceSpecification);
                            }
                        }
                    }
                    finally {
                        // Remove the room reservation as allocated
                        schedulerContextState.removeAllocatedReservation(reservation);
                    }
                }
            }

            // Set executable to room reservation
            reservation.setExecutable(roomEndpoint);

            // Notify participants
            if (roomEndpoint != null && roomEndpoint.isParticipantNotificationEnabled()) {
                schedulerContextState.addNotification(new RoomNotification.RoomCreated(roomEndpoint));
            }

            endReport();

            return reservation;
        }
        catch (SchedulerException exception) {
            endReportError(exception.getReport());
            throw exception;
        }
    }

    /**
     * Filters given {@code capabilities}. Removes capabilities which are not shared for foreign {@link Domain}, specified in {@code userId}.
     *
     * @param capabilities to filter
     * @param userId which should contain {@link Domain}'s id
     */
    private void filterCapabilitiesByUser(Collection<RoomProviderCapability> capabilities, String userId)
    {
        if (!schedulerContext.isLocalByUser() && capabilities != null && !capabilities.isEmpty() && Controller.isInterDomainInitialized()) {
//            cz.cesnet.shongo.controller.api.Domain domain = new cz.cesnet.shongo.controller.api.Domain();
////            domain.setId();
////            DomainCapabilityListRequest listRequest = new DomainCapabilityListRequest(domain);
////            listRequest.setCapabilityType(DomainCapabilityListRequest.Type.VIRTUAL_ROOM);
////            listRequest.setTechnologyVariants(technologyVariants);

            Long domainId = UserInformation.parseDomainId(userId);
            DomainCapability.Type type = DomainCapability.Type.VIRTUAL_ROOM;
            List<DomainCapability> resources = InterDomainAgent.getInstance().getDomainService().listLocalResourcesByDomain(domainId, type, null, technologyVariants);

            Set<Long> resourcesIds = new HashSet<>();
            for (DomainCapability resource : resources) {
                resourcesIds.add(ObjectIdentifier.parseLocalId(resource.getId(), ObjectType.RESOURCE));
            }

            Iterator<RoomProviderCapability> iterator = capabilities.iterator();
            while (iterator.hasNext()) {
                RoomProviderCapability capability = iterator.next();
                if (!resourcesIds.contains(capability.getResource().getId())) {
                    iterator.remove();
                }
            }
        }
    }

    /**
     * Returns remaining license count for given {@code resource} (must be room provider)
     * by given {@link Domain} (specified in {@code userId}).
     *
     * @param resource for which to look for reservations
     * @param userId which should contain {@link Domain}'s id
     *
     * @return remaining license count or -1 when resource is not assigned to the domain
     */
    private int getRemainingLicenseCount(cz.cesnet.shongo.controller.booking.resource.DeviceResource resource, String userId, Reservation reservation)
    {
        EntityManager entityManager = schedulerContext.getEntityManager();
        ResourceManager resourceManager = new ResourceManager(entityManager);
        ReservationManager reservationManager = new ReservationManager(entityManager);
        try {
            if (!UserInformation.isLocal(userId)) {
                Long reservationId = reservation != null ? reservation.getId() : null;
                Long domainId = UserInformation.parseDomainId(userId);
                DomainResource domainResource = resourceManager.getDomainResource(domainId, resource.getId());
                int availableLicenseCount = domainResource.getLicenseCount();

                List<RoomReservation> reservations = reservationManager.getRoomReservationsForDomain(domainId, resource.getId(), slot, reservationId);
                int usedLicenseCount = schedulerContext.getLicenseCountPeak(slot, reservations, resource.getCapability(RoomProviderCapability.class));

                return availableLicenseCount - usedLicenseCount;
            }
        }
        catch (CommonReportSet.ObjectNotExistsException ex) {
            // when resource is not assigned
            return -1;
        }
        return -1;
    }

    /**
     * Allocate {@link RoomEndpoint} for given {@code roomProviderVariant}.
     *
     * @param roomProviderVariant to be allocated
     * @return (re)allocated {@link RoomEndpoint} or null if no executable should be allocated
     */
    private RoomEndpoint allocateRoomEndpoint(RoomProviderVariant roomProviderVariant)
            throws SchedulerException
    {
        RoomProviderCapability roomProviderCapability = roomProviderVariant.getRoomProviderCapability();
        Long deviceResourceId = roomProviderCapability.getDeviceResource().getId();

        // Room configuration
        RoomConfiguration roomConfiguration = new RoomConfiguration();
        roomConfiguration.setTechnologies(roomProviderVariant.getTechnologies());
        roomConfiguration.setLicenseCount(roomProviderVariant.getLicenseCount());
        for (RoomSetting roomSetting : roomSettings) {
            try {
                roomConfiguration.addRoomSetting(roomSetting.clone());
            }
            catch (CloneNotSupportedException exception) {
                throw new RuntimeException(exception);
            }
        }

        AvailableExecutable<RoomEndpoint> availableRoomEndpoint = roomProviderVariant.getReusableRoomEndpoint();
        // Reuse available room endpoint
        if (availableRoomEndpoint != null) {
            if (!availableRoomEndpoint.getType().equals(AvailableReservation.Type.REUSABLE)) {
                throw new RuntimeException("Available room endpoint should be reusable.");
            }
            Reservation originalReservation = availableRoomEndpoint.getOriginalReservation();
            RoomEndpoint reusedRoomEndpoint = availableRoomEndpoint.getExecutable();
            addReport(new SchedulerReportSet.ReservationReusingReport(originalReservation));

            // Reuse available reservation which allocates the reusable room
            ExistingReservation existingReservation = new ExistingReservation();
            existingReservation.setSlot(slot);
            existingReservation.setReusedReservation(originalReservation);
            addChildReservation(existingReservation);
            schedulerContextState.removeAvailableReservation(availableRoomEndpoint.getAvailableReservation());

            // Reserve only the remaining capacity
            int allocatedLicenseCount = reusedRoomEndpoint.getRoomConfiguration().getLicenseCount();
            int remainingLicenseCount = roomProviderVariant.getLicenseCount() - allocatedLicenseCount;
            roomConfiguration.setLicenseCount(remainingLicenseCount);

            addReport(new SchedulerReportSet.ExecutableReusingReport(reusedRoomEndpoint));

            // Create new used room endpoint
            UsedRoomEndpoint usedRoomEndpoint = new UsedRoomEndpoint();
            usedRoomEndpoint.setReusedRoomEndpoint(reusedRoomEndpoint);
            usedRoomEndpoint.setRoomConfiguration(roomConfiguration);
            return usedRoomEndpoint;
        }
        // Allocate UsedRoomEndpoint
        else if (reusedRoomEndpoint != null) {
            Interval reusedRoomEndpointSlot = reusedRoomEndpoint.getSlot();

            // Check slot
            if (!reusedRoomEndpointSlot.contains(slot)) {
                throw new SchedulerReportSet.ExecutableInvalidSlotException(reusedRoomEndpoint, reusedRoomEndpointSlot);
            }

            // Check availability
            EntityManager entityManager = schedulerContext.getEntityManager();
            ReservationManager reservationManager = new ReservationManager(entityManager);
            List<RoomReservation> reusedRoomEndpointReservations =
                    reservationManager.getRoomReservationsByReusedRoomEndpoint(reusedRoomEndpoint, slot);
            schedulerContextState.applyAvailableReservations(reusedRoomEndpointReservations, RoomReservation.class);
            if (reusedRoomEndpointReservations.size() > 0) {
                RoomReservation roomReservation = reusedRoomEndpointReservations.get(0);
                Interval usageSlot = roomReservation.getSlot();
                Reservation usageReservation = roomReservation.getTopReservation();
                AbstractReservationRequest usageReservationRequest = usageReservation.getReservationRequest();
                throw new SchedulerReportSet.ExecutableAlreadyUsedException(
                        reusedRoomEndpoint, usageReservationRequest, usageSlot);
            }

            addReport(new SchedulerReportSet.ExecutableReusingReport(reusedRoomEndpoint));

            // Allocate new UsedRoomEndpoint
            UsedRoomEndpoint usedRoomEndpoint = new UsedRoomEndpoint();
            usedRoomEndpoint.setReusedRoomEndpoint(reusedRoomEndpoint);
            usedRoomEndpoint.setRoomConfiguration(roomConfiguration);
            return usedRoomEndpoint;
        }
        // Allocate ResourceRoomEndpoint
        else {
            addReport(new SchedulerReportSet.AllocatingExecutableReport());

            // Allocate new ResourceRoomEndpoint
            ResourceRoomEndpoint resourceRoomEndpoint = new ResourceRoomEndpoint();
            resourceRoomEndpoint.setRoomProviderCapability(roomProviderCapability);
            resourceRoomEndpoint.setRoomConfiguration(roomConfiguration);
            return resourceRoomEndpoint;
        }
    }

    /**
     * Allocate {@link Alias}es for given {@code roomEndpoint}.
     *
     * @param roomProviderCapability for which the given {@code roomEndpoint} is allocated
     * @param roomEndpoint           for which the {@link Alias}es should be allocated.
     * @throws SchedulerException when the allocation of {@link Alias}es fails
     */
    private void allocateAliases(RoomProviderCapability roomProviderCapability, RoomEndpoint roomEndpoint)
            throws SchedulerException
    {
        DeviceResource deviceResource = roomProviderCapability.getDeviceResource();

        // Set of technologies which are supported in the room
        Set<Technology> roomTechnologies = roomEndpoint.getTechnologies();

        // Allocate aliases from alias specifications
        for (AliasSpecification aliasSpecification : aliasSpecifications) {
            AliasReservationTask aliasReservationTask =
                    aliasSpecification.createReservationTask(schedulerContext, slot);
            aliasReservationTask.setTargetResource(deviceResource);
            AliasReservation aliasReservation = addChildReservation(aliasReservationTask, AliasReservation.class);
            // Assign allocated aliases to the room
            for (Alias alias : aliasReservation.getAliases()) {
                // Assign only aliases which can be assigned to the room (according to room technologies)
                Technology aliasTechnology = alias.getTechnology();
                if (aliasTechnology.isCompatibleWith(roomTechnologies)) {
                    roomEndpoint.addAssignedAlias(alias);
                }
            }
        }

        // Allocate aliases for the room
        // Set of alias types which should be supported in the room
        Set<AliasType> roomAliasTypes = roomProviderCapability.getRequiredAliasTypes();
        // Remove all aliases which should be supported but which aren't technology compatible with current
        // room configuration
        Iterator<AliasType> iterator = roomAliasTypes.iterator();
        while (iterator.hasNext()) {
            if (!iterator.next().getTechnology().isCompatibleWith(roomTechnologies)) {
                iterator.remove();
            }
        }

        // Build set of technologies and alias types which are missing (by adding all and removing all
        // for which the room already has an alias).
        Set<AliasType> missingAliasTypes = new HashSet<AliasType>();
        missingAliasTypes.addAll(roomAliasTypes);
        for (Alias alias : roomEndpoint.getAliases()) {
            missingAliasTypes.remove(alias.getType());
        }

        // Allocate aliases for alias types which are missing
        while (missingAliasTypes.size() > 0) {
            // Allocate missing alias
            AliasType missingAliasType = missingAliasTypes.iterator().next();
            AliasReservationTask aliasReservationTask = new AliasReservationTask(schedulerContext, slot);
            aliasReservationTask.addAliasType(missingAliasType);
            aliasReservationTask.setTargetResource(deviceResource);
            AliasReservation aliasReservation =
                    addChildReservation(aliasReservationTask, AliasReservation.class);

            // Assign allocated aliases to the room
            for (Alias alias : aliasReservation.getAliases()) {
                // Assign only aliases which can be assigned to the room (according to room technologies)
                Technology aliasTechnology = alias.getTechnology();
                if (aliasTechnology.isCompatibleWith(roomTechnologies)) {
                    roomEndpoint.addAssignedAlias(alias);
                    missingAliasTypes.remove(alias.getType());
                }
            }
        }
    }

    /**
     * Represents {@link RoomProviderCapability} which can be allocated by the {@link RoomReservationTask}.
     */
    private static class RoomProvider
    {
        /**
         * @see RoomProviderCapability
         */
        private RoomProviderCapability roomProviderCapability;

        /**
         * {@link AvailableRoom} for the {@link #roomProviderCapability}.
         */
        private AvailableRoom availableRoom;

        /**
         * Possible {@link RoomProviderVariant}s.
         */
        private List<RoomProviderVariant> roomProviderVariants = new LinkedList<RoomProviderVariant>();

        /**
         * Available {@link RoomEndpoint}s which can be reused/reallocated.
         */
        private List<AvailableExecutable<RoomEndpoint>> availableRoomEndpoints =
                new LinkedList<AvailableExecutable<RoomEndpoint>>();

        /**
         * Constructor.
         *
         * @param roomProviderCapability sets the {@link #roomProviderCapability}
         * @param availableRoom          sets the {@link #availableRoom}
         */
        public RoomProvider(RoomProviderCapability roomProviderCapability, AvailableRoom availableRoom)
        {
            this.roomProviderCapability = roomProviderCapability;
            this.availableRoom = availableRoom;
        }

        /**
         * @return {@link #roomProviderCapability}
         */
        private RoomProviderCapability getRoomProviderCapability()
        {
            return roomProviderCapability;
        }

        /**
         * @return {@link #availableRoom}
         */
        private AvailableRoom getAvailableRoom()
        {
            return availableRoom;
        }

        /**
         * @param roomProviderVariant to be added to the {@link #roomProviderVariants}
         */
        public void addRoomProviderVariant(RoomProviderVariant roomProviderVariant)
        {
            roomProviderVariants.add(roomProviderVariant);
        }

        /**
         * @return {@link #roomProviderVariants}
         */
        public List<RoomProviderVariant> getRoomProviderVariants()
        {
            return roomProviderVariants;
        }

        /**
         * @return {@link #availableRoomEndpoints}
         */
        public List<AvailableExecutable<RoomEndpoint>> getAvailableRoomEndpoints()
        {
            return availableRoomEndpoints;
        }

        /**
         * @param availableRoomEndpoint to be added to the {@link #availableRoomEndpoints}
         */
        public void addAvailableRoomEndpoint(AvailableExecutable<RoomEndpoint> availableRoomEndpoint)
        {
            availableRoomEndpoints.add(availableRoomEndpoint);
        }
    }

    /**
     * Represents a variant for allocating a {@link RoomReservation}.
     */
    public class RoomProviderVariant
    {
        private RoomProvider roomProvider;

        /**
         * Set of {@link Technology}s which must be supported by the {@link RoomReservation}.
         */
        private Set<Technology> technologies;

        /**
         * Number of licenses.
         */
        private int licenseCount;

        /**
         * Reusable {@link RoomEndpoint} which doesn't allocate the whole requested capacity
         * but the missing capacity can be additionally allocated.
         */
        private AvailableExecutable<RoomEndpoint> reusableRoomEndpoint = null;

        /**
         * Constructor.
         *
         * @param roomProvider for which the variant is
         * @param technologies set of {@link Technology}s
         */
        public RoomProviderVariant(RoomProvider roomProvider, Set<Technology> technologies)
        {
            this.roomProvider = roomProvider;
            this.technologies = technologies;
            this.licenseCount = 0;
        }

        /**
         * Constructor.
         *
         * @param roomProvider     for which the variant is
         * @param participantCount number of participants for the {@link RoomReservation}
         * @param technologies     set of {@link Technology}s
         */
        public RoomProviderVariant(RoomProvider roomProvider, int participantCount,
                Set<Technology> technologies)
        {
            this.roomProvider = roomProvider;
            this.technologies = technologies;
            this.licenseCount = computeLicenseCount(participantCount, technologies);
        }

        public RoomProvider getRoomProvider()
        {
            return roomProvider;
        }

        /**
         * @return {@link #technologies}
         */
        public Set<Technology> getTechnologies()
        {
            return technologies;
        }

        /**
         * @return {@link #licenseCount}
         */
        public int getLicenseCount()
        {
            return licenseCount;
        }

        /**
         * @return {@link #reusableRoomEndpoint}
         */
        public AvailableExecutable<RoomEndpoint> getReusableRoomEndpoint()
        {
            return reusableRoomEndpoint;
        }

        /**
         * @param reusableRoomEndpoint sets the {@link #reusableRoomEndpoint}
         */
        public void setReusableRoomEndpoint(AvailableExecutable<RoomEndpoint> reusableRoomEndpoint)
        {
            if (this.reusableRoomEndpoint == null) {
                this.reusableRoomEndpoint = reusableRoomEndpoint;
            }
            else {
                // Reuse available reservation with greater license count
                int newLicenseCount = reusableRoomEndpoint.getExecutable().getRoomConfiguration().getLicenseCount();
                int existingLicenseCount =
                        this.reusableRoomEndpoint.getExecutable().getRoomConfiguration().getLicenseCount();

                if (newLicenseCount < existingLicenseCount) {
                    this.reusableRoomEndpoint = reusableRoomEndpoint;
                }
            }
        }

        /**
         * @return {@link #roomProvider#getRoomProviderCapability()}
         */
        public RoomProviderCapability getRoomProviderCapability()
        {
            return roomProvider.getRoomProviderCapability();
        }

        /**
         * @param participantCount
         * @param technologies
         * @return number of licenses for given {@code participantCount} and {@code technologies}
         *         in {@link DeviceResource} with given {@code deviceResourceId}
         */
        private int computeLicenseCount(int participantCount, Set<Technology> technologies)
        {
            return participantCount;
        }
    }
}
