package cz.cesnet.shongo.controller.cache;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.booking.resource.*;
import cz.cesnet.shongo.controller.booking.resource.Capability;
import cz.cesnet.shongo.controller.booking.resource.DeviceResource;
import cz.cesnet.shongo.controller.booking.resource.Resource;
import cz.cesnet.shongo.controller.booking.resource.ResourceReservation;
import cz.cesnet.shongo.controller.booking.room.RoomProviderCapability;
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

        // If resource is a device, add it to device topology
        if (resource instanceof DeviceResource) {
            DeviceResource deviceResource = (DeviceResource) resource;
            deviceTopology.addDeviceResource(deviceResource);
        }
        // Add capabilities
        for (Capability capability : resource.getCapabilities()) {
            // Load lazy collections
            capability.loadLazyProperties();
            // Add it to capability state
            Class<? extends Capability> capabilityType = capability.getClass();
            CapabilityState capabilityState = capabilityStateByType.get(capabilityType);
            if (capabilityState == null) {
                if (capability instanceof DeviceCapability) {
                    DeviceCapability deviceCapability = (DeviceCapability) capability;
                    capabilityState = new DeviceCapabilityState(deviceCapability.getClass());
                }
                else {
                    capabilityState = new CapabilityState(capabilityType);
                }
                capabilityStateByType.put(capabilityType, capabilityState);
            }
            capabilityState.addCapability(capability);
        }
        super.addObject(resource);
    }

    @Override
    public void removeObject(Resource resource)
    {
        // Remove capabilities
        for (Capability capability : resource.getCapabilities()) {
            // Remove it from capability state
            Class<? extends Capability> capabilityType = capability.getClass();
            CapabilityState capabilityState = capabilityStateByType.get(capabilityType);
            if (capabilityState != null) {
                capabilityState.removeCapability(resource);
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

    public <T extends Capability> Collection<T> getCapabilities(Class<T> capabilityType)
    {
        CapabilityState capabilityState = capabilityStateByType.get(capabilityType);
        if (capabilityState == null) {
            return Collections.emptyList();
        }
        @SuppressWarnings("unchecked")
        Collection<T> capabilities = (Collection) capabilityState.getCapabilities();
        return capabilities;
    }

    /**
     * @param technologies to be lookup-ed
     * @return list of {@link DeviceCapability}s which supports given {@code technologies}
     */
    public <T extends DeviceCapability> Collection<T> getDeviceCapabilities(Class<T> capabilityType, Set<Technology> technologies)
    {
        DeviceCapabilityState capabilityState = (DeviceCapabilityState) capabilityStateByType.get(capabilityType);
        if (capabilityState == null) {
            return Collections.emptyList();
        }
        Collection<T> deviceCapabilities = new LinkedList<T>();
        for (Capability capability : capabilityState.getCapabilities()) {
            DeviceResource deviceResource = (DeviceResource) capability.getResource();
            if (technologies == null || deviceResource.hasTechnologies(technologies)) {
                deviceCapabilities.add(capabilityType.cast(capability));
            }
        }
        return deviceCapabilities;
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
     * @return set of device resource ids which has capability of given {@code capabilityType} supporting given {@code technologies}
     */
    public Set<Long> getDeviceResourceIdsByCapabilityTechnologies(
            Class<? extends DeviceCapability> capabilityType, Set<Technology> technologies)
    {
        DeviceCapabilityState capabilityState = (DeviceCapabilityState) capabilityStateByType.get(capabilityType);
        if (capabilityState == null) {
            return new HashSet<Long>();
        }
        if (technologies == null) {
            return getResourcesByCapability(capabilityType);
        }
        Set<Long> devices = null;
        for (Technology technology : technologies) {
            Set<Long> technologyDevices = capabilityState.getDeviceResourceIds(technology);
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
        if (devices == null) {
            devices = new HashSet<Long>();
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

        // If reservation request purpose implies allocation of only owned resources or by root
        if (schedulerContext.isOwnerRestricted() && !Authorization.isAdministrator(schedulerContext.getUserId())) {
            // Check resource owner against reservation request owner
            if (!schedulerContext.containsCreatedByUserId(resource)) {
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
        schedulerContext.detectCollisions(reservationTask, resourceReservations);
    }

    /**
     * Checks whether given {@code capability} is available for given {@code reservationRequest}.
     * Device resources with {@link RoomProviderCapability} can be available even if theirs capacity is fully used.
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
     * Device resources with {@link RoomProviderCapability} can be available even if theirs capacity is fully used.
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
