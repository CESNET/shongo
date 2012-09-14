package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.controller.report.ReportException;
import cz.cesnet.shongo.controller.request.PersonSpecification;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.fault.TodoImplementException;

/**
 * Represents {@link ReservationTask} for a {@link PersonSpecification}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class PersonReservationTask extends ReservationTask<PersonSpecification, Reservation>
{
    /**
     * Constructor.
     *
     * @param specification sets the {@link #specification}
     * @param context       sets the {@link #context}
     */
    public PersonReservationTask(PersonSpecification specification, Context context)
    {
        super(specification, context);
    }

    @Override
    protected Reservation createReservation(PersonSpecification specification) throws ReportException
    {
        throw new TodoImplementException();
    }
}
