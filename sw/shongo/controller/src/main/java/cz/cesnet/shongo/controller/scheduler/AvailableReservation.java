package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.controller.booking.reservation.ExistingReservation;
import cz.cesnet.shongo.controller.booking.reservation.Reservation;

/**
 * Represents a {@link Reservation} which can be reused/reallocated by the scheduler (the resources allocated by
 * the {@link #targetReservation} are considered as available).
 */
public class AvailableReservation<R extends Reservation>
{
    /**
     * Represents an original {@link Reservation} which can be reused (it can be {@link ExistingReservation}).
     */
    private final Reservation originalReservation;

    /**
     * Represents a target reservation which is referenced (it allocates something, e.g., room or value
     * and thus it cannot be the {@link ExistingReservation}).
     */
    private final Reservation targetReservation;

    /**
     * @see Type
     */
    private final Type type;

    /**
     * Constructor.
     *
     * @param reservation sets the {@link #originalReservation}
     * @param type        sets the {@link #type}
     */
    private AvailableReservation(Reservation reservation, Type type)
    {
        if (reservation == null) {
            throw new IllegalArgumentException("Reservation must not be null.");
        }
        if (type == null) {
            throw new IllegalArgumentException("Type must not be null.");
        }
        this.originalReservation = reservation;
        this.targetReservation = reservation.getTargetReservation();
        this.type = type;
    }

    /**
     * @param reservation sets the {@link #originalReservation}
     * @param type        sets the {@link #type}
     * @return new instance of {@link AvailableReservation}
     */
    public static <T extends Reservation> AvailableReservation<T> create(T reservation, Type type)
    {
        return new AvailableReservation<T>(reservation, type);
    }

    /**
     * @param reservation sets the {@link #originalReservation}
     * @return new instance of {@link AvailableReservation} with {@link Type#REUSABLE} {@link #type}
     */
    public static <T extends Reservation> AvailableReservation<T> create(T reservation)
    {
        return create(reservation, Type.REUSABLE);
    }

    /**
     * @return {@link #originalReservation}
     */
    public Reservation getOriginalReservation()
    {
        return originalReservation;
    }

    /**
     * @return {@link #targetReservation}
     */
    @SuppressWarnings("unchecked")
    public R getTargetReservation()
    {
        return (R) targetReservation;
    }

    /**
     * @return {@link #type}
     */
    public Type getType()
    {
        return type;
    }

    /**
     * @param type
     * @return true whether {@link #type} is of given {@code type},
     *         false otherwise
     */
    public boolean isType(Type type)
    {
        return this.type.equals(type);
    }

    /**
     * @param reservationType
     * @param <T>
     * @return this {@link AvailableReservation} cast to given {@code reservationType}
     */
    @SuppressWarnings("unchecked")
    public <T extends Reservation> AvailableReservation<T> cast(Class<T> reservationType)
    {
        if (!reservationType.isInstance(targetReservation)) {
            throw new IllegalArgumentException("Target reservation is of type " + targetReservation.getClass().getName()
                    + " which is not compatible with " + reservationType.getName());
        }
        return (AvailableReservation<T>) this;
    }

    @Override
    public int hashCode()
    {
        return originalReservation.hashCode();
    }

    @Override
    public boolean equals(Object object)
    {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        AvailableReservation availableReservation = (AvailableReservation) object;
        return originalReservation.equals(availableReservation.originalReservation);
    }

    public boolean isExistingReservationRequired()
    {
        return type.equals(Type.REUSABLE);
    }

    public boolean isModifiable()
    {
        return type.equals(Type.REALLOCATABLE);
    }

    /**
     * Type of {@link AvailableReservation}.
     */
    public static enum Type
    {
        /**
         * {@link AvailableReservation#targetReservation} can be reused only as it is.
         */
        REUSABLE,

        /**
         * {@link AvailableReservation#targetReservation} can be reused or can be reallocated
         * (e.g., the number of room licenses can be increased).
         */
        REALLOCATABLE
    }
}
