package cz.cesnet.shongo.controller.booking.recording;

import cz.cesnet.shongo.controller.booking.specification.ExecutableServiceSpecification;
import cz.cesnet.shongo.controller.scheduler.ReservationTask;
import cz.cesnet.shongo.controller.scheduler.ReservationTaskProvider;
import cz.cesnet.shongo.controller.scheduler.SchedulerContext;

import javax.persistence.Entity;

/**
 * {@link cz.cesnet.shongo.controller.booking.specification.ExecutableServiceSpecification} for recording.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class RecordingServiceSpecification extends ExecutableServiceSpecification implements ReservationTaskProvider
{
    @Override
    protected cz.cesnet.shongo.controller.api.Specification createApi()
    {
        return cz.cesnet.shongo.controller.api.ExecutableServiceSpecification.createRecording();
    }

    @Override
    public RecordingServiceReservationTask createReservationTask(SchedulerContext schedulerContext)
    {
        RecordingServiceReservationTask recordingServiceReservationTask =
                new RecordingServiceReservationTask(schedulerContext);
        recordingServiceReservationTask.setExecutable(getExecutable());
        recordingServiceReservationTask.setEnabled(isEnabled());
        return recordingServiceReservationTask;
    }
}
