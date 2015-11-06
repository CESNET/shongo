package cz.cesnet.shongo.controller.booking.recording;

import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.domain.DomainResource;
import cz.cesnet.shongo.controller.booking.executable.Executable;
import cz.cesnet.shongo.controller.booking.executable.ExecutableService;
import cz.cesnet.shongo.controller.booking.reservation.Reservation;
import cz.cesnet.shongo.controller.booking.reservation.ReservationManager;
import cz.cesnet.shongo.controller.booking.resource.DeviceResource;
import cz.cesnet.shongo.controller.booking.resource.Resource;
import cz.cesnet.shongo.controller.booking.resource.ResourceManager;
import cz.cesnet.shongo.controller.booking.room.RoomEndpoint;
import cz.cesnet.shongo.controller.booking.room.RoomProviderCapability;
import cz.cesnet.shongo.controller.booking.room.RoomReservation;
import cz.cesnet.shongo.controller.booking.room.RoomReservationTask;
import cz.cesnet.shongo.controller.cache.Cache;
import cz.cesnet.shongo.controller.cache.ResourceCache;
import cz.cesnet.shongo.controller.scheduler.*;
import cz.cesnet.shongo.util.RangeSet;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import javax.persistence.EntityManager;
import java.util.*;

/**
 * Represents {@link ReservationTask} for a {@link RecordingServiceReservation}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class RecordingServiceReservationTask extends ReservationTask
{
    private Resource resource;

    private Executable executable;

    private boolean enabled;

    /**
     * Constructor.
     *
     * @param schedulerContext sets the {@link #schedulerContext}
     * @param slot             sets the {@link #slot}
     */
    public RecordingServiceReservationTask(SchedulerContext schedulerContext, Interval slot)
    {
        super(schedulerContext, slot);
    }

    public void setResource(Resource resource)
    {
        this.resource = resource;
    }

    public void setExecutable(Executable executable)
    {
        this.executable = executable;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    @Override
    protected SchedulerReport createMainReport()
    {
        return new SchedulerReportSet.AllocatingRecordingServiceReport(enabled);
    }

    @Override
    protected Reservation allocateReservation(Reservation currentReservation) throws SchedulerException
    {
        if (executable == null) {
            throw new IllegalStateException("Executable must be set.");
        }

        Cache cache = getCache();
        ResourceCache resourceCache = cache.getResourceCache();
        Interval executableSlot = executable.getSlot();

        // Check interval
        if (!executableSlot.contains(slot)) {
            throw new SchedulerReportSet.ExecutableServiceInvalidSlotException(executableSlot, slot);
        }

        // Check executable
        Set<Technology> technologies = new HashSet<Technology>();
        if (executable instanceof RecordableEndpoint) {
            RecordableEndpoint recordableEndpoint = (RecordableEndpoint) executable;
            DeviceResource deviceResource = recordableEndpoint.getResource();
            RecordingCapability recordingCapability = deviceResource.getCapability(RecordingCapability.class);

            // Set required technologies for recorder
            technologies.addAll(recordableEndpoint.getTechnologies());

            // If room is not automatically recordable
            if (recordingCapability == null) {
                // Additional allocate something for executable
                if (executable instanceof RoomEndpoint) {
                    // Allocate one room license for recording
                    RoomProviderCapability roomProviderCapability =
                            deviceResource.getCapabilityRequired(RoomProviderCapability.class);
                    RoomReservationTask roomReservationTask = new RoomReservationTask(schedulerContext, this.slot);
                    roomReservationTask.setRoomProviderCapability(roomProviderCapability);
                    roomReservationTask.setParticipantCount(1);
                    roomReservationTask.setAllocateRoomEndpoint(false);
                    addChildReservation(roomReservationTask);
                }
            }
            else {
                // Check whether licenses aren't unlimited (otherwise the rooms are always recordable
                // and we shouldn't allocate the recording service for it)
                if (recordingCapability.getLicenseCount() == null) {
                    throw new SchedulerReportSet.RoomEndpointAlwaysRecordableException(
                            ObjectIdentifier.formatId(executable));
                }
            }
        }
        else {
            throw new TodoImplementException(
                    executable.getClass() + " doesn't implement " + RecordableEndpoint.class.getSimpleName() + ".");
        }

        // Find matching recording devices
        List<AvailableRecorder> availableRecorders = new LinkedList<>();
        beginReport(new SchedulerReportSet.FindingAvailableResourceReport());
        for (RecordingCapability recordingCapability : resourceCache.getCapabilities(RecordingCapability.class)) {
            DeviceResource deviceResource = recordingCapability.getDeviceResource();
            if (this.resource != null && !deviceResource.getId().equals(this.resource.getId())) {
                continue;
            }
            if (technologies.size() > 0 && !deviceResource.hasTechnologies(technologies)) {
                continue;
            }
            EntityManager entityManager = schedulerContext.getEntityManager();
            ResourceManager resourceManager = new ResourceManager(entityManager);
            ReservationManager reservationManager = new ReservationManager(entityManager);

            String userId = schedulerContext.getUserId();
            if (!UserInformation.isLocal(userId)) {
                Long domainId = UserInformation.parseDomainId(userId);
                DomainResource domainResource;
                try {
                    domainResource = resourceManager.getDomainResource(domainId, deviceResource.getId());

                    Long reservationId = currentReservation == null ? null : currentReservation.getId();
                    List<RecordingServiceReservation> reservations = reservationManager.getRecordingReservationsForDomain(domainId, deviceResource.getId(), slot, reservationId);
                    int usedLicensesByDomain = schedulerContext.getLicenseCountPeak(slot, reservations, deviceResource.getCapability(RecordingCapability.class));

                    if (domainResource.getLicenseCount() - usedLicensesByDomain < 1) {
                        // Recording capability already used by this domain
                        continue;
                    }
                }
                catch (CommonReportSet.ObjectNotExistsException e) {
                    // Skip this resource, not allowed for domain (given by user id)
                    continue;
                }
            }

            // Get available recorder
            List<RecordingServiceReservation> roomReservations =
                    reservationManager.getRecordingServiceReservations(recordingCapability, slot);
            schedulerContextState.applyReservations(
                    recordingCapability.getId(), this.slot, roomReservations, RecordingServiceReservation.class);
            RangeSet<RecordingServiceReservation, DateTime> rangeSet =
                    new RangeSet<RecordingServiceReservation, DateTime>();
            for (RecordingServiceReservation roomReservation : roomReservations) {
                rangeSet.add(roomReservation, roomReservation.getSlotStart(), roomReservation.getSlotEnd());
            }
            List<RangeSet.Bucket> roomBuckets = new LinkedList<RangeSet.Bucket>();
            roomBuckets.addAll(rangeSet.getBuckets(slot.getStart(), slot.getEnd()));
            Collections.sort(roomBuckets, new Comparator<RangeSet.Bucket>()
            {
                @Override
                public int compare(RangeSet.Bucket roomBucket1, RangeSet.Bucket roomBucket2)
                {
                    return -Double.compare(roomBucket1.size(), roomBucket2.size());
                }
            });
            int usedLicenseCount = 0;
            if (roomBuckets.size() > 0) {
                RangeSet.Bucket roomBucket = roomBuckets.get(0);
                usedLicenseCount = roomBucket.size();
            }
            AvailableRecorder availableRecorder = new AvailableRecorder(recordingCapability, usedLicenseCount);
            if (Integer.valueOf(0).equals(availableRecorder.getAvailableLicenseCount())) {
                addReport(new SchedulerReportSet.ResourceRecordingCapacityExceededReport(deviceResource));
                continue;
            }
            availableRecorders.add(availableRecorder);
            addReport(new SchedulerReportSet.ResourceReport(deviceResource));
        }
        if (availableRecorders.size() == 0) {
            throw new SchedulerReportSet.ResourceNotFoundException();
        }
        endReport();

        // Sort recorders
        addReport(new SchedulerReportSet.SortingResourcesReport());
        Collections.sort(availableRecorders, new Comparator<AvailableRecorder>()
        {
            @Override
            public int compare(AvailableRecorder first, AvailableRecorder second)
            {
                int result = -Double.compare(first.getFullnessRatio(), second.getFullnessRatio());
                if (result != 0) {
                    return result;
                }
                return 0;
            }
        });

        // Allocate reservation in some matching recorder
        for (AvailableRecorder availableRecorder : availableRecorders) {
            RecordingCapability recordingCapability = availableRecorder.getRecordingCapability();
            DeviceResource deviceResource = availableRecorder.getDeviceResource();

            // Check whether alias provider can be allocated
            try {
                resourceCache.checkCapabilityAvailable(recordingCapability, this.slot, schedulerContext, this);
            }
            catch (SchedulerException exception) {
                addReport(exception.getReport());
                continue;
            }
            beginReport(new SchedulerReportSet.AllocatingResourceReport(deviceResource));

            // Allocate recording service
            RecordingService recordingService = new RecordingService();
            recordingService.setRecordingCapability(recordingCapability);
            recordingService.setExecutable(executable);
            recordingService.setSlot(slot);
            if (enabled) {
                recordingService.setState(ExecutableService.State.PREPARED);
            }
            else {
                recordingService.setState(ExecutableService.State.NOT_ACTIVE);
            }

            // Allocate recording reservation
            RecordingServiceReservation recordingServiceReservation = new RecordingServiceReservation();
            recordingServiceReservation.setRecordingCapability(recordingCapability);
            recordingServiceReservation.setSlot(slot);
            recordingServiceReservation.setExecutableService(recordingService);
            return recordingServiceReservation;
        }
        throw new SchedulerException(getCurrentReport());
    }

    /**
     * Represents available recordings in {@link RecordingCapability}.
     */
    private static class AvailableRecorder
    {
        /**
         * {@link DeviceResource} with the {@link RecordingCapability}.
         */
        private final RecordingCapability recordingCapability;

        /**
         * Number of available {@link RecordingCapability#licenseCount}.
         */
        private final Integer availableLicenseCount;

        /**
         * Constructor.
         *
         * @param recordingCapability sets the {@link #recordingCapability}
         * @param usedLicenseCount    to be used for computing {@link #availableLicenseCount}
         */
        private AvailableRecorder(RecordingCapability recordingCapability, int usedLicenseCount)
        {
            this.recordingCapability = recordingCapability;
            Integer maximumLicenseCount = recordingCapability.getLicenseCount();
            if (maximumLicenseCount != null) {
                this.availableLicenseCount = maximumLicenseCount - usedLicenseCount;
                if (this.availableLicenseCount < 0) {
                    throw new IllegalStateException("Available license count can't be negative.");
                }
            }
            else {
                this.availableLicenseCount = null;
            }
        }

        /**
         * @return {@link #recordingCapability}
         */
        public RecordingCapability getRecordingCapability()
        {
            return recordingCapability;
        }

        /**
         * @return {@link DeviceResource} of the {@link #recordingCapability}
         */
        public DeviceResource getDeviceResource()
        {
            return recordingCapability.getDeviceResource();
        }

        /**
         * @return {@link #availableLicenseCount}
         */
        public Integer getAvailableLicenseCount()
        {
            return availableLicenseCount;
        }

        /**
         * @return maximum {@link RecordingCapability#licenseCount} for {@link #recordingCapability}
         */
        public Integer getMaximumLicenseCount()
        {
            return recordingCapability.getLicenseCount();
        }

        /**
         * @return ratio of fullness for the device (0.0 - 1.0)
         */
        public Double getFullnessRatio()
        {
            Integer maximumLicenseCount = getMaximumLicenseCount();
            if (maximumLicenseCount == null) {
                return 0.0;
            }
            else {
                return 1.0 - (double) availableLicenseCount / (double) maximumLicenseCount;
            }
        }
    }
}
