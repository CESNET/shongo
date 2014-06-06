package cz.cesnet.shongo.controller.booking.recording;

import cz.cesnet.shongo.controller.booking.specification.ExecutableServiceSpecification;
import cz.cesnet.shongo.controller.scheduler.ReservationTaskProvider;
import cz.cesnet.shongo.controller.scheduler.SchedulerContext;
import cz.cesnet.shongo.controller.scheduler.SchedulerException;
import org.joda.time.Interval;

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
        return new cz.cesnet.shongo.controller.api.RecordingServiceSpecification();
    }

    @Override
    public RecordingServiceReservationTask createReservationTask(SchedulerContext schedulerContext, Interval slot)
            throws SchedulerException
    {
        RecordingServiceReservationTask recordingServiceReservationTask =
                new RecordingServiceReservationTask(schedulerContext, slot);
        recordingServiceReservationTask.setResource(getResource());
        recordingServiceReservationTask.setExecutable(getExecutable());
        recordingServiceReservationTask.setEnabled(isEnabled());
        return recordingServiceReservationTask;
    }
}
