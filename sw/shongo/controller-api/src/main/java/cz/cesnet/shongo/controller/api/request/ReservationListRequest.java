package cz.cesnet.shongo.controller.api.request;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.api.Reservation;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.Specification;

import java.util.*;

/**
 * {@link ListRequest} for reservations.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationListRequest extends ListRequest
{
    private Collection<String> reservationIds = new LinkedList<String>();

    private String reservationRequestId;

    private Set<Class<? extends Reservation>> reservationClasses = new HashSet<Class<? extends Reservation>>();

    private Set<Technology> technologies = new HashSet<Technology>();

    public ReservationListRequest()
    {
    }

    public ReservationListRequest(SecurityToken securityToken)
    {
        super(securityToken);
    }

    public ReservationListRequest(SecurityToken securityToken, String reservationRequestId)
    {
        super(securityToken);
        this.reservationRequestId = reservationRequestId;
    }

    public Collection<String> getReservationIds()
    {
        return reservationIds;
    }

    public void setReservationIds(Collection<String> reservationIds)
    {
        this.reservationIds = reservationIds;
    }

    public void addReservationId(String reservationId)
    {
        this.reservationIds.add(reservationId);
    }

    public String getReservationRequestId()
    {
        return reservationRequestId;
    }

    public void setReservationRequestId(String reservationRequestId)
    {
        this.reservationRequestId = reservationRequestId;
    }

    public Set<Class<? extends Reservation>> getReservationClasses()
    {
        return Collections.unmodifiableSet(reservationClasses);
    }

    public void setReservationClasses(Set<Class<? extends Reservation>> reservationClasses)
    {
        this.reservationClasses = reservationClasses;
    }

    public void addReservationClass(Class<? extends Reservation> reservationClass)
    {
        this.reservationClasses.add(reservationClass);
    }

    public Set<Technology> getTechnologies()
    {
        return Collections.unmodifiableSet(technologies);
    }

    public void setTechnologies(Set<Technology> technologies)
    {
        this.technologies = technologies;
    }

    public void addTechnology(Technology technology)
    {
        technologies.add(technology);
    }
}
