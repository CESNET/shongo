package cz.cesnet.shongo.controller.booking.room;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.booking.TechnologySet;
import cz.cesnet.shongo.controller.booking.alias.Alias;
import cz.cesnet.shongo.controller.booking.alias.AliasReservation;
import cz.cesnet.shongo.controller.booking.alias.AliasReservationTask;
import cz.cesnet.shongo.controller.booking.alias.AliasSpecification;
import cz.cesnet.shongo.controller.booking.executable.Executable;
import cz.cesnet.shongo.controller.booking.executable.ExecutableService;
import cz.cesnet.shongo.controller.booking.participant.AbstractParticipant;
import cz.cesnet.shongo.controller.booking.recording.RecordingCapability;
import cz.cesnet.shongo.controller.booking.recording.RecordingService;
import cz.cesnet.shongo.controller.booking.recording.RecordingServiceReservationTask;
import cz.cesnet.shongo.controller.booking.recording.RecordingServiceSpecification;
import cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.booking.reservation.ExistingReservation;
import cz.cesnet.shongo.controller.booking.reservation.Reservation;
import cz.cesnet.shongo.controller.booking.reservation.ReservationManager;
import cz.cesnet.shongo.controller.booking.resource.DeviceResource;
import cz.cesnet.shongo.controller.booking.room.settting.RoomSetting;
import cz.cesnet.shongo.controller.booking.specification.ExecutableServiceSpecification;
import cz.cesnet.shongo.controller.cache.Cache;
import cz.cesnet.shongo.controller.cache.ResourceCache;
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
            throw new IllegalArgumentException("Technologies cannot be empty.");
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
    protected Reservation allocateReservation() throws SchedulerException
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
        List<RoomProviderVariant> roomProviderVariants = getRoomProviderVariants();

        // Sort room provider variants
        sortRoomProviderVariants(roomProviderVariants);

        // Try to allocate room reservation in room provider variants
        for (RoomProviderVariant roomProviderVariant : roomProviderVariants) {
            SchedulerContextState.Savepoint schedulerContextSavepoint = schedulerContextState.createSavepoint();
            try {
                Reservation reservation = allocateVariant(roomProviderVariant);
                if (reservation != null) {
                    return reservation;
                }
            }
            catch (SchedulerException exception) {
                schedulerContextSavepoint.revert();
            }
            finally {
                schedulerContextSavepoint.destroy();
            }
        }
        throw new SchedulerException(getCurrentReport());
    }

    @Override
    public void migrateReservation(Reservation oldReservation, Reservation newReservation, EntityManager entityManager)
            throws SchedulerException
    {
        Executable oldExecutable = oldReservation.getExecutable();
        Executable newExecutable = newReservation.getExecutable();

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
    private List<RoomProviderVariant> getRoomProviderVariants()
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
                    int requestedLicenseCount = roomProviderVariant.getLicenseCount();
                    if (availableLicenseCount < requestedLicenseCount) {
                        addReport(new SchedulerReportSet.ResourceRoomCapacityExceededReport(
                                deviceResource, availableLicenseCount, availableRoom.getMaximumLicenseCount()));
                        continue;
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
            RoomEndpoint oldRoomEndpoint = null;
            RoomEndpoint roomEndpoint = null;
            if (allocateRoomEndpoint && schedulerContext.isExecutableAllowed()) {
                // Allocate room endpoint
                roomEndpoint = allocateRoomEndpoint(roomProviderVariant, oldRoomEndpoint);
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
            // Stop old room endpoint
            if (oldRoomEndpoint != null && oldRoomEndpoint.getState().isStarted()) {
                // If room endpoint exists it will be stopped
                oldRoomEndpoint.setSlotEnd(DateTime.now());
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
     * Allocate {@link RoomEndpoint} for given {@code roomProviderVariant}.
     *
     * @param roomProviderVariant to be allocated
     * @param oldRoomEndpoint     which is already allocated and which can be reallocated
     * @return (re)allocated {@link RoomEndpoint} or null if no executable should be allocated
     */
    private RoomEndpoint allocateRoomEndpoint(RoomProviderVariant roomProviderVariant, RoomEndpoint oldRoomEndpoint)
            throws SchedulerException
    {
        RoomProviderCapability roomProviderCapability = roomProviderVariant.getRoomProviderCapability();
        Long deviceResourceId = roomProviderCapability.getDeviceResource().getId();

        // Room configuration
        RoomConfiguration roomConfiguration = new RoomConfiguration();
        roomConfiguration.setTechnologies(roomProviderVariant.getTechnologies());
        roomConfiguration.setLicenseCount(roomProviderVariant.getLicenseCount());
        for (RoomSetting roomSetting : roomSettings) {
            roomConfiguration.addRoomSetting(roomSetting.clone());
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

            // Allocate room endpoint
            if (oldRoomEndpoint != null && oldRoomEndpoint instanceof UsedRoomEndpoint) {
                UsedRoomEndpoint usedRoomEndpoint = (UsedRoomEndpoint) oldRoomEndpoint;
                if (deviceResourceId.equals(usedRoomEndpoint.getResource().getId())) {
                    // Reallocate existing room endpoint
                    usedRoomEndpoint.clearAssignedAliases();
                    return usedRoomEndpoint;
                }
                else {
                    throw new TodoImplementException("Schedule room migration.");
                }
            }
            else {
                // Create new used room endpoint
                UsedRoomEndpoint usedRoomEndpoint = new UsedRoomEndpoint();
                usedRoomEndpoint.setReusedRoomEndpoint(reusedRoomEndpoint);
                usedRoomEndpoint.setRoomConfiguration(roomConfiguration);
                return usedRoomEndpoint;
            }
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

            if (oldRoomEndpoint != null && oldRoomEndpoint instanceof ResourceRoomEndpoint) {
                // Reallocate ResourceRoomEndpoint
                ResourceRoomEndpoint resourceRoomEndpoint = (ResourceRoomEndpoint) oldRoomEndpoint;
                if (deviceResourceId.equals(resourceRoomEndpoint.getResource().getId())) {
                    // Reallocate existing room endpoint
                    resourceRoomEndpoint.clearAssignedAliases();
                    return resourceRoomEndpoint;
                }
                else {
                    throw new TodoImplementException("Schedule room migration.");
                }
            }
            else {
                // Reallocate new ResourceRoomEndpoint
                ResourceRoomEndpoint resourceRoomEndpoint = new ResourceRoomEndpoint();
                resourceRoomEndpoint.setRoomProviderCapability(roomProviderCapability);
                resourceRoomEndpoint.setRoomConfiguration(roomConfiguration);
                return resourceRoomEndpoint;
            }
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
        for (AliasType aliasType : roomAliasTypes) {
            if (!aliasType.getTechnology().isCompatibleWith(roomTechnologies)) {
                roomAliasTypes.remove(aliasType);
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
