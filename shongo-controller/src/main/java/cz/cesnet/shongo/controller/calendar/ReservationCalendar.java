package cz.cesnet.shongo.controller.calendar;

import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.LocalDomain;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.request.ReservationRequest;
import cz.cesnet.shongo.controller.booking.reservation.Reservation;
import cz.cesnet.shongo.controller.booking.resource.ResourceReservation;
import cz.cesnet.shongo.controller.util.iCalendar;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;

/**
 * Created by Marek Pericjta on 15.3.2017.
 */
public abstract class ReservationCalendar
{

    private String reservationId;

    public ReservationCalendar(Reservation reservation)
    {
        this.reservationId = ObjectIdentifier.formatId(reservation);
    }

    public String getReservationId()
    {
        return reservationId;
    }

    final public String getCalendarString () {
        return renderCalendarString();
    }

    abstract protected String renderCalendarString();

    abstract public String getType();


    public static class New extends ReservationCalendar
    {


        private String resourceName = null;

        private String description = null;

        private UserInformation user;

        public Interval getSlot()
        {
            return slot;
        }

        public void setSlot(Interval slot)
        {
            this.slot = slot;
        }

        private Interval slot;

        public String getDescription()
        {
            return description;
        }

        public New(Reservation reservation, ReservationRequest reservationRequest, AuthorizationManager authorizationManager)
        {

            super(reservation);
            String userId = reservation.getUserId();
            if (userId != null) {
                user = authorizationManager.getUserInformation(userId);
            }

            this.slot = reservationRequest.getSlot();

            if (reservation instanceof ResourceReservation) {
                this.resourceName = ((ResourceReservation) reservation).getResource().getName();
            }

            this.description = reservationRequest.getDescription();


        }

        protected String renderCalendarString () {
            //TODO get controller configuration
            iCalendar iCalendar = new iCalendar(/*getConfiguration().getString("domain.name"),resourceName*/);


            cz.cesnet.shongo.controller.util.iCalendar.Event event = iCalendar.addEvent(LocalDomain.getLocalDomainName(), getReservationId(), getDescription());
            event.setInterval(getSlot(), DateTimeZone.getDefault());


            return iCalendar.toString();
        }

        public String getType()
        {
            return "NEW";
        }

    }



    public static class Deleted extends ReservationCalendar
    {
        public Deleted(Reservation reservation)
        {
            super(reservation);
        }

        protected String renderCalendarString () {
            return "";
        }

        public String getType()
        {
            return "DELETED";
        }
    }


}
