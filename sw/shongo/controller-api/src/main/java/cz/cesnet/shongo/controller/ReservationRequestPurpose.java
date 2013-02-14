package cz.cesnet.shongo.controller;

/**
 * A purpose for which the reservation will be used.
 */
public enum ReservationRequestPurpose
{
    /**
     * Reservation will be used e.g., for research purposes.
     */
    SCIENCE(0, true),

    /**
     * Reservation will be used for education purposes (e.g., for a lecture).
     */
    EDUCATION(0, true),

    /**
     * Reservation is used for owner purposes (no executable will be allocated).
     */
    EXCLUSION(1, false),

    /**
     * Reservation is used for maintenance purposes (no executable will be allocated).
     */
    MAINTENANCE(1, false);

    /**
     * Specifies priority for the scheduler.
     */
    private Integer priority;

    /**
     * Specifies whether scheduler should allocate executable for allocated reservations based on the request.
     */
    private boolean executableAllowed;

    /**
     * Constructor.
     *
     * @param executableAllowed sets the {@link #executableAllowed}
     */
    private ReservationRequestPurpose(int priority, boolean executableAllowed)
    {
        this.priority = priority;
        this.executableAllowed = executableAllowed;
    }

    /**
     * @return {@link #priority}
     */
    public Integer getPriority()
    {
        return priority;
    }

    /**
     * @return {@link #executableAllowed}
     */
    public boolean isExecutableAllowed()
    {
        return executableAllowed;
    }

    /**
     * @param purpose
     * @return comparison result based on {@link #priority}
     */
    public int priorityCompareTo(ReservationRequestPurpose purpose)
    {
        return -getPriority().compareTo(purpose.getPriority());
    }
}
