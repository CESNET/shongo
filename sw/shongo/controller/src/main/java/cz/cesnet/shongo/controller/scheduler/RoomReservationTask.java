package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.Cache;
import cz.cesnet.shongo.controller.cache.AvailableRoom;
import cz.cesnet.shongo.controller.cache.ResourceCache;
import cz.cesnet.shongo.controller.common.RoomConfiguration;
import cz.cesnet.shongo.controller.common.RoomSetting;
import cz.cesnet.shongo.controller.executor.ResourceRoomEndpoint;
import cz.cesnet.shongo.controller.executor.RoomEndpoint;
import cz.cesnet.shongo.controller.report.ReportException;
import cz.cesnet.shongo.controller.request.AliasSpecification;
import cz.cesnet.shongo.controller.reservation.AliasReservation;
import cz.cesnet.shongo.controller.reservation.ExistingReservation;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.reservation.RoomReservation;
import cz.cesnet.shongo.controller.resource.Alias;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.resource.RoomProviderCapability;
import cz.cesnet.shongo.controller.scheduler.report.NoAvailableRoomReport;
import cz.cesnet.shongo.fault.TodoImplementException;

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
     * Specifies whether {@link Alias} should be acquired for each {@link Technology}.
     */
    private boolean withAlias = false;

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
     * @param withAlias sets the {@link #withAlias}
     */
    public void setWithAlias(boolean withAlias)
    {
        this.withAlias = withAlias;
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
        Collection<ResourceRoomEndpoint> providedRoomEndpoints =
                cacheTransaction.getProvidedExecutables(ResourceRoomEndpoint.class);
        Map<Long, RoomEndpoint> providedRoomEndpointByDeviceResourceId = new HashMap<Long, RoomEndpoint>();
        for (ResourceRoomEndpoint providedRoomEndpoint : providedRoomEndpoints) {
            //providedRoomEndpointByDeviceResourceId.put(roomEndpoint.getDeviceResource().getId(), roomEndpoint);
            Long providedRoomDeviceResourceId = providedRoomEndpoint.getDeviceResource().getId();
            if (roomVariantByDeviceResourceId.containsKey(providedRoomDeviceResourceId)) {
                RoomVariant roomVariant = roomVariantByDeviceResourceId.get(providedRoomDeviceResourceId);
                if (providedRoomEndpoint.getLicenseCount() >= roomVariant.getLicenseCount()) {
                    // Reuse provided reservation
                    Reservation providedReservation =
                            cacheTransaction.getProvidedReservationByExecutable(providedRoomEndpoint);
                    ExistingReservation existingReservation = new ExistingReservation();
                    existingReservation.setSlot(getInterval());
                    existingReservation.setReservation(providedReservation);
                    getCacheTransaction().removeProvidedReservation(providedReservation);
                    return existingReservation;
                }
                else if (providedRoomEndpoint.getLicenseCount() == 0) {
                    providedRoomEndpointByDeviceResourceId.put(providedRoomDeviceResourceId, providedRoomEndpoint);
                }
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
                return -Double.valueOf(first.getFullnessRatio()).compareTo(second.getFullnessRatio());
            }
        });
        // Get the first available room
        AvailableRoom availableRoom = availableRooms.get(0);
        RoomVariant roomVariant = roomVariantByDeviceResourceId.get(availableRoom.getDeviceResource().getId());

        // Room configuration
        RoomConfiguration roomConfiguration = new RoomConfiguration();
        roomConfiguration.setLicenseCount(roomVariant.getLicenseCount());
        roomConfiguration.setTechnologies(roomVariant.getTechnologies());
        for (RoomSetting roomSetting : roomSettings) {
            roomConfiguration.addRoomSetting(roomSetting.clone());
        }

        // Create room endpoint executable
        ResourceRoomEndpoint roomEndpoint = new ResourceRoomEndpoint();
        roomEndpoint.setUserId(getContext().getUserId());
        roomEndpoint.setDeviceResource(availableRoom.getDeviceResource());
        roomEndpoint.setRoomName(getContext().getReservationRequest().getName());
        roomEndpoint.setRoomConfiguration(roomConfiguration);
        roomEndpoint.setSlot(getInterval());
        roomEndpoint.setState(ResourceRoomEndpoint.State.NOT_STARTED);

        // Allocate aliases for each technology
        if (withAlias) {
            DeviceResource deviceResource = roomEndpoint.getDeviceResource();
            Set<Technology> roomTechnologies = roomConfiguration.getTechnologies();
            Set<Technology> missingAliasTechnologies = new HashSet<Technology>();
            missingAliasTechnologies.addAll(roomTechnologies);
            while ( missingAliasTechnologies.size() > 0 ) {
                // Allocate missing alias
                Technology technology = missingAliasTechnologies.iterator().next();
                AliasReservationTask aliasReservationTask = new AliasReservationTask(getContext());
                aliasReservationTask.addTechnology(technology);
                aliasReservationTask.setTargetResource(deviceResource);
                AliasReservation aliasReservation = addChildReservation(aliasReservationTask, AliasReservation.class);
                // Assign allocated aliases to the room
                for (Alias alias : aliasReservation.getAliases()) {
                    // Assign only aliases which can be assigned to the room (according to technology)
                    Technology aliasTechnology = alias.getTechnology();
                    if (roomTechnologies.contains(aliasTechnology)) {
                        roomEndpoint.addAssignedAlias(alias);
                        missingAliasTechnologies.remove(aliasTechnology);
                    }
                }
            }
        }

        // Create room reservation
        RoomReservation roomReservation = new RoomReservation();
        roomReservation.setSlot(getInterval());
        roomReservation.setResource(availableRoom.getDeviceResource());
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
