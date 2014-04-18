package cz.cesnet.shongo.controller.api;

/**
 * State of the {@link Executable}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public enum ExecutableState
{
    /**
     * {@link Executable} has not been started yet.
     */
    NOT_STARTED(false),

    /**
     * {@link Executable} is already started.
     */
    STARTED(true),

    /**
     * {@link Executable} failed to start.
     */
    STARTING_FAILED(false),

    /**
     * {@link Executable} has been already stopped.
     */
    STOPPED(false),

    /**
     * {@link Executable} failed to stop.
     */
    STOPPING_FAILED(true);

    /**
     * Specifies whether the executable is available (e.g., it is started).
     */
    private final boolean available;

    /**
     * Constructor.
     *
     * @param available sets the {@link #available}
     */
    private ExecutableState(boolean available)
    {
        this.available = available;
    }

    /**
     * @return {@link #available}
     */
    public boolean isAvailable()
    {
        return available;
    }
}
