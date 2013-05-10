package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.cache.Cache;
import cz.cesnet.shongo.controller.cache.ResourceCache;
import cz.cesnet.shongo.controller.common.RoomConfiguration;
import cz.cesnet.shongo.controller.common.RoomSetting;
import cz.cesnet.shongo.controller.executor.ResourceRoomEndpoint;
import cz.cesnet.shongo.controller.executor.RoomEndpoint;
import cz.cesnet.shongo.controller.executor.UsedRoomEndpoint;
import cz.cesnet.shongo.controller.request.AliasSpecification;
import cz.cesnet.shongo.controller.reservation.AliasReservation;
import cz.cesnet.shongo.controller.reservation.ExistingReservation;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.reservation.RoomReservation;
import cz.cesnet.shongo.controller.resource.Alias;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.resource.RoomProviderCapability;
import org.joda.time.Interval;

import java.util.*;

/**
 * Represents {@link ReservationTask} for a {@link RoomReservation}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class RoomReservationTask extends ReservationTask
{
    /**
     * Number of participants in the virtual room.
     */
    private final int participantCount;

    /**
     * Collection of {@link Technology} set variants where at least one must be supported by
     * allocated {@link RoomReservation}.
     */
    private List<Set<Technology>> technologyVariants = new LinkedList<Set<Technology>>();

    /**
     * Collection of {@link RoomSetting} for allocated {@link RoomReservation}.
     */
    private List<RoomSetting> roomSettings = new LinkedList<RoomSetting>();

    /**
     * Collection of {@link AliasSpecification} for {@link Alias}es which shoudl be allocated for the room.
     */
    private List<AliasSpecification> aliasSpecifications = new LinkedList<AliasSpecification>();

    /**
     * {@link DeviceResource} with {@link RoomProviderCapability} for which the {@link RoomReservation}
     * should be allocated.
     */
    private DeviceResource deviceResource = null;

    /**
     * Constructor.
     *
     * @param schedulerContext sets the {@link #schedulerContext}
     */
    public RoomReservationTask(SchedulerContext schedulerContext, int participantCount)
    {
        super(schedulerContext);
        this.participantCount = participantCount;
    }

    /**
     * @param technologies to be added to the {@link #technologyVariants}
     */
    public void addTechnologyVariant(Set<Technology> technologies)
    {
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
     * @param deviceResource sets the {@link #deviceResource}
     */
    public void setDeviceResource(DeviceResource deviceResource)
    {
        this.deviceResource = deviceResource;
    }

    @Override
    protected SchedulerReport createMainReport()
    {
        List<TechnologySet> technologySets = new LinkedList<TechnologySet>();
        for (Set<Technology> technologies : technologyVariants) {
            technologySets.add(new TechnologySet(technologies));
        }
        return new SchedulerReportSet.AllocatingRoomReport(technologySets, participantCount);
    }

    @Override
    protected Reservation allocateReservation(Reservation allocatedReservation) throws SchedulerException
    {
        SchedulerContext schedulerContext = getSchedulerContext();
        Cache cache = getCache();
        ResourceCache resourceCache = cache.getResourceCache();
        Interval interval = getInterval();

        // Check maximum duration
        if (schedulerContext.isMaximumFutureAndDurationRestricted()) {
            checkMaximumDuration(interval, cache.getRoomReservationMaximumDuration());
        }

        // Get possible room providers
        Collection<RoomProviderCapability> roomProviderCapabilities;
        if (deviceResource != null) {
            // Use only specified room provider
            roomProviderCapabilities = new LinkedList<RoomProviderCapability>();
            roomProviderCapabilities.add(deviceResource.getCapability(RoomProviderCapability.class));
        }
        else {
            // Use all room providers from the cache
            roomProviderCapabilities = cache.getRoomProviders();
        }

        // Find matching room provider variants
        beginReport(new SchedulerReportSet.FindingAvailableResourceReport());
        List<RoomProviderVariant> roomProviderVariants = new LinkedList<RoomProviderVariant>();
        for (RoomProviderCapability roomProviderCapability : roomProviderCapabilities) {
            DeviceResource deviceResource = roomProviderCapability.getDeviceResource();

            // Initialize room provider
            RoomProvider roomProvider = null;
            for (Set<Technology> technologies : technologyVariants) {
                if (!deviceResource.hasTechnologies(technologies)) {
                    continue;
                }
                AvailableRoom availableRoom;
                if (roomProvider == null) {
                    availableRoom = schedulerContext.getAvailableRoom(roomProviderCapability);
                    roomProvider = new RoomProvider(roomProviderCapability, availableRoom);
                }
                else {
                    availableRoom = roomProvider.getAvailableRoom();
                }
                RoomProviderVariant roomProviderVariant =
                        new RoomProviderVariant(roomProvider, participantCount, technologies);
                if (availableRoom.getAvailableLicenseCount() < roomProviderVariant.getLicenseCount()) {
                    continue;
                }
                roomProvider.addRoomProviderVariant(roomProviderVariant);
            }
            if (roomProvider == null) {
                continue;
            }

            addReport(new SchedulerReportSet.ResourceReport(deviceResource));

            // Add available room reservations to room provider
            for (AvailableReservation<RoomReservation> availableRoomReservation :
                    schedulerContext.getAvailableRoomReservations(roomProviderCapability)) {
                if (allocatedReservation != null) {
                    if (availableRoomReservation.getOriginalReservation().equals(allocatedReservation)) {
                        roomProvider.setAllocated(true);
                    }
                }
                roomProvider.addAvailableRoomReservation(availableRoomReservation);
            }
            sortAvailableReservations(roomProvider.getAvailableRoomReservations(), allocatedReservation);

            // Add room provider
            for (RoomProviderVariant roomProviderVariant : roomProvider.getRoomProviderVariants()) {
                roomProviderVariants.add(roomProviderVariant);
            }
        }
        if (roomProviderVariants.size() == 0) {
            throw new SchedulerException(getCurrentReport());
        }
        endReport();

        // Sort room provider variants
        addReport(new SchedulerReportSet.SortingResourcesReport());
        Collections.sort(roomProviderVariants, new Comparator<RoomProviderVariant>()
        {
            @Override
            public int compare(RoomProviderVariant first, RoomProviderVariant second)
            {
                RoomProvider firstRoomProvider = first.getRoomProvider();
                RoomProvider secondRoomProvider = second.getRoomProvider();

                if (firstRoomProvider != secondRoomProvider) {
                    // Prefer room provider which is already allocated by allocatedReservation
                    if (secondRoomProvider.isAllocated()) {
                        return 1;
                    }

                    // Prefer room providers which has reallocatable available room reservation(s)
                    boolean firstReallocatable = firstRoomProvider.hasReallocatableAvailableRoomReservation();
                    boolean secondReallocatable = secondRoomProvider.hasReallocatableAvailableRoomReservation();
                    if (!firstReallocatable && secondReallocatable) {
                        return 1;
                    }

                    // Prefer room providers which has some available room reservation(s)
                    boolean firstHasAvailableRoom = firstRoomProvider.getAvailableRoomReservations().size() > 0;
                    boolean secondHasAvailableRoom = secondRoomProvider.getAvailableRoomReservations().size() > 0;
                    if (!firstHasAvailableRoom && secondHasAvailableRoom) {
                        return 1;
                    }

                    AvailableRoom firstRoom = firstRoomProvider.getAvailableRoom();
                    AvailableRoom secondRoom = firstRoomProvider.getAvailableRoom();

                    // Prefer already allocated room providers
                    if (firstRoom.getFullnessRatio() < secondRoom.getFullnessRatio()) {
                        return 1;
                    }

                    // Prefer room providers with greater license capacity
                    if (firstRoom.getMaximumLicenseCount() < secondRoom.getMaximumLicenseCount()) {
                        return 1;
                    }
                }

                // Prefer variant with smaller license count
                if (first.getLicenseCount() > second.getLicenseCount()) {
                    return 1;
                }

                return 0;
            }
        });

        // TODO: continue reworking
        if (true) {
            throw new TodoImplementException();
        }

        // Allocate room reservation in some matching room provider variant
        for (RoomProviderVariant roomProviderVariant : roomProviderVariants) {
            RoomProvider roomProvider = roomProviderVariant.getRoomProvider();
            RoomProviderCapability roomProviderCapability = roomProvider.getRoomProviderCapability();
            DeviceResource deviceResource = roomProviderCapability.getDeviceResource();
            beginReport(new SchedulerReportSet.AllocatingResourceReport(deviceResource));
            SchedulerContext.Savepoint schedulerContextSavepoint = schedulerContext.createSavepoint();
            try {
                // Preferably use available room reservation
                for (AvailableReservation<RoomReservation> availableAliasReservation :
                        roomProvider.getAvailableRoomReservations()) {
                    // Check available room reservation
                    Reservation originalReservation = availableAliasReservation.getOriginalReservation();

                    // Original reservation slot must contain requested slot
                    if (!originalReservation.getSlot().contains(interval)) {
                        // Original reservation slot doesn't contain the requested
                        continue;
                    }

                    // Available reservation will be returned so remove it from context (to not be used again)
                    schedulerContext.removeAvailableReservation(availableAliasReservation);

                    // Return available reservation
                    if (availableAliasReservation.isExistingReservationRequired()) {
                        addReport(new SchedulerReportSet.ReservationReusingReport(originalReservation));
                        ExistingReservation existingReservation = new ExistingReservation();
                        existingReservation.setSlot(interval);
                        existingReservation.setReservation(originalReservation);
                        schedulerContext.removeAvailableReservation(availableAliasReservation);
                        return existingReservation;
                    }
                    else {
                        addReport(new SchedulerReportSet.ReservationReallocatingReport(originalReservation));
                        return originalReservation;
                    }
                }

                // Check whether alias provider can be allocated
                try {
                    resourceCache.checkCapabilityAvailable(roomProviderCapability, schedulerContext);
                }
                catch (SchedulerException exception) {
                    endReportError(exception.getReport());
                    continue;
                }

                throw new SchedulerException(new TodoImplementException());

                // Create room reservation
                /*RoomReservation roomReservation = new RoomReservation();
                roomReservation.setSlot(interval);
                roomReservation.setRoomProviderCapability(roomProviderCapability);

                // Room configuration
                RoomConfiguration roomConfiguration = new RoomConfiguration();
                roomConfiguration.setTechnologies(roomProviderVariant.getTechnologies());
                for (RoomSetting roomSetting : roomSettings) {
                    roomConfiguration.addRoomSetting(roomSetting.clone());
                }

                // Allocated room endpoint
                RoomEndpoint roomEndpoint = null;

                // If provided room is available
                ResourceRoomEndpoint availableProvidedRoom = availableProvidedRooms.get(roomProviderCapability.getId());
                if (availableProvidedRoom != null) {
                    AvailableReservation<Reservation> availableReservation =
                            schedulerContext.getAvailableReservationByExecutable(availableProvidedRoom);
                    Reservation originalReservation = availableReservation.getOriginalReservation();
                    if (availableReservation.isExistingReservationRequired()) {
                        addReport(new SchedulerReportSet.ReservationReusingReport(originalReservation));

                        // Reuse provided reservation which allocates the provided room
                        ExistingReservation existingReservation = new ExistingReservation();
                        existingReservation.setSlot(interval);
                        existingReservation.setReservation(originalReservation);
                        addChildReservation(existingReservation);
                        schedulerContext.removeAvailableReservation(availableReservation);

                        // Reserve only the remaining capacity
                        roomConfiguration.setLicenseCount(
                                roomVariant.getLicenseCount() - availableProvidedRoom.getLicenseCount());

                        if (schedulerContext.isExecutableAllowed()) {
                            addReport(new SchedulerReportSet.ExecutableReusingReport(availableProvidedRoom));

                            // Create new used room endpoint
                            UsedRoomEndpoint usedRoomEndpoint = new UsedRoomEndpoint();
                            usedRoomEndpoint.setRoomEndpoint(availableProvidedRoom);
                            roomEndpoint = usedRoomEndpoint;
                        }
                    }
                    else {
                        throw new TodoImplementException("reallocate room");
                    }
                }
                else {
                    // Reserve full capacity
                    roomConfiguration.setLicenseCount(roomVariant.getLicenseCount());

                    if (schedulerContext.isExecutableAllowed()) {
                        addReport(new SchedulerReportSet.AllocatingExecutableReport());

                        // Create new room endpoint  \
                        ResourceRoomEndpoint resourceRoomEndpoint = new ResourceRoomEndpoint();
                        resourceRoomEndpoint.setRoomProviderCapability(roomProviderCapability);
                        roomEndpoint = resourceRoomEndpoint;
                    }
                }

                // Setup room endpoint
                if (schedulerContext.isExecutableAllowed()) {
                    // Set of technologies which are supported in the room
                    Set<Technology> roomTechnologies = roomConfiguration.getTechnologies();

                    // Allocate aliases from alias specifications
                    for (AliasSpecification aliasSpecification : aliasSpecifications) {
                        AliasReservationTask aliasReservationTask = aliasSpecification
                                .createReservationTask(schedulerContext);
                        aliasReservationTask.setTargetResource(deviceResource);
                        AliasReservation aliasReservation =
                                addChildReservation(aliasReservationTask, AliasReservation.class);
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
                    RoomProviderCapability roomProviderCapability =
                            deviceResource.getCapability(RoomProviderCapability.class);
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
                    Set<Technology> missingAliasTechnologies = new HashSet<Technology>();
                    Set<AliasType> missingAliasTypes = new HashSet<AliasType>();
                    missingAliasTechnologies.addAll(roomTechnologies);
                    missingAliasTypes.addAll(roomAliasTypes);
                    for (Alias alias : roomEndpoint.getAliases()) {
                        missingAliasTechnologies.remove(alias.getTechnology());
                        missingAliasTypes.remove(alias.getType());
                    }

                    // Allocate aliases for alias types which are missing
                    while (missingAliasTypes.size() > 0) {
                        // Allocate missing alias
                        AliasType missingAliasType = missingAliasTypes.iterator().next();
                        AliasReservationTask aliasReservationTask = new AliasReservationTask(getSchedulerContext());
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
                                missingAliasTechnologies.remove(aliasTechnology);
                                missingAliasTypes.remove(alias.getType());
                            }
                        }
                    }

                    // Setup abstract room endpoint
                    roomEndpoint.setSlot(interval);
                    roomEndpoint.setRoomDescription(schedulerContext.getReservationDescription());
                    roomEndpoint.setRoomConfiguration(roomConfiguration);
                    roomEndpoint.setState(ResourceRoomEndpoint.State.NOT_STARTED);
                }

                // Set room configuration and executable
                roomReservation.setRoomConfiguration(roomConfiguration);
                roomReservation.setExecutable(roomEndpoint);

                return roomReservation;*/
            }
            catch (SchedulerException exception) {
                schedulerContextSavepoint.revert();

                // Try to allocate next available room
                continue;
            }
            finally {
                schedulerContextSavepoint.destroy();
                endReport();
            }
        }
        throw new SchedulerException(getCurrentReport());
    }

    /**
     * Represents {@link RoomProviderCapability} which can be used allocated by the {@link RoomReservationTask}.
     */
    private static class RoomProvider
    {
        private RoomProviderCapability roomProviderCapability;

        private AvailableRoom availableRoom;

        private List<RoomProviderVariant> roomProviderVariants = new LinkedList<RoomProviderVariant>();

        private List<AvailableReservation<RoomReservation>> availableRoomReservations =
                new LinkedList<AvailableReservation<RoomReservation>>();

        private boolean allocated = false;

        private boolean reallocatableAvailableRoomReservation = false;

        public RoomProvider(RoomProviderCapability roomProviderCapability, AvailableRoom availableRoom)
        {
            this.roomProviderCapability = roomProviderCapability;
            this.availableRoom = availableRoom;
        }

        private RoomProviderCapability getRoomProviderCapability()
        {
            return roomProviderCapability;
        }

        private AvailableRoom getAvailableRoom()
        {
            return availableRoom;
        }

        public void addRoomProviderVariant(RoomProviderVariant roomProviderVariant)
        {
            roomProviderVariants.add(roomProviderVariant);
        }

        public List<RoomProviderVariant> getRoomProviderVariants()
        {
            return roomProviderVariants;
        }

        public List<AvailableReservation<RoomReservation>> getAvailableRoomReservations()
        {
            return availableRoomReservations;
        }

        public void addAvailableRoomReservation(AvailableReservation<RoomReservation> availableRoomReservation)
        {
            availableRoomReservations.add(availableRoomReservation);
            if (availableRoomReservation.getType().equals(AvailableReservation.Type.REALLOCATABLE)) {
                reallocatableAvailableRoomReservation = true;
            }
        }

        private boolean isAllocated()
        {
            return allocated;
        }

        private void setAllocated(boolean allocated)
        {
            this.allocated = allocated;
        }

        public boolean hasReallocatableAvailableRoomReservation()
        {
            return reallocatableAvailableRoomReservation;
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
