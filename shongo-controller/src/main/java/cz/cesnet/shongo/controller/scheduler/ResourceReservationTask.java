package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.ControllerReportSet;
import cz.cesnet.shongo.controller.ObjectPermission;
import cz.cesnet.shongo.controller.ObjectRole;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.booking.recording.RecordingCapability;
import cz.cesnet.shongo.controller.booking.recording.RecordingServiceReservation;
import cz.cesnet.shongo.controller.booking.reservation.ExistingReservation;
import cz.cesnet.shongo.controller.booking.reservation.Reservation;
import cz.cesnet.shongo.controller.booking.reservation.ReservationManager;
import cz.cesnet.shongo.controller.booking.resource.EndpointReservation;
import cz.cesnet.shongo.controller.booking.resource.ResourceReservation;
import cz.cesnet.shongo.controller.booking.room.RoomReservation;
import cz.cesnet.shongo.controller.cache.Cache;
import cz.cesnet.shongo.controller.cache.ResourceCache;
import cz.cesnet.shongo.controller.booking.resource.DeviceResource;
import cz.cesnet.shongo.controller.booking.resource.Resource;
import cz.cesnet.shongo.controller.booking.room.RoomProviderCapability;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import java.util.LinkedList;
import java.util.List;

/**
 * Represents {@link cz.cesnet.shongo.controller.scheduler.ReservationTask} for a {@link cz.cesnet.shongo.controller.booking.compartment.CompartmentSpecification}.
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
     * @param schedulerContext sets the {@link #schedulerContext}
     * @param slot sets the {@link #slot}
     * @param resource         sets the {@link #resource}
     */
    public ResourceReservationTask(SchedulerContext schedulerContext, Interval slot, Resource resource)
    {
        super(schedulerContext,slot);
        this.resource = resource;
    }

    @Override
    protected SchedulerReport createMainReport()
    {
        return new SchedulerReportSet.AllocatingResourceReport(resource);
    }

    @Override
    protected Reservation allocateReservation(Reservation currentReservation) throws SchedulerException
    {
        //Check permissions
        AuthorizationManager authorizationManager = schedulerContext.getAuthorizationManager();
        Authorization authorization = authorizationManager.getAuthorization();
        if (UserInformation.isLocal(schedulerContext.getUserId())) {
            if (!authorization.getUsersWithRole(resource, ObjectRole.RESERVATION).isEmpty()) {
                throw new SchedulerReportSet.UserNotAllowedException(resource);
            }
        } else {
            // Check permissions for reservation from foreign domain
            //TODO
        }

        validateReservationSlot(ResourceReservation.class);

        Cache cache = getCache();
        ResourceCache resourceCache = cache.getResourceCache();

        if (schedulerContextState.containsReferencedResource(resource)) {
            // Same resource is requested multiple times
            throw new SchedulerReportSet.ResourceMultipleRequestedException(resource);
        }

        // Check resource and parent resources availability
        resourceCache.checkResourceAvailableByParent(resource, slot, schedulerContext, this);

        // Get available resource reservations
        List<AvailableReservation<ResourceReservation>> availableResourceReservations =
                new LinkedList<AvailableReservation<ResourceReservation>>();
        availableResourceReservations.addAll(schedulerContextState.getAvailableResourceReservations(resource, slot));
        sortAvailableReservations(availableResourceReservations);

        // Find matching resource reservation
        for (AvailableReservation<ResourceReservation> availableResourceReservation : availableResourceReservations) {
            Reservation originalReservation = availableResourceReservation.getOriginalReservation();

            // Only reusable available reservations
            if (!availableResourceReservation.isType(AvailableReservation.Type.REUSABLE)) {
                continue;
            }

            // Original reservation slot must contain requested slot
            if (!originalReservation.getSlot().contains(slot)) {
                continue;
            }

            // Available reservation will be returned so remove it from context (to not be used again)
            schedulerContextState.removeAvailableReservation(availableResourceReservation);

            // Create new existing resource reservation
            addReport(new SchedulerReportSet.ReservationReusingReport(originalReservation));
            ExistingReservation existingValueReservation = new ExistingReservation();
            existingValueReservation.setSlot(slot);
            existingValueReservation.setReusedReservation(originalReservation);
            return existingValueReservation;
        }

        // Proper instance of new resource reservation
        ResourceReservation resourceReservation = null;

        // If resource is a device
        if (resource instanceof DeviceResource) {
            DeviceResource deviceResource = (DeviceResource) resource;

            List<Reservation> collidingReservations = new LinkedList<Reservation>();
            // Get room reservations
            RoomProviderCapability roomProviderCapability = deviceResource.getCapability(RoomProviderCapability.class);
            if (roomProviderCapability != null) {
                ReservationManager reservationManager = new ReservationManager(schedulerContext.getEntityManager());
                List<RoomReservation> roomReservations =
                        reservationManager.getRoomReservations(roomProviderCapability, slot);
                schedulerContextState.applyReservations(roomProviderCapability.getId(), slot,
                        roomReservations, RoomReservation.class);
                collidingReservations.addAll(roomReservations);
            }
            // Get recording service reservations
            RecordingCapability recordingCapability = deviceResource.getCapability(RecordingCapability.class);
            if (recordingCapability != null) {
                ReservationManager reservationManager = new ReservationManager(schedulerContext.getEntityManager());
                List<RecordingServiceReservation> recordingServiceReservations =
                        reservationManager.getRecordingServiceReservations(recordingCapability, slot);
                schedulerContextState.applyReservations(recordingCapability.getId(), slot,
                        recordingServiceReservations, RecordingServiceReservation.class);
                collidingReservations.addAll(recordingServiceReservations);
            }

            // Check if requested resource is not fully available in the requested slot
            schedulerContext.detectCollisions(this, collidingReservations);

            // Allocate endpoint reservation
            if (deviceResource.isTerminal()) {
                // Create new endpoint reservation
                resourceReservation = new EndpointReservation();
            }
        }

        // Allocate resource reservation
        if (resourceReservation == null) {
            // Create new resource reservation
            resourceReservation = new ResourceReservation();
        }

        // Set attributes to resource reservation
        resourceReservation.setSlot(slot);
        resourceReservation.setResource(resource);

        // Add resource as referenced to the cache to prevent from multiple checking of the same parent
        schedulerContext.getState().addReferencedResource(resource);

        // Add child reservations for parent resources
        Resource parentResource = resource.getParentResource();
        if (parentResource != null && !schedulerContextState.containsReferencedResource(parentResource)) {
            ResourceReservationTask resourceReservationTask =
                    new ResourceReservationTask(schedulerContext, slot, parentResource);
            addChildReservation(resourceReservationTask);
        }

        return resourceReservation;
    }
}
