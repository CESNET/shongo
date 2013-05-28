package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.common.RoomConfiguration;
import cz.cesnet.shongo.controller.common.RoomSetting;
import cz.cesnet.shongo.controller.executor.*;
import cz.cesnet.shongo.controller.request.AliasSpecification;
import cz.cesnet.shongo.controller.reservation.AliasReservation;
import cz.cesnet.shongo.controller.reservation.ExistingReservation;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.reservation.RoomReservation;
import cz.cesnet.shongo.controller.resource.Alias;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.resource.RoomProviderCapability;
import org.joda.time.DateTime;
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
    protected Reservation allocateReservation() throws SchedulerException
    {
        // Check maximum duration
        if (schedulerContext.isMaximumFutureAndDurationRestricted()) {
            checkMaximumDuration(getInterval(), getCache().getRoomReservationMaximumDuration());
        }

        // Find room provider variants
        List<RoomProviderVariant> roomProviderVariants = getRoomProviderVariants();

        // Sort room provider variants
        sortRoomProviderVariants(roomProviderVariants);

        // Try to allocate room reservation in room provider variants
        for (RoomProviderVariant roomProviderVariant : roomProviderVariants) {
            SchedulerContext.Savepoint schedulerContextSavepoint = schedulerContext.createSavepoint();
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
    public void migrateReservation(Reservation oldReservation, Reservation newReservation) throws SchedulerException
    {
        Executable oldExecutable = oldReservation.getExecutable();
        Executable newExecutable = newReservation.getExecutable();
        if (oldExecutable instanceof ResourceRoomEndpoint && newExecutable instanceof ResourceRoomEndpoint) {
            Migration migration = new Migration();
            migration.setSourceExecutable(oldExecutable);
            migration.setTargetExecutable(newExecutable);
        }
        super.migrateReservation(oldReservation, newReservation);
    }

    /**
     * @return list of possible {@link RoomProviderVariant}s
     * @throws SchedulerException when none {@link RoomProviderVariant} is found
     */
    private List<RoomProviderVariant> getRoomProviderVariants()
            throws SchedulerException
    {
        // Get possible room providers
        Collection<RoomProviderCapability> roomProviderCapabilities;
        if (deviceResource != null) {
            // Use only specified room provider
            roomProviderCapabilities = new LinkedList<RoomProviderCapability>();
            roomProviderCapabilities.add(deviceResource.getCapability(RoomProviderCapability.class));
        }
        else {
            // Use all room providers from the cache
            roomProviderCapabilities = getCache().getRoomProviders();
        }

        // Available room endpoints
        Collection<AvailableExecutable<RoomEndpoint>> availableRoomEndpoints =
                schedulerContext.getAvailableExecutables(RoomEndpoint.class);

        // Find all matching room provider variants
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
                // No technology variant is available in the room provider
                continue;
            }

            addReport(new SchedulerReportSet.ResourceReport(deviceResource));

            // Add available rooms to room provider
            for (AvailableExecutable<RoomEndpoint> availableExecutable : availableRoomEndpoints) {
                RoomEndpoint roomEndpoint = availableExecutable.getExecutable();

                // Check whether available room is in current device resource
                Long roomEndpointResourceId;
                if (roomEndpoint instanceof ResourceRoomEndpoint) {
                    roomEndpointResourceId = ((ResourceRoomEndpoint) roomEndpoint).getResource().getId();
                }
                else if (roomEndpoint instanceof UsedRoomEndpoint) {
                    roomEndpointResourceId = ((UsedRoomEndpoint) roomEndpoint).getResource().getId();
                }
                else {
                    throw new TodoImplementException(roomEndpoint.getClass().getName());
                }
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
            throw new SchedulerException(getCurrentReport());
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

                if (firstRoomProvider != secondRoomProvider) {
                    // Prefer room providers which has some available room reservation(s)
                    boolean firstHasAvailableRoom = firstRoomProvider.getAvailableRoomEndpoints().size() > 0;
                    boolean secondHasAvailableRoom = secondRoomProvider.getAvailableRoomEndpoints().size() > 0;
                    if (!firstHasAvailableRoom && secondHasAvailableRoom) {
                        return 1;
                    }

                    AvailableRoom firstRoom = firstRoomProvider.getAvailableRoom();
                    AvailableRoom secondRoom = secondRoomProvider.getAvailableRoom();

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
        Interval interval = getInterval();
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
                if (!originalReservation.getSlot().contains(interval)) {
                    continue;
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
                schedulerContext.removeAvailableReservation(availableReservation);

                // Create new existing room reservation
                addReport(new SchedulerReportSet.ReservationReusingReport(originalReservation));
                ExistingReservation existingValueReservation = new ExistingReservation();
                existingValueReservation.setSlot(interval);
                existingValueReservation.setReservation(originalReservation);
                return existingValueReservation;

            }

            // Check whether room provider can be allocated
            getCache().getResourceCache().checkCapabilityAvailable(roomProviderCapability, schedulerContext);

            // Allocate room reservation
            RoomReservation roomReservation = new RoomReservation();

            // Allocated room endpoint
            RoomConfiguration roomConfiguration = new RoomConfiguration();
            RoomEndpoint roomEndpoint = roomReservation.getEndpoint();
            roomEndpoint = allocateRoomEndpoint(roomProviderVariant, roomConfiguration, roomEndpoint);
            if (roomEndpoint != null) {
                // Setup room endpoint
                roomEndpoint.setSlot(interval);
                roomEndpoint.setRoomDescription(schedulerContext.getDescription());
                roomEndpoint.setRoomConfiguration(roomConfiguration);

                // Allocate aliases for the room endpoint
                allocateAliases(roomProviderCapability, roomEndpoint);

                // Update room endpoint state
                if (roomEndpoint.getState() == null) {
                    roomEndpoint.setState(ResourceRoomEndpoint.State.NOT_STARTED);
                }
                else if (roomEndpoint.getState().equals(Executable.State.STARTED)) {
                    roomEndpoint.setState(ResourceRoomEndpoint.State.MODIFIED);
                }
            }

            // Setup room reservation
            roomReservation.setSlot(interval);
            roomReservation.setRoomProviderCapability(roomProviderCapability);
            roomReservation.setRoomConfiguration(roomConfiguration);
            roomReservation.setExecutable(roomEndpoint);

            endReport();

            return roomReservation;
        }
        catch (SchedulerException exception) {
            endReportError(exception.getReport());
            throw exception;
        }
    }

    /**
     * Allocate {@link RoomEndpoint} and initialize {@code roomConfiguration} for given {@code roomProviderVariant}.
     *
     * @param roomProviderVariant  to be allocated
     * @param roomConfiguration    to be initialized
     * @param existingRoomEndpoint which is already allocated and which can be reallocated
     * @return (re)allocated {@link RoomEndpoint} or null if no executable should be allocated
     */
    private RoomEndpoint allocateRoomEndpoint(RoomProviderVariant roomProviderVariant,
            RoomConfiguration roomConfiguration, RoomEndpoint existingRoomEndpoint)
    {
        RoomProviderCapability roomProviderCapability = roomProviderVariant.getRoomProviderCapability();
        Long deviceResourceId = roomProviderCapability.getDeviceResource().getId();

        // Room configuration
        roomConfiguration.setTechnologies(roomProviderVariant.getTechnologies());
        roomConfiguration.setLicenseCount(roomProviderVariant.getLicenseCount());
        for (RoomSetting roomSetting : roomSettings) {
            roomConfiguration.addRoomSetting(roomSetting.clone());
        }

        AvailableExecutable<RoomEndpoint> reusableRoomEndpoint = roomProviderVariant.getReusableRoomEndpoint();
        if (reusableRoomEndpoint != null) {
            if (!reusableRoomEndpoint.getType().equals(AvailableReservation.Type.REUSABLE)) {
                throw new RuntimeException("Reused room should be reusable.");
            }
            Reservation originalReservation = reusableRoomEndpoint.getOriginalReservation();
            RoomEndpoint reusedRoomEndpoint = reusableRoomEndpoint.getExecutable();
            addReport(new SchedulerReportSet.ReservationReusingReport(originalReservation));

            // Reuse available reservation which allocates the reusable room
            ExistingReservation existingReservation = new ExistingReservation();
            existingReservation.setSlot(getInterval());
            existingReservation.setReservation(originalReservation);
            addChildReservation(existingReservation);
            schedulerContext.removeAvailableReservation(reusableRoomEndpoint.getAvailableReservation());

            // Reserve only the remaining capacity
            int allocatedLicenseCount = reusedRoomEndpoint.getRoomConfiguration().getLicenseCount();
            int remainingLicenseCount = roomProviderVariant.getLicenseCount() - allocatedLicenseCount;
            roomConfiguration.setLicenseCount(remainingLicenseCount);

            if (schedulerContext.isExecutableAllowed()) {
                addReport(new SchedulerReportSet.ExecutableReusingReport(reusedRoomEndpoint));

                // Allocate room endpoint
                if (existingRoomEndpoint != null && existingRoomEndpoint instanceof UsedRoomEndpoint) {
                    UsedRoomEndpoint usedRoomEndpoint = (UsedRoomEndpoint) existingRoomEndpoint;
                    if (deviceResourceId.equals(usedRoomEndpoint.getResource().getId())) {
                        // Reallocate existing room endpoint
                        usedRoomEndpoint.clearAssignedAliases();
                        return usedRoomEndpoint;
                    }
                    else if (usedRoomEndpoint.getState().isStarted()) {
                        throw new TodoImplementException("Schedule room migration.");
                    }
                }

                // Create new used room endpoint
                UsedRoomEndpoint usedRoomEndpoint = new UsedRoomEndpoint();
                usedRoomEndpoint.setRoomEndpoint(reusedRoomEndpoint);
                return usedRoomEndpoint;
            }
        }
        else if (schedulerContext.isExecutableAllowed()) {
            addReport(new SchedulerReportSet.AllocatingExecutableReport());

            // Allocate room endpoint
            if (existingRoomEndpoint != null && existingRoomEndpoint instanceof ResourceRoomEndpoint) {
                ResourceRoomEndpoint resourceRoomEndpoint = (ResourceRoomEndpoint) existingRoomEndpoint;
                if (deviceResourceId.equals(resourceRoomEndpoint.getResource().getId())) {
                    // Reallocate existing room endpoint
                    resourceRoomEndpoint.clearAssignedAliases();
                    return resourceRoomEndpoint;
                }
                else if (resourceRoomEndpoint.getState().isStarted()) {
                    throw new TodoImplementException("Schedule room migration.");
                }
            }

            // Create new resource room endpoint
            ResourceRoomEndpoint resourceRoomEndpoint = new ResourceRoomEndpoint();
            resourceRoomEndpoint.setRoomProviderCapability(roomProviderCapability);
            return resourceRoomEndpoint;
        }
        // No room endpoint should be returned
        if (existingRoomEndpoint != null && existingRoomEndpoint.getState().isStarted()) {
            // If room endpoint exists it will be stopped
            existingRoomEndpoint.setSlotEnd(DateTime.now());
        }
        return null;
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
            AliasReservationTask aliasReservationTask = aliasSpecification.createReservationTask(schedulerContext);
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
            AliasReservationTask aliasReservationTask = new AliasReservationTask(schedulerContext);
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
