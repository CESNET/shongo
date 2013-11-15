package cz.cesnet.shongo.controller.booking.recording;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.booking.EntityIdentifier;
import cz.cesnet.shongo.controller.booking.TechnologySet;
import cz.cesnet.shongo.controller.booking.executable.Executable;
import cz.cesnet.shongo.controller.booking.executable.ExecutableService;
import cz.cesnet.shongo.controller.booking.reservation.Reservation;
import cz.cesnet.shongo.controller.booking.reservation.ReservationManager;
import cz.cesnet.shongo.controller.booking.resource.DeviceResource;
import cz.cesnet.shongo.controller.booking.room.*;
import cz.cesnet.shongo.controller.cache.Cache;
import cz.cesnet.shongo.controller.cache.ResourceCache;
import cz.cesnet.shongo.controller.scheduler.*;
import cz.cesnet.shongo.controller.util.RangeSet;
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
    private Executable executable;

    private boolean enabled;

    /**
     * Constructor.
     */
    public RecordingServiceReservationTask(SchedulerContext schedulerContext)
    {
        super(schedulerContext);
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
    protected Reservation allocateReservation() throws SchedulerException
    {
        if (executable == null) {
            throw new IllegalStateException("Executable must be set.");
        }

        Interval interval = getInterval();
        Cache cache = getCache();
        ResourceCache resourceCache = cache.getResourceCache();
        Interval executableSlot = executable.getSlot();

        // Check interval
        if (!executableSlot.contains(interval)) {
            throw new SchedulerReportSet.ExecutableServiceInvalidSlotException(executableSlot, interval);
        }

        // Check executable
        Set<Technology> technologies = new HashSet<Technology>();

        if (executable instanceof RecordableEndpoint) {
            RecordableEndpoint recordableEndpoint = (RecordableEndpoint) executable;
            DeviceResource deviceResource = recordableEndpoint.getDeviceResource();
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
                    RoomReservationTask roomReservationTask = new RoomReservationTask(schedulerContext, 1, false);
                    roomReservationTask.setRoomProviderCapability(roomProviderCapability);
                    addChildReservation(roomReservationTask);
                }
            }
            else {
                // Check whether licenses aren't unlimited (otherwise the rooms are always recordable
                // and we shouldn't allocate the recording service for it)
                if (recordingCapability.getLicenseCount() == null) {
                    throw new SchedulerReportSet.RoomEndpointAlwaysRecordableException(
                            EntityIdentifier.formatId(executable));
                }
            }
        }
        else {
            throw new TodoImplementException(
                    executable.getClass() + " doesn't implement " + RecordableEndpoint.class.getSimpleName() + ".");
        }

        // Find matching recorders
        List<AvailableRecorder> availableRecorders = new LinkedList<AvailableRecorder>();
        beginReport(new SchedulerReportSet.FindingAvailableResourceReport());
        for (RecordingCapability recordingCapability : cache.getRecorders()) {
            DeviceResource deviceResource = recordingCapability.getDeviceResource();
            if (technologies.size() > 0 && !deviceResource.hasTechnologies(technologies)) {
                continue;
            }

            // Get available recorder
            EntityManager entityManager = schedulerContext.getEntityManager();
            ReservationManager reservationManager = new ReservationManager(entityManager);
            List<RecordingServiceReservation> roomReservations =
                    reservationManager.getRecordingServiceReservations(recordingCapability, interval);
            schedulerContext.applyReservations(recordingCapability.getId(),
                    roomReservations, RecordingServiceReservation.class);
            RangeSet<RecordingServiceReservation, DateTime> rangeSet =
                    new RangeSet<RecordingServiceReservation, DateTime>();
            for (RecordingServiceReservation roomReservation : roomReservations) {
                rangeSet.add(roomReservation, roomReservation.getSlotStart(), roomReservation.getSlotEnd());
            }
            List<RangeSet.Bucket> roomBuckets = new LinkedList<RangeSet.Bucket>();
            roomBuckets.addAll(rangeSet.getBuckets(interval.getStart(), interval.getEnd()));
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
            if (availableRecorder.getAvailableLicenseCount() == 0) {
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
            beginReport(new SchedulerReportSet.AllocatingResourceReport(deviceResource));

            // Check whether alias provider can be allocated
            try {
                resourceCache.checkCapabilityAvailable(recordingCapability, schedulerContext);
            }
            catch (SchedulerException exception) {
                endReportError(exception.getReport());
                continue;
            }

            // Allocate recording service
            RecordingService recordingService = new RecordingService();
            recordingService.setRecordingCapability(recordingCapability);
            recordingService.setExecutable(executable);
            recordingService.setSlot(interval);
            if (enabled) {
                recordingService.setState(ExecutableService.State.PREPARED);
            }
            else {
                recordingService.setState(ExecutableService.State.NOT_ACTIVE);
            }

            // Allocate recording reservation
            RecordingServiceReservation recordingServiceReservation = new RecordingServiceReservation();
            recordingServiceReservation.setRecordingCapability(recordingCapability);
            recordingServiceReservation.setSlot(interval);
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
        private final int availableLicenseCount;

        /**
         * Constructor.
         *
         * @param recordingCapability sets the {@link #recordingCapability}
         * @param usedLicenseCount to be used for computing {@link #availableLicenseCount}
         */
        private AvailableRecorder(RecordingCapability recordingCapability, int usedLicenseCount)
        {
            this.recordingCapability = recordingCapability;
            this.availableLicenseCount = recordingCapability.getLicenseCount() - usedLicenseCount;
            if (this.availableLicenseCount < 0) {
                throw new IllegalStateException("Available license count can't be negative.");
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
        public int getAvailableLicenseCount()
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
            return 1.0 - (double) getAvailableLicenseCount() / (double) getMaximumLicenseCount();
        }
    }
}
