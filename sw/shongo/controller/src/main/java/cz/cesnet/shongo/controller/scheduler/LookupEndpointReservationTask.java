package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.report.ReportException;
import cz.cesnet.shongo.controller.request.LookupEndpointSpecification;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.scheduler.report.ResourceNotFoundReport;
import cz.cesnet.shongo.fault.TodoImplementException;

import java.util.List;
import java.util.Set;

/**
 * Represents {@link ReservationTask} for a {@link LookupEndpointSpecification}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class LookupEndpointReservationTask extends ReservationTask<LookupEndpointSpecification, Reservation>
{
    /**
     * Constructor.
     *
     * @param specification sets the {@link #specification}
     * @param context       sets the {@link #context}
     */
    public LookupEndpointReservationTask(LookupEndpointSpecification specification, Context context)
    {
        super(specification, context);
    }

    @Override
    protected Reservation createReservation(LookupEndpointSpecification specification) throws ReportException
    {
        Set<Technology> technologies = specification.getTechnologies();

        // Lookup device resources
        List<DeviceResource> deviceResources = getCache().findAvailableTerminal(getInterval(), technologies,
                getCacheTransaction());

        // Select first available device resource
        // TODO: Select best resource based on some criteria
        DeviceResource deviceResource = null;
        for (DeviceResource possibleDeviceResource : deviceResources) {
            deviceResource = possibleDeviceResource;
            break;
        }

        // If some was found
        if (deviceResource != null) {
            throw new TodoImplementException();
        }
        else {
            throw new ResourceNotFoundReport(technologies).exception();
        }
    }
}
