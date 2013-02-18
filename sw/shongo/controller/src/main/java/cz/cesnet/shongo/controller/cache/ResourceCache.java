package cz.cesnet.shongo.controller.cache;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.Authorization;
import cz.cesnet.shongo.controller.report.ReportException;
import cz.cesnet.shongo.controller.reservation.ResourceReservation;
import cz.cesnet.shongo.controller.resource.*;
import cz.cesnet.shongo.controller.scheduler.ReservationTask;
import cz.cesnet.shongo.controller.scheduler.report.ResourceAlreadyAllocatedReport;
import cz.cesnet.shongo.controller.scheduler.report.ResourceNotAllocatableReport;
import cz.cesnet.shongo.controller.scheduler.report.ResourceNotAvailableReport;
import cz.cesnet.shongo.controller.scheduler.report.UserNotOwnerReport;
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

    @Override
    public void addObject(Resource resource, EntityManager entityManager)
    {
        // Load lazy collections
        resource.getAdministrators().size();

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

            // If also has terminal capability, remove managed capability
            if (deviceResource.hasCapability(TerminalCapability.class)) {
                removeResourceCapability(deviceResource, TerminalCapability.class);
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
     * @param capabilityType
     * @return set of resource ids which has capability of given {@code capabilityType}
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
     * @return set of device resource ids which has capability of given {@code capabilityType}
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
     * @return set of device resource ids which has capability of given {@code capabilityType}
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
     * Check if resource is available (the {@link Resource#maximumFuture} is not verified).
     *
     * @param resource to be checked
     * @param context  to be used
     * @throws ReportException
     */
    private void checkResourceAvailableWithoutFuture(Resource resource, ReservationTask.Context context)
            throws ReportException
    {
        // Check if resource can be allocated and if it is available in the future
        if (!resource.isAllocatable()) {
            throw new ResourceNotAllocatableReport(resource).exception();
        }

        // If reservation request purpose implies allocation of only owned resources
        if (context.isByOwner()) {
            // Check resource owner against reservation request owner
            String userId = context.getUserId();
            if (!Authorization.Permission.isUserOwner(userId, resource)) {
                throw new UserNotOwnerReport(userId).exception();
            }
        }

        // Check if resource is not already allocated
        ObjectState<ResourceReservation> resourceState = getObjectStateRequired(resource);
        Set<ResourceReservation> resourceReservations;
        CacheTransaction cacheTransaction = context.getCacheTransaction();
        if (cacheTransaction != null) {
            resourceReservations = resourceState.getReservations(context.getInterval(),
                    cacheTransaction.getResourceCacheTransaction());
        }
        else {
            resourceReservations = resourceState.getReservations(context.getInterval());
        }
        if (resourceReservations.size() > 0) {
            throw new ResourceAlreadyAllocatedReport(resource).exception();
        }
    }

    /**
     * Checks whether given {@code capability} is available for given {@code reservationRequest}.
     * Device resources with {@link RoomProviderCapability} can be available even if theirs capacity is fully used.
     *
     * @param capability to be checked
     * @param context    for checking
     * @throws ReportException when the given {@code resource} is not available
     */
    public void checkCapabilityAvailable(Capability capability, ReservationTask.Context context) throws ReportException
    {
        // Check capability resource
        Resource resource = capability.getResource();
        checkResourceAvailableWithoutFuture(resource, context);

        if (!context.isByOwner()) {
            // Check if the capability can be allocated in the interval future
            if (!capability.isAvailableInFuture(context.getInterval().getEnd(), getReferenceDateTime())) {
                throw new ResourceNotAvailableReport(resource).exception();
            }
        }
    }

    /**
     * Checks whether given {@code resource} is available for given {@code context}.
     * Device resources with {@link RoomProviderCapability} can be available even if theirs capacity is fully used.
     *
     * @param resource to be checked
     * @param context  for checking
     * @throws ReportException when the given {@code resource} is not available
     */
    public void checkResourceAvailable(Resource resource, ReservationTask.Context context) throws ReportException
    {
        checkResourceAvailableWithoutFuture(resource, context);

        if (!context.isByOwner()) {
            // Check if the resource can be allocated in the interval future
            if (!resource.isAvailableInFuture(context.getInterval().getEnd(), getReferenceDateTime())) {
                throw new ResourceNotAvailableReport(resource).exception();
            }
        }
    }

    /**
     * @see #checkResourceAvailable(Resource, ReservationTask.Context)
     */
    public boolean isResourceAvailable(Resource resource, ReservationTask.Context context)
    {
        try {
            checkResourceAvailable(resource, context);
            return true;
        }
        catch (ReportException exception) {
            return false;
        }
    }

    public void checkResourceAvailableByParent(Resource resource, ReservationTask.Context context) throws ReportException
    {
        checkResourceAvailable(resource, context);

        // Get top parent resource and checks whether it is available
        Resource parentResource = resource;
        while (parentResource.getParentResource() != null) {
            parentResource = parentResource.getParentResource();
        }
        // Checks whether the top parent and all children resources are available
        checkResourceAndChildResourcesAvailable(parentResource, context, resource);
    }

    /**
     * @see #checkResourceAvailableByParent(Resource, ReservationTask.Context)
     */
    public boolean isResourceAvailableByParent(Resource resource, ReservationTask.Context context)
    {
        try {
            checkResourceAvailableByParent(resource, context);
            return true;
        }
        catch (ReportException exception) {
            return false;
        }
    }

    /**
     * Checks whether all children resources for given {@code dependentResource} are available
     * in given {@code interval} (recursive).
     *
     * @param resource        to be checked for availability
     * @param context         for checking
     * @param skippedResource to be skipped from checking (it is available)
     * @return
     */
    private void checkResourceAndChildResourcesAvailable(Resource resource, ReservationTask.Context context,
            Resource skippedResource) throws ReportException
    {
        // We do not check the skipped resource (it is considered as available)
        if (resource.equals(skippedResource)) {
            return;
        }
        // If the resource is already contained in the transaction, it is available
        if (context.getCacheTransaction().containsReferencedResource(resource)) {
            return;
        }
        // Check resource availability
        checkResourceAvailable(resource, context);
        // Check child resources availability
        for (Resource childResource : resource.getChildResources()) {
            checkResourceAndChildResourcesAvailable(childResource, context, skippedResource);
        }
    }
}
