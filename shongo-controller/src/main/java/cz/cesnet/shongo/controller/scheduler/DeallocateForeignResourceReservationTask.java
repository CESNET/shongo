package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.ForeignDomainConnectException;
import cz.cesnet.shongo.controller.api.Domain;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.booking.reservation.Reservation;
import cz.cesnet.shongo.controller.booking.reservation.ReservationManager;
import cz.cesnet.shongo.controller.booking.resource.ForeignResourceReservation;
import cz.cesnet.shongo.controller.domains.InterDomainAgent;
import cz.cesnet.shongo.controller.notification.AbstractNotification;
import org.joda.time.Interval;

import javax.persistence.EntityManager;
import java.util.List;

//TODO
public class DeallocateForeignResourceReservationTask extends DeallocateReservationTask
{
    public DeallocateForeignResourceReservationTask(ForeignResourceReservation reservation)
    {
        super(reservation);
    }

    @Override
    protected List<AbstractNotification> deallocate(Interval slot, Scheduler.Result result, EntityManager entityManager, ReservationManager reservationManager, AuthorizationManager authorizationManager) throws SchedulerException
    {
        ForeignResourceReservation reservation = getReservation();
        Domain domain = reservation.getDomain().toApi();
        if (InterDomainAgent.getInstance().getConnector().deallocateReservation(domain, reservation.getForeignReservationRequestId())) {
            return super.deallocate(slot, result, entityManager, reservationManager, authorizationManager);
        }
        else {
            throw new TodoImplementException("process returned error");
        }
    }

    @Override
    public ForeignResourceReservation getReservation()
    {
        return (ForeignResourceReservation) super.getReservation();
    }
}
