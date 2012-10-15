package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.controller.Cache;
import cz.cesnet.shongo.controller.report.ReportException;
import cz.cesnet.shongo.controller.reservation.EndpointReservation;
import cz.cesnet.shongo.controller.reservation.ResourceReservation;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.resource.Resource;
import cz.cesnet.shongo.controller.scheduler.report.ResourceNotAllocatableReport;
import cz.cesnet.shongo.controller.scheduler.report.ResourceNotAvailableReport;
import cz.cesnet.shongo.controller.scheduler.report.ResourceRequestedMultipleTimesReport;

import java.util.Set;

/**
 * Represents {@link cz.cesnet.shongo.controller.scheduler.ReservationTask} for a {@link cz.cesnet.shongo.controller.request.CompartmentSpecification}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ResourceReservationTask extends ReservationTask<ResourceReservation>
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
    protected ResourceReservation createReservation() throws ReportException
    {
        Cache.Transaction cacheTransaction = getCacheTransaction();
        if (cacheTransaction.containsResource(resource)) {
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

        // Create proper type of new resource reservation
        ResourceReservation resourceReservation;
        if (resource instanceof DeviceResource) {
            DeviceResource deviceResource = (DeviceResource) resource;
            if (deviceResource.isTerminal()) {
                resourceReservation = new EndpointReservation();
            }
            else {
                throw new IllegalStateException(
                        String.format(
                                "Device resource (id: %d) is not terminal and thus cannot be directly allocated!",
                                deviceResource.getId()));
            }
        }
        else {
            resourceReservation = new ResourceReservation();
        }
        // Set attributes to resource reservation
        resourceReservation.setSlot(getInterval());
        resourceReservation.setResource(resource);

        // Reuse resource reservation
        Set<ResourceReservation> resourceReservations = cacheTransaction.getProvidedResourceReservations(resource);
        if (resourceReservations.size() > 0) {
            // Reuse provided reservation
            ResourceReservation providedResourceReservation = resourceReservations.iterator().next();
            resourceReservation.addChildReservation(providedResourceReservation);
            cacheTransaction.removeProvidedReservation(resourceReservation);
        }

        // Add child reservations for parent resources
        Resource parentResource = resource.getParentResource();
        if (parentResource != null && !cacheTransaction.containsResource(parentResource)) {
            ResourceReservationTask resourceReservationTask = new ResourceReservationTask(getContext(), parentResource);
            ResourceReservation parentResourceReservation = resourceReservationTask.perform();
            addChildReservation(parentResourceReservation);
        }

        return resourceReservation;
    }
}