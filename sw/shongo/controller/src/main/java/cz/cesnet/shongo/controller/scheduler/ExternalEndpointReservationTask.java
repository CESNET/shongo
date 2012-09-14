package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.controller.report.ReportException;
import cz.cesnet.shongo.controller.request.ExternalEndpointSpecification;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.fault.TodoImplementException;

/**
 * Represents {@link ReservationTask} for a {@link ExternalEndpointSpecification}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ExternalEndpointReservationTask extends ReservationTask<ExternalEndpointSpecification, Reservation>
{
    /**
     * Constructor.
     *
     * @param specification sets the {@link #specification}
     * @param context       sets the {@link #context}
     */
    public ExternalEndpointReservationTask(ExternalEndpointSpecification specification, Context context)
    {
        super(specification, context);
    }

    @Override
    protected Reservation createReservation(ExternalEndpointSpecification specification) throws ReportException
    {
        throw new TodoImplementException();
    }
}
