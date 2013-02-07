package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.controller.cache.CacheTransaction;
import cz.cesnet.shongo.controller.report.ReportException;
import cz.cesnet.shongo.controller.reservation.EndpointReservation;
import cz.cesnet.shongo.controller.reservation.ExistingReservation;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.reservation.ResourceReservation;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.resource.Resource;
import cz.cesnet.shongo.controller.resource.RoomProviderCapability;
import cz.cesnet.shongo.controller.scheduler.report.ResourceNotAllocatableReport;
import cz.cesnet.shongo.controller.scheduler.report.ResourceNotAvailableReport;
import cz.cesnet.shongo.controller.scheduler.report.ResourceRequestedMultipleTimesReport;

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
     * @param context  sets the {@link #context}
     * @param resource sets the {@link #resource}
     */
    public ResourceReservationTask(Context context, Resource resource)
    {
        super(context);
        this.resource = resource;
    }

    @Override
    protected Reservation createReservation() throws ReportException
    {
        CacheTransaction cacheTransaction = getCacheTransaction();
        if (cacheTransaction.containsReferencedResource(resource)) {
            // Same resource is requested multiple times
            throw new ResourceRequestedMultipleTimesReport(resource).exception();
        }
        if (!resource.isAllocatable()) {
            // Requested resource cannot be allocated
            throw new ResourceNotAllocatableReport(resource).exception();
        }
        if (!getCache().isResourceAvailable(resource, getInterval(), cacheTransaction)) {
            // Requested resource is not available in the requested slot
            throw new ResourceNotAvailableReport(resource).exception();
        }

        // Reuse existing reservation
        Set<ResourceReservation> resourceReservations = cacheTransaction.getProvidedResourceReservations(resource);
        if (resourceReservations.size() > 0) {
            // Reuse provided reservation
            ResourceReservation providedResourceReservation = resourceReservations.iterator().next();
            ExistingReservation existingReservation = new ExistingReservation();
            existingReservation.setSlot(getInterval());
            existingReservation.setReservation(providedResourceReservation);
            cacheTransaction.removeProvidedReservation(providedResourceReservation);
            return existingReservation;
        }

        // Proper instance of new resource reservation
        ResourceReservation resourceReservation = null;

        // If resource is a device
        if (resource instanceof DeviceResource) {
            DeviceResource deviceResource = (DeviceResource) resource;
            if (deviceResource.isTerminal()) {
                resourceReservation = new EndpointReservation();
            }
            if (deviceResource.hasCapability(RoomProviderCapability.class)) {
                if (getCache().getResourceCache().getRoomReservations(deviceResource, getInterval(), cacheTransaction)
                        .size() > 0) {
                    // Requested resource is not available in the requested slot
                    throw new ResourceNotAvailableReport(resource).exception();
                }
            }
        }

        // If no instance was set use default
        if (resourceReservation == null) {
            resourceReservation = new ResourceReservation();
        }

        // Set attributes to resource reservation
        resourceReservation.setSlot(getInterval());
        resourceReservation.setResource(resource);

        // Add resource as referenced to the cache to prevent from multiple checking of the same parent
        cacheTransaction.addReferencedResource(resource);

        // Add child reservations for parent resources
        Resource parentResource = resource.getParentResource();
        if (parentResource != null && !cacheTransaction.containsReferencedResource(parentResource)) {
            ResourceReservationTask resourceReservationTask = new ResourceReservationTask(getContext(), parentResource);
            addChildReservation(resourceReservationTask);
        }

        return resourceReservation;
    }
}
