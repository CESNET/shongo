package cz.cesnet.shongo.controller.cache;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.booking.resource.*;
import cz.cesnet.shongo.controller.scheduler.*;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Represents a cache for all resources in efficient form. It also holds
 * allocation information about resources which are used , e.g., by scheduler.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ResourceCache extends AbstractCache<Resource>
{
    private static Logger logger = LoggerFactory.getLogger(ResourceCache.class);

    /**
     * Map of capability states by theirs types.
     */
    private Map<Class<? extends Capability>, CapabilityState> capabilityStateByType;

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
    public void addObject(Resource resource)
    {
        // Load lazy collections
        resource.loadLazyProperties();

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
        super.addObject(resource);
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
        capabilityStateByType = new HashMap<Class<? extends Capability>, CapabilityState>();
        deviceTopology = new DeviceTopology();
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
     * supporting given {@code technologies}
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
     * Check if resource is available (the {@link Resource#maximumFuture} is not verified).
     *
     *
     * @param resource         to be checked
     * @param slot             to be checked
     * @param schedulerContext to be used
     * @param reservationTask  to be used
     * @throws SchedulerException
     */
    private void checkResourceAvailableWithoutFuture(Resource resource, Interval slot,
            SchedulerContext schedulerContext, ReservationTask reservationTask) throws SchedulerException
    {
        // Check if resource can be allocated and if it is available in the future
        if (!resource.isAllocatable()) {
            throw new SchedulerReportSet.ResourceNotAllocatableException(resource);
        }

        // If reservation request purpose implies allocation of only owned resources
        if (schedulerContext.isOwnerRestricted()) {
            // Check resource owner against reservation request owner
            if (!schedulerContext.containsOwnerUserId(resource)) {
                throw new SchedulerReportSet.UserNotOwnerException();
            }
        }

        // Check if resource is not already allocated
        Long resourceId = resource.getId();
        ResourceManager resourceManager = new ResourceManager(schedulerContext.getEntityManager());
        List<ResourceReservation> resourceReservations =
                resourceManager.listResourceReservationsInInterval(resourceId, slot);

        // Apply transaction
        SchedulerContextState schedulerContextState = schedulerContext.getState();
        schedulerContextState.applyReservations(resourceId, slot, resourceReservations, ResourceReservation.class);

        // Perform check
        schedulerContext.detectCollisions(reservationTask, resourceReservations,
                new SchedulerReportSet.ResourceAlreadyAllocatedReport(resource));
    }

    /**
     * Checks whether given {@code capability} is available for given {@code reservationRequest}.
     * Device resources with {@link cz.cesnet.shongo.controller.booking.room.RoomProviderCapability} can be available even if theirs capacity is fully used.
     *
     * @param capability       to be checked
     * @param slot             to be checked
     * @param schedulerContext for checking  @throws SchedulerException when the given {@code resource} is not available
     * @param reservationTask  to be used
     */
    public void checkCapabilityAvailable(Capability capability, Interval slot, SchedulerContext schedulerContext,
            ReservationTask reservationTask)
            throws SchedulerException
    {
        // Check capability resource
        Resource resource = capability.getResource();
        checkResourceAvailableWithoutFuture(resource, slot, schedulerContext, reservationTask);

        if (schedulerContext.isMaximumFutureAndDurationRestricted()) {
            // Check if the capability can be allocated in the interval future
            if (!capability.isAvailableInFuture(slot.getEnd(),
                    schedulerContext.getMinimumDateTime())) {
                DateTime maxDateTime = capability.getMaximumFutureDateTime(schedulerContext.getMinimumDateTime());
                throw new SchedulerReportSet.ResourceNotAvailableException(resource, maxDateTime);
            }
        }
    }

    /**
     * Checks whether given {@code resource} is available for given {@code context}.
     * Device resources with {@link cz.cesnet.shongo.controller.booking.room.RoomProviderCapability} can be available even if theirs capacity is fully used.
     *
     *
     * @param resource         to be checked
     * @param slot             for checking
     * @param schedulerContext for checking
     * @param reservationTask  to be used
     * @throws SchedulerException when the given {@code resource} is not available
     */
    public void checkResourceAvailable(Resource resource, Interval slot,
            SchedulerContext schedulerContext, ReservationTask reservationTask)
            throws SchedulerException
    {
        checkResourceAvailableWithoutFuture(resource, slot, schedulerContext, reservationTask);

        if (schedulerContext.isMaximumFutureAndDurationRestricted()) {
            // Check if the resource can be allocated in the interval future
            if (!resource.isAvailableInFuture(slot.getEnd(),
                    schedulerContext.getMinimumDateTime())) {
                DateTime maxDateTime = resource.getMaximumFutureDateTime(schedulerContext.getMinimumDateTime());
                throw new SchedulerReportSet.ResourceNotAvailableException(resource, maxDateTime);
            }
        }
    }

    /**
     * @see #checkResourceAvailable
     */
    public boolean isResourceAvailable(Resource resource, Interval slot,
            SchedulerContext schedulerContext, ReservationTask reservationTask)
    {
        try {
            checkResourceAvailable(resource, slot, schedulerContext, reservationTask);
            return true;
        }
        catch (SchedulerException exception) {
            return false;
        }
    }

    public void checkResourceAvailableByParent(Resource resource, Interval slot,
            SchedulerContext schedulerContext, ReservationTask reservationTask) throws SchedulerException
    {
        checkResourceAvailable(resource, slot, schedulerContext, reservationTask);

        // Get top parent resource and checks whether it is available
        Resource parentResource = resource;
        while (parentResource.getParentResource() != null) {
            parentResource = parentResource.getParentResource();
        }
        // Checks whether the top parent and all children resources are available
        checkResourceAndChildResourcesAvailable(parentResource, slot, resource, schedulerContext, reservationTask);
    }

    /**
     * @see #checkResourceAvailableByParent
     */
    public boolean isResourceAvailableByParent(Resource resource, Interval slot,
            SchedulerContext schedulerContext, ReservationTask reservationTask)
    {
        try {
            checkResourceAvailableByParent(resource, slot, schedulerContext, reservationTask);
            return true;
        }
        catch (SchedulerException exception) {
            return false;
        }
    }

    /**
     * Checks whether all children resources for given {@code dependentResource} are available
     * in given {@code interval} (recursive).
     *
     *
     * @param resource         to be checked for availability
     * @param skippedResource  to be skipped from checking (it is available)
     * @param schedulerContext for checking
     * @return
     */
    private void checkResourceAndChildResourcesAvailable(Resource resource, Interval slot, Resource skippedResource,
            SchedulerContext schedulerContext, ReservationTask reservationTask) throws SchedulerException
    {
        // We do not check the skipped resource (it is considered as available)
        if (resource.equals(skippedResource)) {
            return;
        }
        // If the resource is already contained in the transaction, it is available
        SchedulerContextState schedulerContextState = schedulerContext.getState();
        if (schedulerContextState.containsReferencedResource(resource)) {
            return;
        }
        // Check resource availability
        checkResourceAvailable(resource, slot, schedulerContext, reservationTask);
        // Check child resources availability
        for (Resource childResource : resource.getChildResources()) {
            checkResourceAndChildResourcesAvailable(
                    childResource, slot, skippedResource, schedulerContext, reservationTask);
        }
    }
}
