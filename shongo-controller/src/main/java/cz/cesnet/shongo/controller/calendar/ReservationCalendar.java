package cz.cesnet.shongo.controller.calendar;

import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.reservation.Reservation;
import cz.cesnet.shongo.hibernate.PersistentDateTime;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import jakarta.persistence.*;
import java.util.Objects;


/**
 * Represents calendar notification for a {@link Reservation}.
 *
 * @author Marek Perichta <mperichta@cesnet.cz>
 */
@Entity
@Inheritance(strategy=InheritanceType.JOINED)
public abstract class ReservationCalendar extends PersistentObject {

    private String reservationId;

    private String remoteCalendarName;

    protected ReservationCalendar (){}

    public ReservationCalendar(Reservation reservation) {
        this.reservationId = ObjectIdentifier.formatId(reservation);
        this.remoteCalendarName = reservation.getAllocatedResource().getRemoteCalendarName();

    }

    @Column
    public String getReservationId()
    {
        return reservationId;
    }

    @Column
    public String getRemoteCalendarName() {
        return remoteCalendarName;
    }

    public void setReservationId(String reservationId) {
        this.reservationId = reservationId;
    }

    public void setRemoteCalendarName(String remoteCalendarName) {
        this.remoteCalendarName = remoteCalendarName;
    }

    @Id
    @SequenceGenerator(name = "calendar_id", sequenceName = "calendar_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "calendar_id")
    @Override
    public Long getId()
    {
        return id;
    }

    @Transient
    abstract public Type getType();

    public enum Type {
        NEW,
        DELETED
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ReservationCalendar that = (ReservationCalendar) o;

        if (reservationId != null ? !reservationId.equals(that.reservationId) : that.reservationId != null)
            return false;
        return remoteCalendarName != null ? remoteCalendarName.equals(that.remoteCalendarName) : that.remoteCalendarName == null;
    }

    @Override
    public int hashCode() {
        int result = reservationId != null ? reservationId.hashCode() : 0;
        result = 31 * result + (remoteCalendarName != null ? remoteCalendarName.hashCode() : 0);
        return result;
    }

    @Entity
    public static class New extends ReservationCalendar {

        private String resourceName = null;

        private String description = null;

        private DateTime slotStart = null;

        private DateTime slotEnd = null;

        private String organizerName = null;

        private String organizerEmail = null;

        protected New () {

        }

        public New(Reservation reservation, AuthorizationManager authorizationManager) {

            super(reservation);
            if (reservation == null || authorizationManager == null) {
                return;
            }
            String userId = reservation.getUserId();
            if (userId != null) {
                UserInformation organizer = authorizationManager.getUserInformation(userId);
                organizerEmail = organizer.getPrimaryEmail();
                organizerName = organizer.getFullName();
            }
            this.slotStart = reservation.getSlotStart();
            this.slotEnd = reservation.getSlotEnd();
            this.resourceName = reservation.getAllocatedResource().getName();
            this.description = reservation.getAllocation().getReservationRequest().getDescription();
        }

        @Transient
        public Interval getSlot () {
            return new Interval(slotStart, slotEnd);
        }

        @Column
        @org.hibernate.annotations.Type(value = PersistentDateTime.class)
        public DateTime getSlotStart() {
            return slotStart;
        }

        @Column
        @org.hibernate.annotations.Type(value = PersistentDateTime.class)
        public DateTime getSlotEnd () {
            return slotEnd;
        }

        @Column
        public String getDescription() {
            return description;
        }

        @Column
        public String getResourceName() {
            return resourceName;
        }

        @Column
        public String getOrganizerName() {
            return organizerName;
        }

        @Column
        public String getOrganizerEmail() {
            return organizerEmail;
        }

        public void setResourceName(String resourceName) {
            this.resourceName = resourceName;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public void setOrganizerName(String organizerName) {
            this.organizerName = organizerName;
        }

        public void setOrganizerEmail(String organizerEmail) {
            this.organizerEmail = organizerEmail;
        }

        public void setSlotStart(DateTime slotStart) {
            this.slotStart = slotStart;
        }

        public void setSlotEnd(DateTime slotEnd) {
            this.slotEnd = slotEnd;
        }

        @Transient
        public Type getType() {
            return Type.NEW;
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;

            New aNew = (New) o;

            if (resourceName != null ? !resourceName.equals(aNew.resourceName) : aNew.resourceName != null)
                return false;
            if (description != null ? !description.equals(aNew.description) : aNew.description != null) return false;
            if (slotStart != null ? !slotStart.equals(aNew.slotStart) : aNew.slotStart != null) return false;
            if (slotEnd != null ? !slotEnd.equals(aNew.slotEnd) : aNew.slotEnd != null) return false;
            if (organizerName != null ? !organizerName.equals(aNew.organizerName) : aNew.organizerName != null)
                return false;
            return organizerEmail != null ? organizerEmail.equals(aNew.organizerEmail) : aNew.organizerEmail == null;
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + (resourceName != null ? resourceName.hashCode() : 0);
            result = 31 * result + (description != null ? description.hashCode() : 0);
            result = 31 * result + (slotStart != null ? slotStart.hashCode() : 0);
            result = 31 * result + (slotEnd != null ? slotEnd.hashCode() : 0);
            result = 31 * result + (organizerName != null ? organizerName.hashCode() : 0);
            result = 31 * result + (organizerEmail != null ? organizerEmail.hashCode() : 0);
            return result;
        }
    }

    @Entity
    public static class Deleted extends ReservationCalendar {

        protected Deleted() {}

        public Deleted(Reservation reservation) {
            super(reservation);
        }

        @Transient
        public Type getType() {
            return Type.DELETED;
        }

        @Override
        public boolean equals (Object object) {
            return super.equals(object);
        }

        public int hashCode () {
            return super.hashCode();
        }


    }

}
