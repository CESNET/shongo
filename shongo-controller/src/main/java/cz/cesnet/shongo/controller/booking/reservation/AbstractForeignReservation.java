package cz.cesnet.shongo.controller.booking.reservation;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.booking.domain.Domain;
import cz.cesnet.shongo.controller.booking.request.ReservationRequest;
import cz.cesnet.shongo.controller.booking.resource.Resource;
import cz.cesnet.shongo.controller.scheduler.SchedulerContext;

import javax.persistence.*;

/**
 * Represents a {@link cz.cesnet.shongo.controller.booking.reservation.Reservation} for a foreign {@link Resource}.
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class AbstractForeignReservation extends TargetedReservation
{
    private Domain domain;

    private String foreignReservationRequestId;

    private boolean complete = false;

    @ManyToOne
    @JoinColumn(name = "domain_id")
    public Domain getDomain()
    {
        return domain;
    }

    public void setDomain(Domain domain)
    {
        this.domain = domain;
    }

    @Column
    public String getForeignReservationRequestId()
    {
        return foreignReservationRequestId;
    }

    public void setForeignReservationRequestId(String foreignReservationRequestId)
    {
        this.foreignReservationRequestId = foreignReservationRequestId;
    }

    @Column
    public boolean isComplete()
    {
        return complete;
    }

    public void setComplete(boolean complete)
    {
        this.complete = complete;
    }

    public void setCompletedByState(SchedulerContext schedulerContext, cz.cesnet.shongo.controller.api.domains.response.Reservation reservation)
    {
        switch (reservation.getStatus()) {
            case OK:
                setSlot(reservation.getSlotStart(), reservation.getSlotEnd());
                if (reservation.isAllocated()) {
                    setComplete(true);
                    schedulerContext.setRequestWantedState(ReservationRequest.AllocationState.ALLOCATED);
                }
                break;
            case FAILED:
                setComplete(true);
                setSlotStart(null);
                setSlotEnd(null);
                schedulerContext.setRequestWantedState(ReservationRequest.AllocationState.ALLOCATION_FAILED);
                break;
            case ERROR:
                throw new TodoImplementException();
        }
    }
}
