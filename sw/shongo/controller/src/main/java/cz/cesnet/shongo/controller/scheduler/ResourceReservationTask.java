package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.controller.Cache;
import cz.cesnet.shongo.controller.cache.CacheTransaction;
import cz.cesnet.shongo.controller.cache.ResourceCache;
import cz.cesnet.shongo.controller.cache.RoomCache;
import cz.cesnet.shongo.controller.report.Report;
import cz.cesnet.shongo.controller.report.ReportException;
import cz.cesnet.shongo.controller.request.ReservationRequest;
import cz.cesnet.shongo.controller.reservation.EndpointReservation;
import cz.cesnet.shongo.controller.reservation.ExistingReservation;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.reservation.ResourceReservation;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.resource.Resource;
import cz.cesnet.shongo.controller.resource.RoomProviderCapability;
import cz.cesnet.shongo.controller.scheduler.report.*;
import org.joda.time.Interval;

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
    protected Report createdMainReport()
    {
        return new AllocatingResourceReport(resource);
    }

    @Override
    protected Reservation createReservation() throws ReportException
    {
        validateReservationSlot(ResourceReservation.class);

        Context context = getContext();
        Interval interval = context.getInterval();

        Cache cache = getCache();
        CacheTransaction cacheTransaction = getCacheTransaction();
        ResourceCache resourceCache = cache.getResourceCache();

        if (cacheTransaction.containsReferencedResource(resource)) {
            // Same resource is requested multiple times
            throw new ResourceRequestedMultipleTimesReport(resource).exception();
        }

        // Check resource and parent resources availability
        resourceCache.checkResourceAvailableByParent(resource, context);

        // Reuse existing reservation
        Set<ResourceReservation> resourceReservations = cacheTransaction.getProvidedResourceReservations(resource);
        if (resourceReservations.size() > 0) {
            ResourceReservation providedResourceReservation = resourceReservations.iterator().next();
            addReport(new ReusingReservationReport(providedResourceReservation));

            // Reuse provided reservation
            ExistingReservation existingReservation = new ExistingReservation();
            existingReservation.setSlot(interval);
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
            RoomProviderCapability roomProvider = deviceResource.getCapability(RoomProviderCapability.class);
            if (roomProvider != null) {
                RoomCache roomCache = cache.getRoomCache();
                if (roomCache.getRoomReservations(roomProvider, interval, cacheTransaction).size() > 0) {
                    // Requested resource is not available in the requested slot
                    throw new ResourceAlreadyAllocatedReport(resource).exception();
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
        cacheTransaction.addReferencedResource(resource);

        // Add child reservations for parent resources
        Resource parentResource = resource.getParentResource();
        if (parentResource != null && !cacheTransaction.containsReferencedResource(parentResource)) {
            ResourceReservationTask resourceReservationTask = new ResourceReservationTask(context, parentResource);
            addChildReservation(resourceReservationTask);
        }

        return resourceReservation;
    }
}
