package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.cache.Cache;
import cz.cesnet.shongo.controller.cache.ResourceCache;
import cz.cesnet.shongo.controller.reservation.*;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.resource.Resource;
import cz.cesnet.shongo.controller.resource.RoomProviderCapability;
import org.joda.time.Interval;

import java.util.List;
import java.util.Set;

/**
 * Represents {@link cz.cesnet.shongo.controller.scheduler.ReservationTask} for a {@link cz.cesnet.shongo.controller.request.CompartmentSpecification}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ResourceReservationTask extends ReservationTask
{
    /**
     * {@link Resource} which should be allocated.
     */
    private Resource resource;

    /**
     * Constructor.
     *
     * @param schedulerContext  sets the {@link #schedulerContext}
     * @param resource sets the {@link #resource}
     */
    public ResourceReservationTask(SchedulerContext schedulerContext, Resource resource)
    {
        super(schedulerContext);
        this.resource = resource;
    }

    @Override
    protected SchedulerReport createMainReport()
    {
        return new SchedulerReportSet.AllocatingResourceReport(resource);
    }

    @Override
    protected Reservation allocateReservation(Reservation allocatedReservation) throws SchedulerException
    {
        validateReservationSlot(ResourceReservation.class);

        SchedulerContext schedulerContext = getSchedulerContext();
        Interval interval = schedulerContext.getInterval();

        Cache cache = getCache();
        ResourceCache resourceCache = cache.getResourceCache();

        if (schedulerContext.containsReferencedResource(resource)) {
            // Same resource is requested multiple times
            throw new SchedulerReportSet.ResourceMultipleRequestedException(resource);
        }

        // Check resource and parent resources availability
        resourceCache.checkResourceAvailableByParent(resource, schedulerContext);

        // Reuse existing reservation
        Set<AvailableReservation<ResourceReservation>> availableReservations =
                schedulerContext.getAvailableResourceReservations(resource);
        if (availableReservations.size() > 0) {
            AvailableReservation<ResourceReservation> availableReservation = availableReservations.iterator().next();
            Reservation originalReservation = availableReservation.getOriginalReservation();
            if (availableReservation.isExistingReservationRequired()) {
                addReport(new SchedulerReportSet.ReservationReusingReport(originalReservation));

                // Reuse provided reservation
                ExistingReservation existingReservation = new ExistingReservation();
                existingReservation.setSlot(interval);
                existingReservation.setReservation(originalReservation);
                schedulerContext.removeAvailableReservation(availableReservation);
                return existingReservation;
            }
            else {
                throw new TodoImplementException("reallocate resource");
            }
        }

        // Proper instance of new resource reservation
        ResourceReservation resourceReservation = null;

        // If resource is a device
        if (resource instanceof DeviceResource) {
            DeviceResource deviceResource = (DeviceResource) resource;
            if (deviceResource.isTerminal()) {
                resourceReservation = new EndpointReservation();
            }
            RoomProviderCapability roomProviderCapability = deviceResource.getCapability(RoomProviderCapability.class);
            if (roomProviderCapability != null) {
                ReservationManager reservationManager = new ReservationManager(schedulerContext.getEntityManager());
                List<RoomReservation> roomReservations =
                        reservationManager.getRoomReservations(roomProviderCapability, interval);
                schedulerContext.applyRoomReservations(roomProviderCapability.getId(), roomReservations);
                if (roomReservations.size() > 0) {
                    // Requested resource is not available in the requested slot
                    throw new SchedulerReportSet.ResourceAlreadyAllocatedException(resource);
                }
            }
        }
        // If no instance was set use default
        if (resourceReservation == null) {
            resourceReservation = new ResourceReservation();
        }

        // Set attributes to resource reservation
        resourceReservation.setSlot(interval);
        resourceReservation.setResource(resource);

        // Add resource as referenced to the cache to prevent from multiple checking of the same parent
        schedulerContext.addReferencedResource(resource);

        // Add child reservations for parent resources
        Resource parentResource = resource.getParentResource();
        if (parentResource != null && !schedulerContext.containsReferencedResource(parentResource)) {
            ResourceReservationTask resourceReservationTask = new ResourceReservationTask(schedulerContext, parentResource);
            addChildReservation(resourceReservationTask);
        }

        return resourceReservation;
    }
}
