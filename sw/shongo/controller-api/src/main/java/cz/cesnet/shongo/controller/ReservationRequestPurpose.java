package cz.cesnet.shongo.controller;

/**
 * A purpose for which the reservation will be used.
 */
public enum ReservationRequestPurpose
{
    /**
     * Reservation will be used e.g., for user purposes.
     */
    USER(0, true, false),

    /**
     * Reservation will be used e.g., for research purposes.
     */
    SCIENCE(0, true, false),

    /**
     * Reservation will be used for education purposes (e.g., for a lecture).
     */
    EDUCATION(0, true, false),

    /**
     * Reservation is used for owner purposes (no executable will be allocated).
     */
    OWNER(1, false, true),

    /**
     * Reservation is used for maintenance purposes (no executable will be allocated).
     */
    MAINTENANCE(1, false, true);

    /**
     * Specifies priority for the scheduler.
     */
    private Integer priority;

    /**
     * Specifies whether scheduler should allocate executable for allocated reservations based on the request.
     */
    private boolean executableAllowed;

    /**
     * Specifies whether scheduler can allocate only owned resources (and if it should not check maximum duration
     * and future).
     */
    private boolean byOwner;

    /**
     * Constructor.
     *
     * @param executableAllowed sets the {@link #executableAllowed}
     */
    private ReservationRequestPurpose(int priority, boolean executableAllowed, boolean byOwner)
    {
        this.priority = priority;
        this.executableAllowed = executableAllowed;
        this.byOwner = byOwner;
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
     * @return {@link #byOwner}
     */
    public boolean isByOwner()
    {
        return byOwner;
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
