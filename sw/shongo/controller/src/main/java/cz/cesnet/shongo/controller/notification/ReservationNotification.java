package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.controller.booking.reservation.Reservation;

import javax.persistence.*;

/**
 * Represents a {@link Notification} for a {@link Reservation}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class ReservationNotification extends Notification
{
    /**
     * {@link Reservation} for which the {@link ReservationNotification} is created.
     */
    private Reservation reservation;

    /**
     * @see Type
     */
    private Type type;

    /**
     * @return {@link #reservation}
     */
    @ManyToOne
    public Reservation getReservation()
    {
        return reservation;
    }

    /**
     * @param reservation sets the {@link #reservation}
     */
    public void setReservation(Reservation reservation)
    {
        this.reservation = reservation;
    }

    /**
     * @return {@link #type}
     */
    @Column
    @Enumerated(EnumType.STRING)
    public Type getType()
    {
        return type;
    }

    /**
     * @param type sets the {@link #type}
     */
    public void setType(Type type)
    {
        this.type = type;
    }

    /**
     * Available types of {@link ReservationNotification}s.
     */
    public static enum Type
    {
        /**
         * {@link ReservationNotification} for new {@link Reservation}s.
         */
        NEW,

        /**
         * {@link ReservationNotification} for deleted {@link Reservation}s.
         */
        DELETED
    }
}
