package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.Cache;
import cz.cesnet.shongo.controller.cache.AvailableRoom;
import cz.cesnet.shongo.controller.cache.ResourceCache;
import cz.cesnet.shongo.controller.common.RoomConfiguration;
import cz.cesnet.shongo.controller.common.RoomSetting;
import cz.cesnet.shongo.controller.executor.ResourceRoomEndpoint;
import cz.cesnet.shongo.controller.executor.RoomEndpoint;
import cz.cesnet.shongo.controller.executor.UsedRoomEndpoint;
import cz.cesnet.shongo.controller.report.ReportException;
import cz.cesnet.shongo.controller.reservation.AliasReservation;
import cz.cesnet.shongo.controller.reservation.ExistingReservation;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.reservation.RoomReservation;
import cz.cesnet.shongo.controller.resource.Alias;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.resource.RoomProviderCapability;
import cz.cesnet.shongo.controller.scheduler.report.NoAvailableRoomReport;

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
    private Collection<Set<Technology>> technologyVariants = new ArrayList<Set<Technology>>();

    /**
     * Collection of {@link RoomSetting} for allocated {@link RoomReservation}.
     */
    private Collection<RoomSetting> roomSettings = new ArrayList<RoomSetting>();

    /**
     * Specifies whether {@link Alias} should be acquired for each {@link RoomProviderCapability#requiredAliasTypes}.
     */
    private boolean withRequiredAliases = false;

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
     * @param roomSetting to be added to the {@link #roomSettings}
     */
    public void addRoomSetting(RoomSetting roomSetting)
    {
        roomSettings.add(roomSetting);
    }

    /**
     * @param withRequiredAliases sets the {@link #withRequiredAliases}
     */
    public void setWithRequiredAliases(boolean withRequiredAliases)
    {
        this.withRequiredAliases = withRequiredAliases;
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
    protected Reservation createReservation() throws ReportException
    {
        Cache.Transaction cacheTransaction = getCacheTransaction();
        ResourceCache resourceCache = getCache().getResourceCache();
        ResourceCache.Transaction resourceCacheTransaction = cacheTransaction.getResourceCacheTransaction();

        Set<Long> specifiedDeviceResourceIds = null;
        if (deviceResource != null) {
            specifiedDeviceResourceIds = new HashSet<Long>();
            specifiedDeviceResourceIds.add(deviceResource.getId());
        }

        // Get map of room variants by device resource ids which supports them (if one device resource supports
        // multiple room variants the one with least requested license count is used)
        Map<Long, RoomVariant> roomVariantByDeviceResourceId = new HashMap<Long, RoomVariant>();
        for (Set<Technology> technologies : technologyVariants) {
            Set<Long> deviceResourceIds = specifiedDeviceResourceIds;
            if (deviceResourceIds == null) {
                deviceResourceIds = resourceCache.getDeviceResourcesByCapabilityTechnologies(
                        RoomProviderCapability.class, technologies);
            }

            for (Long deviceResourceId : deviceResourceIds) {
                RoomVariant newRoomVariant = new RoomVariant(deviceResourceId, participantCount, technologies);
                RoomVariant roomVariant = roomVariantByDeviceResourceId.get(deviceResourceId);
                if (roomVariant == null || roomVariant.getLicenseCount() > newRoomVariant.getLicenseCount()) {
                    roomVariant = newRoomVariant;
                }
                roomVariantByDeviceResourceId.put(deviceResourceId, roomVariant);
            }
        }

        // Get provided room endpoints
        Collection<ResourceRoomEndpoint> providedRooms =
                cacheTransaction.getProvidedExecutables(ResourceRoomEndpoint.class);
        // Map of available provided rooms which doesn't have enough capacity
        final Map<Long, ResourceRoomEndpoint> availableProvidedRooms = new HashMap<Long, ResourceRoomEndpoint>();
        // Find provided room with enough capacity to be reused or suitable room to which will be allocated more capacity
        for (ResourceRoomEndpoint providedRoomEndpoint : providedRooms) {
            Long providedRoomDeviceResourceId = providedRoomEndpoint.getDeviceResource().getId();
            // Skip not suitable endpoints (by technology)
            if (!roomVariantByDeviceResourceId.containsKey(providedRoomDeviceResourceId)) {
                continue;
            }
            // Get room variant with the least requested capacity
            RoomVariant roomVariant = roomVariantByDeviceResourceId.get(providedRoomDeviceResourceId);
            // If provided room has enough allocated capacity
            if (providedRoomEndpoint.getLicenseCount() >= roomVariant.getLicenseCount()) {
                // Reuse provided reservation which allocates the provided room
                Reservation providedReservation =
                        cacheTransaction.getProvidedReservationByExecutable(providedRoomEndpoint);
                ExistingReservation existingReservation = new ExistingReservation();
                existingReservation.setSlot(getInterval());
                existingReservation.setReservation(providedReservation);
                getCacheTransaction().removeProvidedReservation(providedReservation);
                return existingReservation;
            }
            // Else provided room doesn't have enough capacity
            else {
                // Append it to map of provided room
                availableProvidedRooms.put(providedRoomDeviceResourceId, providedRoomEndpoint);
            }
        }

        // Get available rooms
        List<AvailableRoom> availableRooms = new ArrayList<AvailableRoom>();
        for (Long deviceResourceId : roomVariantByDeviceResourceId.keySet()) {
            RoomVariant roomVariant = roomVariantByDeviceResourceId.get(deviceResourceId);
            DeviceResource deviceResource = (DeviceResource) resourceCache.getObject(deviceResourceId);
            AvailableRoom availableRoom = resourceCache.getAvailableRoom(deviceResource,
                    getInterval(), resourceCacheTransaction);
            if (availableRoom.getAvailableLicenseCount() >= roomVariant.getLicenseCount()) {
                availableRooms.add(availableRoom);
            }
        }
        if (availableRooms.size() == 0) {
            NoAvailableRoomReport noAvailableRoomReport = new NoAvailableRoomReport();
            noAvailableRoomReport.setParticipantCount(participantCount);
            for (Set<Technology> technologies : technologyVariants) {
                noAvailableRoomReport.addTechnologies(technologies);
            }
            throw noAvailableRoomReport.exception();
        }

        // TODO: prefer provided room endpoints
        // Sort available rooms from the most filled to the least filled
        Collections.sort(availableRooms, new Comparator<AvailableRoom>()
        {
            @Override
            public int compare(AvailableRoom first, AvailableRoom second)
            {
                boolean firstHasProvidedRoom = availableProvidedRooms.containsKey(first.getDeviceResource().getId());
                boolean secondHasProvidedRoom = availableProvidedRooms.containsKey(second.getDeviceResource().getId());
                if (firstHasProvidedRoom && !secondHasProvidedRoom) {
                    return -1;
                }
                if (!firstHasProvidedRoom && secondHasProvidedRoom) {
                    return 1;
                }
                return -Double.valueOf(first.getFullnessRatio()).compareTo(second.getFullnessRatio());
            }
        });
        // Get the first available room
        AvailableRoom availableRoom = availableRooms.get(0);
        DeviceResource deviceResource = availableRoom.getDeviceResource();
        RoomVariant roomVariant = roomVariantByDeviceResourceId.get(deviceResource.getId());

        // Room configuration
        RoomConfiguration roomConfiguration = new RoomConfiguration();
        roomConfiguration.setTechnologies(roomVariant.getTechnologies());
        for (RoomSetting roomSetting : roomSettings) {
            roomConfiguration.addRoomSetting(roomSetting.clone());
        }

        // Allocated room endpoint
        RoomEndpoint roomEndpoint;

        // If provided room is available
        ResourceRoomEndpoint availableProvidedRoom = availableProvidedRooms.get(deviceResource.getId());
        if (availableProvidedRoom == null) {
            // Reserve full capacity
            roomConfiguration.setLicenseCount(roomVariant.getLicenseCount());

            // Create new room endpoint
            ResourceRoomEndpoint resourceRoomEndpoint = new ResourceRoomEndpoint();
            resourceRoomEndpoint.setDeviceResource(deviceResource);
            roomEndpoint = resourceRoomEndpoint;
        }
        else {
            // Reuse provided reservation which allocates the provided room
            Reservation providedReservation =
                    cacheTransaction.getProvidedReservationByExecutable(availableProvidedRoom);
            ExistingReservation existingReservation = new ExistingReservation();
            existingReservation.setSlot(getInterval());
            existingReservation.setReservation(providedReservation);
            addChildReservation(existingReservation);
            getCacheTransaction().removeProvidedReservation(providedReservation);

            // Reserve only the remaining capacity
            roomConfiguration.setLicenseCount(roomVariant.getLicenseCount() - availableProvidedRoom.getLicenseCount());

            // Create new used room endpoint
            UsedRoomEndpoint usedRoomEndpoint = new UsedRoomEndpoint();
            usedRoomEndpoint.setRoomEndpoint(availableProvidedRoom);
            roomEndpoint = usedRoomEndpoint;
        }

        // Allocate aliases for the room
        if (withRequiredAliases || withTechnologyAliases) {
            // Set of technologies which are supported in the room
            Set<Technology> roomTechnologies = roomConfiguration.getTechnologies();
            // Set of alias types which should be supported in the room
            RoomProviderCapability roomProviderCapability = deviceResource.getCapability(RoomProviderCapability.class);
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
            if (withRequiredAliases) {
                while (missingAliasTypes.size() > 0) {
                    // Allocate missing alias
                    AliasType missingAliasType = missingAliasTypes.iterator().next();
                    AliasReservationTask aliasReservationTask = new AliasReservationTask(getContext());
                    aliasReservationTask.setAliasType(missingAliasType);
                    aliasReservationTask.setTargetResource(deviceResource);
                    AliasReservation aliasReservation = addChildReservation(aliasReservationTask, AliasReservation.class);
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
            }

            // Allocate aliases for technologies which are missing
            if (withTechnologyAliases) {
                while (missingAliasTechnologies.size() > 0) {
                    // Allocate missing alias
                    Technology technology = missingAliasTechnologies.iterator().next();
                    AliasReservationTask aliasReservationTask = new AliasReservationTask(getContext());
                    aliasReservationTask.addTechnology(technology);
                    aliasReservationTask.setTargetResource(deviceResource);
                    AliasReservation aliasReservation = addChildReservation(aliasReservationTask, AliasReservation.class);
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
        }

        // Setup abstract room endpoint
        roomEndpoint.setUserId(getContext().getUserId());
        roomEndpoint.setSlot(getInterval());
        roomEndpoint.setRoomDescription(getContext().getReservationRequest().getDescription());
        roomEndpoint.setRoomConfiguration(roomConfiguration);
        roomEndpoint.setState(ResourceRoomEndpoint.State.NOT_STARTED);

        // Create room reservation
        RoomReservation roomReservation = new RoomReservation();
        roomReservation.setSlot(getInterval());
        roomReservation.setResource(deviceResource);
        roomReservation.setRoomConfiguration(roomConfiguration);
        roomReservation.setExecutable(roomEndpoint);
        return roomReservation;
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
         * @param deviceResourceId {@link DeviceResource} used for computing {@link #licenseCount}
         * @param participantCount number of participants for the {@link RoomReservation}
         * @param technologies     set of {@link Technology}s
         */
        public RoomVariant(Long deviceResourceId, int participantCount, Set<Technology> technologies)
        {
            this.technologies = technologies;
            this.licenseCount = computeLicenseCount(deviceResourceId, participantCount, technologies);
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
         * @param deviceResourceId
         * @param participantCount
         * @param technologies
         * @return number of licenses for given {@code participantCount} and {@code technologies}
         *         in {@link DeviceResource} with given {@code deviceResourceId}
         */
        public int computeLicenseCount(Long deviceResourceId, int participantCount, Set<Technology> technologies)
        {
            return participantCount;
        }
    }
}
