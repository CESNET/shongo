package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.Cache;
import cz.cesnet.shongo.controller.cache.AvailableRoom;
import cz.cesnet.shongo.controller.cache.CacheTransaction;
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
     * Specifies whether {@link Alias} should be acquired for each allocated room {@link Technology}.
     */
    private boolean withTechnologyAliases = false;

    /**
     * {@link DeviceResource} with {@link RoomProviderCapability} for which the {@link RoomReservation}
     * should be allocated.
     */
    private DeviceResource deviceResource = null;

    /**
     * Constructor.
     *
     * @param context sets the {@link #context}
     */
    public RoomReservationTask(Context context, int participantCount)
    {
        super(context);
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
     * @param withTechnologyAliases sets the {@link #withTechnologyAliases}
     */
    public void setWithTechnologyAliases(boolean withTechnologyAliases)
    {
        this.withTechnologyAliases = withTechnologyAliases;
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
    protected Reservation createReservation() throws SchedulerException
    {
        Context context = getContext();
        Cache cache = getCache();
        CacheTransaction cacheTransaction = getCacheTransaction();
        Interval interval = getInterval();

        // Check maximum duration
        if (context.isMaximumFutureAndDurationRestricted()) {
            checkMaximumDuration(interval, cache.getRoomReservationMaximumDuration());
        }

        Set<RoomProviderCapability> specifiedRoomProviders = null;
        if (deviceResource != null) {
            specifiedRoomProviders = new HashSet<RoomProviderCapability>();
            specifiedRoomProviders.add(deviceResource.getCapability(RoomProviderCapability.class));
        }

        // Get map of room variants by device resource ids which supports them (if one device resource supports
        // multiple room variants the one with least requested license count is used)
        Map<Long, RoomVariant> roomVariantByRoomProviderId = new HashMap<Long, RoomVariant>();
        for (Set<Technology> technologies : technologyVariants) {
            Collection<RoomProviderCapability> roomProviders = specifiedRoomProviders;
            if (roomProviders != null) {
                for (Iterator<RoomProviderCapability> iterator = roomProviders.iterator(); iterator.hasNext(); ) {
                    RoomProviderCapability roomProvider = iterator.next();
                    if (!roomProvider.getDeviceResource().hasTechnologies(technologies)) {
                        iterator.remove();
                    }
                }
            }
            else {
                roomProviders = new HashSet<RoomProviderCapability>();
                roomProviders.addAll(cache.getRoomProviders(technologies));
            }

            for (RoomProviderCapability roomProvider : roomProviders) {
                Long roomProviderId = roomProvider.getId();
                RoomVariant newRoomVariant = new RoomVariant(roomProvider, participantCount, technologies);
                RoomVariant roomVariant = roomVariantByRoomProviderId.get(roomProviderId);
                if (roomVariant == null || roomVariant.getLicenseCount() > newRoomVariant.getLicenseCount()) {
                    roomVariant = newRoomVariant;
                }
                roomVariantByRoomProviderId.put(roomProviderId, roomVariant);
            }
        }

        // Get provided room endpoints
        Collection<ResourceRoomEndpoint> providedRooms =
                cacheTransaction.getProvidedExecutables(ResourceRoomEndpoint.class);
        // Map of available provided rooms which doesn't have enough capacity
        final Map<Long, ResourceRoomEndpoint> availableProvidedRooms = new HashMap<Long, ResourceRoomEndpoint>();
        // Find provided room with enough capacity to be reused or suitable room to which will be allocated more capacity
        for (ResourceRoomEndpoint providedRoomEndpoint : providedRooms) {
            Long providedRoomProviderId = providedRoomEndpoint.getRoomProviderCapability().getId();
            // Skip not suitable endpoints (by technology)
            if (!roomVariantByRoomProviderId.containsKey(providedRoomProviderId)) {
                continue;
            }
            // Get room variant with the least requested capacity
            RoomVariant roomVariant = roomVariantByRoomProviderId.get(providedRoomProviderId);
            // If provided room has enough allocated capacity
            if (providedRoomEndpoint.getLicenseCount() >= roomVariant.getLicenseCount()) {
                Reservation providedReservation =
                        cacheTransaction.getProvidedReservationByExecutable(providedRoomEndpoint);
                addReport(new SchedulerReportSet.ReservationReusingReport(providedReservation));

                // Reuse provided reservation which allocates the provided room
                ExistingReservation existingReservation = new ExistingReservation();
                existingReservation.setSlot(getInterval());
                existingReservation.setReservation(providedReservation);
                cacheTransaction.removeProvidedReservation(providedReservation);
                return existingReservation;
            }
            // Else provided room doesn't have enough capacity
            else {
                // Append it to map of provided room
                availableProvidedRooms.put(providedRoomProviderId, providedRoomEndpoint);
            }
        }

        // Get available rooms
        beginReport(new SchedulerReportSet.FindingAvailableResourceReport());
        List<AvailableRoom> availableRooms = new ArrayList<AvailableRoom>();
        try {
            for (Long roomProviderId : roomVariantByRoomProviderId.keySet()) {
                RoomProviderCapability roomProvider = cache.getRoomProvider(roomProviderId);
                RoomVariant roomVariant = roomVariantByRoomProviderId.get(roomProviderId);
                AvailableRoom availableRoom = cache.getAvailableRoom(roomProvider, context);
                if (availableRoom.getAvailableLicenseCount() >= roomVariant.getLicenseCount()) {
                    availableRooms.add(availableRoom);
                    addReport(new SchedulerReportSet.ResourceReport(roomProvider.getResource()));
                }
            }
            if (availableRooms.size() == 0) {
                throw new SchedulerException(getCurrentReport());
            }
        }
        finally {
            endReport();
        }

        // Sort available rooms, prefer the ones with provided room and most filled to the least filled
        addReport(new SchedulerReportSet.SortingResourcesReport());
        Collections.sort(availableRooms, new Comparator<AvailableRoom>()
        {
            @Override
            public int compare(AvailableRoom first, AvailableRoom second)
            {
                boolean firstHasProvidedRoom =
                        availableProvidedRooms.containsKey(first.getRoomProviderCapability().getId());
                boolean secondHasProvidedRoom =
                        availableProvidedRooms.containsKey(second.getRoomProviderCapability().getId());
                if (firstHasProvidedRoom && !secondHasProvidedRoom) {
                    return -1;
                }
                if (!firstHasProvidedRoom && secondHasProvidedRoom) {
                    return 1;
                }
                int result = -Double.valueOf(first.getFullnessRatio()).compareTo(second.getFullnessRatio());
                if (result != 0) {
                    return result;
                }
                return -Integer.valueOf(first.getMaximumLicenseCount()).compareTo(second.getMaximumLicenseCount());
            }
        });
        // Get the first available room
        for (AvailableRoom availableRoom : availableRooms) {
            RoomProviderCapability roomProvider = availableRoom.getRoomProviderCapability();
            DeviceResource deviceResource = roomProvider.getDeviceResource();
            beginReport(new SchedulerReportSet.AllocatingResourceReport(deviceResource));
            CacheTransaction.Savepoint cacheTransactionSavepoint = cacheTransaction.createSavepoint();
            try {
                // Get device and it's room variant
                RoomVariant roomVariant = roomVariantByRoomProviderId.get(roomProvider.getId());

                // Create room reservation
                RoomReservation roomReservation = new RoomReservation();
                roomReservation.setSlot(interval);
                roomReservation.setRoomProviderCapability(roomProvider);

                // Room configuration
                RoomConfiguration roomConfiguration = new RoomConfiguration();
                roomConfiguration.setTechnologies(roomVariant.getTechnologies());
                for (RoomSetting roomSetting : roomSettings) {
                    roomConfiguration.addRoomSetting(roomSetting.clone());
                }

                // Allocated room endpoint
                RoomEndpoint roomEndpoint = null;

                // If provided room is available
                ResourceRoomEndpoint availableProvidedRoom = availableProvidedRooms.get(roomProvider.getId());
                if (availableProvidedRoom != null) {
                    Reservation providedReservation =
                            cacheTransaction.getProvidedReservationByExecutable(availableProvidedRoom);
                    addReport(new SchedulerReportSet.ReservationReusingReport(providedReservation));

                    // Reuse provided reservation which allocates the provided room
                    ExistingReservation existingReservation = new ExistingReservation();
                    existingReservation.setSlot(interval);
                    existingReservation.setReservation(providedReservation);
                    addChildReservation(existingReservation);
                    cacheTransaction.removeProvidedReservation(providedReservation);

                    // Reserve only the remaining capacity
                    roomConfiguration.setLicenseCount(
                            roomVariant.getLicenseCount() - availableProvidedRoom.getLicenseCount());

                    if (context.isExecutableAllowed()) {
                        addReport(new SchedulerReportSet.ExecutableReusingReport(availableProvidedRoom));

                        // Create new used room endpoint
                        UsedRoomEndpoint usedRoomEndpoint = new UsedRoomEndpoint();
                        usedRoomEndpoint.setRoomEndpoint(availableProvidedRoom);
                        roomEndpoint = usedRoomEndpoint;
                    }
                }
                else {
                    // Reserve full capacity
                    roomConfiguration.setLicenseCount(roomVariant.getLicenseCount());

                    if (context.isExecutableAllowed()) {
                        addReport(new SchedulerReportSet.AllocatingExecutableReport());

                        // Create new room endpoint  \
                        ResourceRoomEndpoint resourceRoomEndpoint = new ResourceRoomEndpoint();
                        resourceRoomEndpoint.setRoomProviderCapability(roomProvider);
                        roomEndpoint = resourceRoomEndpoint;
                    }
                }

                // Setup room endpoint
                if (context.isExecutableAllowed()) {
                    // Set of technologies which are supported in the room
                    Set<Technology> roomTechnologies = roomConfiguration.getTechnologies();

                    // Allocate aliases from alias specifications
                    for (AliasSpecification aliasSpecification : aliasSpecifications) {
                        AliasReservationTask aliasReservationTask = aliasSpecification.createReservationTask(context);
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
                        AliasReservationTask aliasReservationTask = new AliasReservationTask(getContext());
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

                    // Allocate aliases for technologies which are missing
                    if (withTechnologyAliases) {
                        while (missingAliasTechnologies.size() > 0) {
                            // Allocate missing alias
                            Technology technology = missingAliasTechnologies.iterator().next();
                            AliasReservationTask aliasReservationTask = new AliasReservationTask(getContext());
                            aliasReservationTask.addTechnology(technology);
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
                                }
                            }
                        }
                    }

                    // Setup abstract room endpoint
                    roomEndpoint.setSlot(interval);
                    roomEndpoint.setRoomDescription(context.getReservationDescription());
                    roomEndpoint.setRoomConfiguration(roomConfiguration);
                    roomEndpoint.setState(ResourceRoomEndpoint.State.NOT_STARTED);
                }

                // Set room configuration and executable
                roomReservation.setRoomConfiguration(roomConfiguration);
                roomReservation.setExecutable(roomEndpoint);

                return roomReservation;
            }
            catch (SchedulerException exception) {
                cacheTransactionSavepoint.revert();

                // Try to allocate next available room
                continue;
            }
            finally {
                cacheTransactionSavepoint.destroy();
                endReport();
            }
        }
        throw new SchedulerException(getCurrentReport());
    }

    /**
     * Represents a variant for allocating a {@link RoomReservation}.
     */
    public class RoomVariant
    {
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
         * @param roomProvider     to be used for computing {@link #licenseCount}
         * @param participantCount number of participants for the {@link RoomReservation}
         * @param technologies     set of {@link Technology}s
         */
        public RoomVariant(RoomProviderCapability roomProvider, int participantCount, Set<Technology> technologies)
        {
            this.technologies = technologies;
            this.licenseCount = computeLicenseCount(roomProvider, participantCount, technologies);
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
         * @param roomProvider
         * @param participantCount
         * @param technologies
         * @return number of licenses for given {@code participantCount} and {@code technologies}
         *         in {@link DeviceResource} with given {@code deviceResourceId}
         */
        public int computeLicenseCount(RoomProviderCapability roomProvider, int participantCount,
                Set<Technology> technologies)
        {
            return participantCount;
        }
    }
}
