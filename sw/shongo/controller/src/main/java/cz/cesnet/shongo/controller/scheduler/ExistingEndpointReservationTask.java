package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.controller.report.ReportException;
import cz.cesnet.shongo.controller.request.ExistingEndpointSpecification;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.resource.Resource;
import cz.cesnet.shongo.controller.scheduler.report.ResourceNotAllocatableReport;
import cz.cesnet.shongo.controller.scheduler.report.ResourceNotAvailableReport;
import cz.cesnet.shongo.controller.scheduler.report.ResourceRequestedMultipleTimesReport;
import cz.cesnet.shongo.fault.TodoImplementException;

/**
 * Represents {@link ReservationTask} for a {@link ExistingEndpointSpecification}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ExistingEndpointReservationTask extends ReservationTask<ExistingEndpointSpecification, Reservation>
{
    /**
     * Constructor.
     *
     * @param specification sets the {@link #specification}
     * @param context       sets the {@link #context}
     */
    public ExistingEndpointReservationTask(ExistingEndpointSpecification specification, Context context)
    {
        super(specification, context);
    }

    @Override
    protected Reservation createReservation(ExistingEndpointSpecification specification) throws ReportException
    {
        Resource resource = specification.getResource();
        if (getCacheTransaction().containsResource(resource)) {
            // Same resource is requested multiple times
            throw new ResourceRequestedMultipleTimesReport(resource).exception();
        }
        if (!resource.isAllocatable()) {
            // Requested resource cannot be allocated
            throw new ResourceNotAllocatableReport(resource).exception();
        }
        if (!getCache().isResourceAvailable(resource, getInterval(), getCacheTransaction())) {
            // Requested resource is not available in requested slot
            throw new ResourceNotAvailableReport(resource).exception();
        }

        throw new TodoImplementException();
    }
}
