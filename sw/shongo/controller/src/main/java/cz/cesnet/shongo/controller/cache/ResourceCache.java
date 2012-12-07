package cz.cesnet.shongo.controller.cache;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.reservation.ResourceReservation;
import cz.cesnet.shongo.controller.reservation.RoomReservation;
import cz.cesnet.shongo.controller.resource.*;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import java.util.*;

/**
 * Represents a cache for all resources in efficient form. It also holds
 * allocation information about resources which are used , e.g., by scheduler.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ResourceCache extends AbstractReservationCache<Resource, ResourceReservation>
{
    private static Logger logger = LoggerFactory.getLogger(ResourceCache.class);

    /**
     * Map of capability states by theirs types.
     */
    private Map<Class<? extends Capability>, CapabilityState> capabilityStateByType =
            new HashMap<Class<? extends Capability>, CapabilityState>();

    /**
     * Map of states for cached objects with {@link cz.cesnet.shongo.controller.resource.RoomProviderCapability}
     * by theirs identifiers.
     */
    private Map<Long, ObjectState<RoomReservation>> roomProviderStateById =
            new HashMap<Long, ObjectState<RoomReservation>>();

    /**
     * @see DeviceTopology
     */
    private DeviceTopology deviceTopology;

    /**
     * Constructor.
     */
    public ResourceCache()
    {
    }

    /**
     * @return {@link #deviceTopology}
     */
    public DeviceTopology getDeviceTopology()
    {
        return deviceTopology;
    }

    /**
     * Load resources from the database.
     *
     * @param entityManager
     */
    @Override
    public void loadObjects(EntityManager entityManager)
    {
        logger.debug("Loading resources...");

        ResourceManager resourceManager = new ResourceManager(entityManager);
        List<Resource> resourceList = resourceManager.list(null);
        for (Resource resource : resourceList) {
            addObject(resource, entityManager);
        }
    }

    @Override
    public void addObject(Resource resource, EntityManager entityManager)
    {
        // If resource is a device
        if (resource instanceof DeviceResource) {
            DeviceResource deviceResource = (DeviceResource) resource;

            // Add it to device topology
            deviceTopology.addDeviceResource(deviceResource);

            // If device resource has terminal capability, add it to managed capabilities
            TerminalCapability terminalCapability = deviceResource.getCapability(TerminalCapability.class);
            if (terminalCapability != null) {
                addResourceCapability(terminalCapability);
            }

            // If device resource has virtual rooms capability, add it to managed capabilities
            RoomProviderCapability roomProviderCapability = deviceResource.getCapability(RoomProviderCapability.class);
            if (roomProviderCapability != null) {
                addResourceCapability(roomProviderCapability);

                // Add virtual room state
                roomProviderStateById.put(deviceResource.getId(), new ObjectState<RoomReservation>());
            }
        }
        super.addObject(resource, entityManager);
    }

    @Override
    public void removeObject(Resource resource)
    {
        // If resource is a device
        if (resource instanceof DeviceResource) {
            DeviceResource deviceResource = (DeviceResource) resource;

            // Remove the device from the device topology
            deviceTopology.removeDeviceResource(deviceResource);

            // If also has virtual rooms, remove managed capability
            if (deviceResource.hasCapability(TerminalCapability.class)) {
                removeResourceCapability(deviceResource, TerminalCapability.class);
            }

            // If also has virtual rooms, remove managed capability
            if (deviceResource.hasCapability(RoomProviderCapability.class)) {
                removeResourceCapability(deviceResource, RoomProviderCapability.class);

                // Remove virtual room state
                roomProviderStateById.remove(deviceResource.getId());
            }
        }
        super.removeObject(resource);
    }

    @Override
    public void clear()
    {
        deviceTopology = new DeviceTopology();
        capabilityStateByType.clear();
        super.clear();
    }

    /**
     * @param deviceResource for which the state should be returned
     * @return state for given {@code object}
     * @throws IllegalArgumentException when state cannot be found
     */
    private ObjectState<RoomReservation> getRoomProviderState(DeviceResource deviceResource)
    {
        Long objectId = deviceResource.getId();
        ObjectState<RoomReservation> objectState = roomProviderStateById.get(objectId);
        if (objectState == null) {
            throw new IllegalArgumentException(
                    RoomProviderCapability.class.getSimpleName() + " '" + objectId + "' isn't in the cache!");
        }
        return objectState;
    }

    @Override
    protected void onAddReservation(Resource object, ResourceReservation reservation)
    {
        if (reservation instanceof RoomReservation) {
            RoomReservation roomReservation = (RoomReservation) reservation;
            DeviceResource deviceResource = roomReservation.getDeviceResource();
            ObjectState<RoomReservation> objectState = getRoomProviderState(deviceResource);
            objectState.addReservation(roomReservation);
        }
        else {
            super.onAddReservation(object, reservation);
        }
    }

    @Override
    public void onRemove(Resource object, ResourceReservation reservation)
    {
        if (reservation instanceof RoomReservation) {
            RoomReservation roomReservation = (RoomReservation) reservation;
            DeviceResource deviceResource = roomReservation.getDeviceResource();
            ObjectState<RoomReservation> objectState = getRoomProviderState(deviceResource);
            objectState.removeReservation(roomReservation);
        }
        else {
            super.onRemove(object, reservation);
        }
    }

    /**
     * Add resource capability to be managed and resource can be looked up by it.
     *
     * @param capability
     */
    private void addResourceCapability(Capability capability)
    {
        Class<? extends Capability> capabilityType = capability.getClass();

        // Get capability state and add the capability to it
        CapabilityState capabilityState = capabilityStateByType.get(capabilityType);
        if (capabilityState == null) {
            capabilityState = new CapabilityState(capabilityType);
            capabilityStateByType.put(capabilityType, capabilityState);
        }
        capabilityState.addCapability(capability);
    }

    /**
     * Remove resource capability from managed capabilities.
     *
     * @param resource
     * @param capabilityType
     */
    private void removeResourceCapability(Resource resource, Class<? extends Capability> capabilityType)
    {
        // Get capability state
        CapabilityState capabilityState = capabilityStateByType.get(capabilityType);
        if (capabilityState == null) {
            return;
        }
        capabilityState.removeCapability(resource);
    }

    /**
     * @param resourceId
     * @param capabilityType
     * @return capability of given {@code capabilityType} from resource with given {@code resourceId}
     */
    private <T extends Capability> T getResourceCapability(Long resourceId, Class<T> capabilityType)
    {
        CapabilityState capabilityState = capabilityStateByType.get(capabilityType);
        if (capabilityState == null) {
            return null;
        }
        return capabilityType.cast(capabilityState.getCapability(resourceId));
    }

    /**
     * @param resourceId
     * @param capabilityType
     * @return true if resource with given {@code resourceId} has capability of given {@code capabilityType},
     *         false otherwise
     */
    private boolean hasResourceCapability(Long resourceId, Class<? extends Capability> capabilityType)
    {
        return getResourceCapability(resourceId, capabilityType) != null;
    }

    /**
     * @param capabilityType
     * @return set of resource identifiers which has capability of given {@code capabilityType}
     */
    private Set<Long> getResourcesByCapability(Class<? extends Capability> capabilityType)
    {
        CapabilityState capabilityState = capabilityStateByType.get(capabilityType);
        if (capabilityState == null) {
            return new HashSet<Long>();
        }
        return capabilityState.getResourceIds();
    }

    /**
     * @param capabilityType
     * @param technologies
     * @return set of device resource identifiers which has capability of given {@code capabilityType}
     *         supporting given {@code technologies}
     */
    public Set<Long> getDeviceResourcesByCapabilityTechnologies(Class<? extends DeviceCapability> capabilityType,
            Set<Technology> technologies)
    {
        CapabilityState capabilityState = capabilityStateByType.get(capabilityType);
        if (capabilityState == null) {
            return new HashSet<Long>();
        }
        if (technologies == null) {
            return getResourcesByCapability(capabilityType);
        }
        Set<Long> devices = null;
        for (Technology technology : technologies) {
            Set<Long> technologyDevices = capabilityState.getResourceIds(technology);
            if (devices == null) {
                devices = new HashSet<Long>();
                if (technologyDevices != null) {
                    devices.addAll(technologyDevices);
                }
            }
            else if (technologyDevices == null) {
                devices.clear();
                break;
            }
            else {
                devices.retainAll(technologyDevices);
            }
        }
        return devices;
    }

    /**
     * @param capabilityType
     * @param technologySets
     * @return set of device resource identifiers which has capability of given {@code capabilityType}
     *         supporting at least one set of given {@code technologySets}
     */
    public Set<Long> getDeviceResourcesByCapabilityTechnologies(
            Class<? extends DeviceCapability> capabilityType, Collection<Set<Technology>> technologySets)
    {
        if (technologySets == null) {
            return getDeviceResourcesByCapabilityTechnologies(capabilityType, (Set<Technology>) null);
        }
        Set<Long> devices = new HashSet<Long>();
        for (Set<Technology> technologies : technologySets) {
            devices.addAll(getDeviceResourcesByCapabilityTechnologies(capabilityType, technologies));
        }
        return devices;
    }

    @Override
    protected void updateObjectState(Resource resource, Interval workingInterval,
            EntityManager entityManager)
    {
        // Get all allocated virtual rooms for the device and add them to the device state
        ResourceManager resourceManager = new ResourceManager(entityManager);
        List<ResourceReservation> resourceReservations = resourceManager.listResourceReservationsInInterval(
                resource.getId(), getWorkingInterval());
        for (ResourceReservation resourceReservation : resourceReservations) {
            addReservation(resource, resourceReservation);
        }
    }

    /**
     * Checks whether {@code resource} is available. Device resources with {@link cz.cesnet.shongo.controller.resource.RoomProviderCapability} can
     * be available even if theirs capacity is fully used.
     *
     * @param resource
     * @param interval
     * @param transaction
     * @return true if given {@code resource} is available,
     *         false otherwise
     */
    public boolean isResourceAvailable(Resource resource, Interval interval, Transaction transaction)
    {
        // Check if resource can be allocated and if it is available in the future
        if (!resource.isAllocatable() || !resource.isAvailableInFuture(interval.getEnd(), getReferenceDateTime())) {
            return false;
        }
        // Check if resource is not already allocated
        ObjectState<ResourceReservation> resourceState = getObjectState(resource);
        Set<ResourceReservation> resourceReservations = resourceState.getReservations(interval, transaction);
        if (resourceReservations.size() > 0) {
            return false;
        }
        return true;
    }

    /**
     * Checks whether all children resources for given {@code currentResource} are available
     * in given {@code interval} (recursive).
     *
     * @param resource          to be skipped from checking
     * @param dependentResource
     * @param interval
     * @return
     */
    private boolean isDependentResourceAvailable(Resource resource, Resource dependentResource, Interval interval,
            Transaction transaction)
    {
        // We do not consider the resource itself as dependent and thus it is ignored (considered as available)
        if (dependentResource.equals(resource)) {
            return true;
        }
        // If dependent resource is already contained in transaction, it is available
        if (transaction.containsResource(dependentResource)) {
            return true;
        }
        if (!isResourceAvailable(dependentResource, interval, transaction)) {
            return false;
        }
        for (Resource childDependentResource : dependentResource.getChildResources()) {
            if (!isDependentResourceAvailable(resource, childDependentResource, interval, transaction)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks whether all dependent resources for given {@code resource} are available
     * in given {@code interval} (recursive).
     *
     * @param resource
     * @param interval
     * @return true if dependent resources from given {@code resource} are available (recursive),
     *         false otherwise
     */
    public boolean isDependentResourcesAvailable(Resource resource, Interval interval, Transaction transaction)
    {
        // Get top parent resource and checks whether it is available
        Resource parentResource = resource;
        while (parentResource.getParentResource() != null) {
            parentResource = parentResource.getParentResource();
        }
        return isDependentResourceAvailable(resource, parentResource, interval, transaction);
    }

    /**
     * @param deviceResource to be checked
     * @param interval       which should be checked
     * @return collection of {@link cz.cesnet.shongo.controller.reservation.RoomReservation}s in given {@code interval} for given {@code deviceResource}
     */
    public Collection<RoomReservation> getRoomReservations(DeviceResource deviceResource,
            Interval interval)
    {
        ObjectState<RoomReservation> roomProviderState = getRoomProviderState(deviceResource);
        Set<RoomReservation> roomReservations = roomProviderState.getReservations(interval);
        return roomReservations;
    }

    /**
     * @param deviceResource
     * @param interval
     * @return {@link AvailableRoom} for given {@code deviceResource} in given {@code interval}
     */
    public AvailableRoom getAvailableRoom(DeviceResource deviceResource, Interval interval,
            Transaction transaction)
    {
        ObjectState<RoomReservation> roomProviderState = getRoomProviderState(deviceResource);
        RoomProviderCapability roomProviderCapability =
                getResourceCapability(deviceResource.getId(), RoomProviderCapability.class);
        if (roomProviderCapability == null) {
            throw new IllegalStateException("Device resource doesn't have "
                    + RoomProviderCapability.class.getSimpleName() + ".");
        }
        Set<RoomReservation> roomReservations = roomProviderState.getReservations(interval);
        int usedLicenseCount = 0;
        if (isResourceAvailable(deviceResource, interval, transaction)) {
            for (ResourceReservation resourceReservation : roomReservations) {
                RoomReservation roomReservation = (RoomReservation) resourceReservation;
                usedLicenseCount += roomReservation.getRoomConfiguration().getLicenseCount();
            }
        }
        else {
            usedLicenseCount = roomProviderCapability.getLicenseCount();
        }
        AvailableRoom availableRoom = new AvailableRoom();
        availableRoom.setDeviceResource(deviceResource);
        availableRoom.setMaximumLicenseCount(roomProviderCapability.getLicenseCount());
        availableRoom.setAvailableLicenseCount(roomProviderCapability.getLicenseCount() - usedLicenseCount);
        return availableRoom;
    }

    /**
     * Transaction for {@link ResourceCache}.
     */
    public static class Transaction
            extends AbstractReservationCache.Transaction<ResourceReservation>
    {
        /**
         * Set of resources referenced from {@link ResourceReservation}s in the transaction.
         */
        private Set<Resource> referencedResources = new HashSet<Resource>();

        /**
         * Provided {@link cz.cesnet.shongo.controller.reservation.RoomReservation}s in the {@link Transaction}.
         */
        private List<RoomReservation> providedRoomReservations = new ArrayList<RoomReservation>();

        /**
         * @param resource to be added to the {@link #referencedResources}
         */
        public void addReferencedResource(Resource resource)
        {
            referencedResources.add(resource);
        }

        /**
         * @param resource to be checked
         * @return true if given resource was referenced by any {@link ResourceReservation} added to the transaction,
         *         false otherwise
         */
        public boolean containsResource(Resource resource)
        {
            return referencedResources.contains(resource);
        }

        @Override
        public void addProvidedReservation(Long objectId, ResourceReservation reservation)
        {
            if (reservation instanceof RoomReservation) {
                RoomReservation roomReservation = (RoomReservation) reservation;
                providedRoomReservations.add(roomReservation);
            }
            else {
                super.addProvidedReservation(objectId, reservation);
            }
        }

        @Override
        public void removeProvidedReservation(Long objectId, ResourceReservation reservation)
        {
            if (reservation instanceof RoomReservation) {
                RoomReservation roomReservation = (RoomReservation) reservation;
                providedRoomReservations.remove(roomReservation);
            }
            else {
                super.removeProvidedReservation(objectId, reservation);
            }
        }

        /**
         * @return collection of provided {@link cz.cesnet.shongo.controller.reservation.RoomReservation}
         */
        public Collection<RoomReservation> getProvidedRoomReservations()
        {
            return providedRoomReservations;
        }
    }
}
