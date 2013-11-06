package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.controller.booking.executable.Executable;
import cz.cesnet.shongo.controller.booking.reservation.Reservation;

/**
 * Represents a {@link Executable} which is available by {@link AvailableReservation}.
 */
public class AvailableExecutable<E extends Executable>
{
    /**
     * {@link Executable} which is available.
     */
    private E executable;

    /**
     * {@link AvailableReservation} by which the {@link #executable} is available.
     */
    private AvailableReservation<Reservation> availableReservation;

    /**
     * Constructor.
     *
     * @param executable           sets the {@link #executable}
     * @param availableReservation sets the {@link #availableReservation}
     */
    @SuppressWarnings("unchecked")
    public AvailableExecutable(E executable, AvailableReservation availableReservation)
    {
        if (executable == null) {
            throw new IllegalArgumentException("Executable must not be null.");
        }
        if (availableReservation.getTargetReservation().getExecutable() != executable) {
            throw new IllegalArgumentException("Reservation doesn't allocate the executable.");
        }
        this.availableReservation = (AvailableReservation<Reservation>) availableReservation;
        this.executable = executable;
    }

    /**
     * @return {@link #executable}
     */
    public E getExecutable()
    {
        return executable;
    }

    /**
     * @return {@link #availableReservation}
     */
    public AvailableReservation<Reservation> getAvailableReservation()
    {
        return availableReservation;
    }

    /**
     * @return {@link #availableReservation#getOriginalReservation()}
     */
    public Reservation getOriginalReservation()
    {
        return availableReservation.getOriginalReservation();
    }

    /**
     * @return {@link #availableReservation#getType()}
     */
    public AvailableReservation.Type getType()
    {
        return availableReservation.getType();
    }
}
