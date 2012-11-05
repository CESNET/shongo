package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.cache.AvailableVirtualRoom;
import cz.cesnet.shongo.controller.cache.ResourceCache;
import cz.cesnet.shongo.controller.executor.ResourceVirtualRoom;
import cz.cesnet.shongo.controller.report.ReportException;
import cz.cesnet.shongo.controller.request.AliasSpecification;
import cz.cesnet.shongo.controller.request.VirtualRoomSpecification;
import cz.cesnet.shongo.controller.reservation.*;
import cz.cesnet.shongo.controller.resource.Alias;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.resource.Resource;
import cz.cesnet.shongo.controller.resource.VirtualRoomsCapability;
import cz.cesnet.shongo.controller.scheduler.report.NoAvailableVirtualRoomReport;

import java.util.*;

/**
 * Represents {@link ReservationTask} for a {@link VirtualRoomSpecification}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class VirtualRoomReservationTask extends ReservationTask
{
    /**
     * Collection of sets of technologies which the virtual rooms must support (at least one of these sets must be
     * supported).
     */
    private Collection<Set<Technology>> technologySets = new ArrayList<Set<Technology>>();

    /**
     * Number of ports which must be allocated for the virtual room.
     */
    private int portCount;

    /**
     * Specifies whether {@link Alias} should be acquired for each {@link Technology}.
     */
    private boolean withAlias = false;

    /**
     * Preferred {@link Resource} with {@link VirtualRoomsCapability}.
     */
    private DeviceResource deviceResource;

    /**
     * Constructor.
     *
     * @param context                  sets the {@link #context}
     * @param virtualRoomSpecification to initialize from
     */
    public VirtualRoomReservationTask(Context context, VirtualRoomSpecification virtualRoomSpecification)
    {
        super(context);
        this.technologySets.add(virtualRoomSpecification.getTechnologies());
        this.portCount = virtualRoomSpecification.getPortCount();
        this.withAlias = virtualRoomSpecification.isWithAlias();
        this.deviceResource = virtualRoomSpecification.getDeviceResource();
    }

    /**
     * Constructor.
     *
     * @param context        sets the {@link #context}
     * @param technologySets sets the {@link #technologySets}
     * @param portCount      sets the {@link #portCount}
     */
    public VirtualRoomReservationTask(Context context, Collection<Set<Technology>> technologySets, int portCount)
    {
        super(context);
        for (Set<Technology> technologySet : technologySets) {
            this.technologySets.add(technologySet);
        }
        this.portCount = portCount;
    }

    @Override
    protected Reservation createReservation() throws ReportException
    {
        // Get device resources for specified sets of technologies
        ResourceCache resourceCache = getCache().getResourceCache();
        Set<Long> deviceResources = resourceCache.getDeviceResourcesByCapabilityTechnologies(
                VirtualRoomsCapability.class, technologySets);

        // Reuse existing reservation
        Collection<VirtualRoomReservation> virtualRoomReservations =
                getCacheTransaction().getResourceCacheTransaction().getProvidedVirtualRoomReservations();
        if (virtualRoomReservations.size() > 0) {
            for ( VirtualRoomReservation virtualRoomReservation : virtualRoomReservations) {
                Long deviceResourceId = virtualRoomReservation.getDeviceResource().getId();
                if (deviceResources.contains(deviceResourceId) && virtualRoomReservation.getPortCount() >= portCount) {
                    // Reuse provided reservation
                    ExistingReservation existingReservation = new ExistingReservation();
                    existingReservation.setSlot(getInterval());
                    existingReservation.setReservation(virtualRoomReservation);
                    getCacheTransaction().removeProvidedReservation(virtualRoomReservation);
                    return existingReservation;
                }
            }
        }

        // Get available virtual rooms
        List<AvailableVirtualRoom> availableVirtualRooms = null;
        if (deviceResource != null && deviceResources.contains(deviceResource.getId())) {
            Set<Long> preferredDeviceResources = new HashSet<Long>();
            preferredDeviceResources.add(deviceResource.getId());
            availableVirtualRooms = resourceCache.findAvailableVirtualRoomsInDeviceResources(getInterval(), portCount,
                    preferredDeviceResources, getCacheTransaction().getResourceCacheTransaction());
        }
        if (availableVirtualRooms == null || availableVirtualRooms.size() == 0) {
            availableVirtualRooms = resourceCache.findAvailableVirtualRoomsInDeviceResources(getInterval(), portCount,
                    deviceResources, getCacheTransaction().getResourceCacheTransaction());
        }
        if (availableVirtualRooms.size() == 0) {
            throw new NoAvailableVirtualRoomReport(technologySets, portCount).exception();
        }
        // Sort virtual rooms from the most filled to the least filled
        Collections.sort(availableVirtualRooms, new Comparator<AvailableVirtualRoom>()
        {
            @Override
            public int compare(AvailableVirtualRoom first, AvailableVirtualRoom second)
            {
                return -Double.valueOf(first.getFullnessRatio()).compareTo(second.getFullnessRatio());
            }
        });
        // Get the first virtual room
        AvailableVirtualRoom availableVirtualRoom = availableVirtualRooms.get(0);

        // Get technologies for the virtual room
        Set<Technology> technologies = null;
        Set<Technology> deviceTechnologies = availableVirtualRoom.getDeviceResource().getTechnologies();
        for ( Set<Technology> possibleTechnologies : technologySets) {
            if (deviceTechnologies.containsAll(possibleTechnologies)) {
                technologies = possibleTechnologies;
            }
        }
        if (technologies == null) {
            throw new IllegalStateException(
                    "Virtual room which was found doesn't match any required set of technologies.");
        }

        // Create virtual room
        ResourceVirtualRoom virtualRoom = new ResourceVirtualRoom();
        virtualRoom.setDeviceResource(availableVirtualRoom.getDeviceResource());
        virtualRoom.setPortCount(portCount);
        virtualRoom.setSlot(getInterval());
        virtualRoom.setState(ResourceVirtualRoom.State.NOT_STARTED);

        // TODO: create virtual room only for specified technologies

        // Allocate aliases for each technology
        if ( withAlias ) {
            for ( Technology technology : technologies) {
                AliasSpecification aliasSpecification = new AliasSpecification(technology);
                AliasReservation aliasReservation = addChildReservation(aliasSpecification, AliasReservation.class);
                virtualRoom.addAssignedAlias(aliasReservation.getAlias().clone());
            }
        }

        // Create virtual room reservation
        VirtualRoomReservation virtualRoomReservation = new VirtualRoomReservation();
        virtualRoomReservation.setSlot(getInterval());
        virtualRoomReservation.setResource(availableVirtualRoom.getDeviceResource());
        virtualRoomReservation.setPortCount(portCount);
        virtualRoomReservation.setExecutable(virtualRoom);
        return virtualRoomReservation;
    }
}
