package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.cache.AvailableRoom;
import cz.cesnet.shongo.controller.cache.ResourceCache;
import cz.cesnet.shongo.controller.common.Room;
import cz.cesnet.shongo.controller.executor.ResourceRoomEndpoint;
import cz.cesnet.shongo.controller.report.ReportException;
import cz.cesnet.shongo.controller.request.AliasSpecification;
import cz.cesnet.shongo.controller.request.RoomSpecification;
import cz.cesnet.shongo.controller.reservation.AliasReservation;
import cz.cesnet.shongo.controller.reservation.ExistingReservation;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.reservation.RoomReservation;
import cz.cesnet.shongo.controller.resource.Alias;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.resource.RoomProviderCapability;
import cz.cesnet.shongo.controller.scheduler.report.NoAvailableVirtualRoomReport;

import java.util.*;

/**
 * Represents {@link ReservationTask} for a {@link RoomReservation}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class RoomReservationTask extends ReservationTask
{
    /**
     * List of {@link Room} variants (the allocated {@link RoomReservation} must match at least one of these).
     */
    private Collection<Room> roomVariants = new ArrayList<Room>();

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
    public RoomReservationTask(Context context)
    {
        super(context);
    }

    /**
     * @param roomVariant to be added to the {@link #roomVariants}
     */
    public void addRoomVariant(Room roomVariant)
    {
        Set<Technology> technologies = roomVariant.getTechnologies();
        if (technologies == null || technologies.size() == 0) {
            throw new IllegalStateException(RoomSpecification.class.getSimpleName()
                    + " variant should have at least one technology.");
        }
        this.roomVariants.add(roomVariant);
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
        ResourceCache resourceCache = getCache().getResourceCache();
        ResourceCache.Transaction resourceCacheTransaction = getCacheTransaction().getResourceCacheTransaction();

        // Get participant count and check that it is same in all variants
        Integer participantCount = null;
        for (Room roomVariant : roomVariants) {
            if (participantCount == null) {
                participantCount = roomVariant.getParticipantCount();
            }
            else if (participantCount != roomVariant.getParticipantCount()) {
                throw new IllegalStateException("All room variants should have the same participant count.");
            }
        }

        // Get map of room variants by device resource ids which supports them (if one device resource supports
        // multiple room variants the one with least requested license count is selected)
        Map<Long, Room> roomVariantByDeviceResourceIds = new HashMap<Long, Room>();
        for (Room roomVariant : roomVariants) {
            Set<Long> deviceResourceIds = resourceCache.getDeviceResourcesByCapabilityTechnologies(
                    RoomProviderCapability.class, roomVariant.getTechnologies());
            // If device resource is provided, retain only the device resource in the set
            if (deviceResource != null) {
                Long deviceResourceId = deviceResource.getId();
                if (deviceResourceIds.contains(deviceResourceId)) {
                    deviceResourceIds.clear();
                    deviceResourceIds.add(deviceResourceId);
                }
                else {
                    deviceResourceIds.clear();
                }
            }
            for (Long deviceResourceId : deviceResourceIds) {
                Room deviceResourceBestRoomVariant = roomVariantByDeviceResourceIds.get(deviceResourceId);
                if (deviceResourceBestRoomVariant == null
                        || roomVariant.getLicenseCount() < deviceResourceBestRoomVariant.getLicenseCount()) {
                    deviceResourceBestRoomVariant = roomVariant;
                }
                roomVariantByDeviceResourceIds.put(deviceResourceId, deviceResourceBestRoomVariant);
            }
        }

        // Reuse existing reservation
        Collection<RoomReservation> virtualRoomReservations = resourceCacheTransaction
                .getProvidedVirtualRoomReservations();
        if (virtualRoomReservations.size() > 0) {
            for (RoomReservation virtualRoomReservation : virtualRoomReservations) {
                Long deviceResourceId = virtualRoomReservation.getDeviceResource().getId();
                if (roomVariantByDeviceResourceIds.containsKey(deviceResourceId)) {
                    Room roomVariant = roomVariantByDeviceResourceIds.get(deviceResourceId);
                    if (virtualRoomReservation.getRoom().getLicenseCount() >= roomVariant.getLicenseCount()) {
                        // Reuse provided reservation
                        ExistingReservation existingReservation = new ExistingReservation();
                        existingReservation.setSlot(getInterval());
                        existingReservation.setReservation(virtualRoomReservation);
                        getCacheTransaction().removeProvidedReservation(virtualRoomReservation);
                        return existingReservation;
                    }
                }
            }
        }

        // Get available virtual rooms
        List<AvailableRoom> availableVirtualRooms = new ArrayList<AvailableRoom>();
        for (Long deviceResourceId : roomVariantByDeviceResourceIds.keySet()) {
            Room roomVariant = roomVariantByDeviceResourceIds.get(deviceResourceId);
            DeviceResource deviceResource = (DeviceResource) resourceCache.getObject(deviceResourceId);
            AvailableRoom availableVirtualRoom = resourceCache.getAvailableVirtualRoom(deviceResource,
                    getInterval(), resourceCacheTransaction);
            if (availableVirtualRoom.getAvailableLicenseCount() >= roomVariant.getLicenseCount()) {
                availableVirtualRooms.add(availableVirtualRoom);
            }
        }
        if (availableVirtualRooms.size() == 0) {
            NoAvailableVirtualRoomReport noAvailableVirtualRoomReport = new NoAvailableVirtualRoomReport();
            for (Room roomVariant : roomVariants) {
                noAvailableVirtualRoomReport.addTechnologies(roomVariant.getTechnologies());
            }
            noAvailableVirtualRoomReport.setParticipantCount(participantCount);
            throw noAvailableVirtualRoomReport.exception();
        }
        // Sort virtual rooms from the most filled to the least filled
        Collections.sort(availableVirtualRooms, new Comparator<AvailableRoom>()
        {
            @Override
            public int compare(AvailableRoom first, AvailableRoom second)
            {
                return -Double.valueOf(first.getFullnessRatio()).compareTo(second.getFullnessRatio());
            }
        });
        // Get the first virtual room
        AvailableRoom availableVirtualRoom = availableVirtualRooms.get(0);
        Room roomVariant = roomVariantByDeviceResourceIds.get(availableVirtualRoom.getDeviceResource().getId());

        // Create virtual room
        ResourceRoomEndpoint virtualRoom = new ResourceRoomEndpoint();
        virtualRoom.setDeviceResource(availableVirtualRoom.getDeviceResource());
        virtualRoom.setRoom(roomVariant.clone());
        virtualRoom.setSlot(getInterval());
        virtualRoom.setState(ResourceRoomEndpoint.State.NOT_STARTED);

        // TODO: create virtual room only for specified technologies

        // Allocate aliases for each technology
        if (withAlias) {
            for (Technology technology : roomVariant.getTechnologies()) {
                AliasSpecification aliasSpecification = new AliasSpecification(technology,
                        availableVirtualRoom.getDeviceResource());
                AliasReservation aliasReservation = addChildReservation(aliasSpecification, AliasReservation.class);
                virtualRoom.addAssignedAlias(aliasReservation.getAlias().clone());
            }
        }

        // Create virtual room reservation
        RoomReservation virtualRoomReservation = new RoomReservation();
        virtualRoomReservation.setSlot(getInterval());
        virtualRoomReservation.setResource(availableVirtualRoom.getDeviceResource());
        virtualRoomReservation.setRoom(roomVariant.clone());
        virtualRoomReservation.setExecutable(virtualRoom);
        return virtualRoomReservation;
    }
}
