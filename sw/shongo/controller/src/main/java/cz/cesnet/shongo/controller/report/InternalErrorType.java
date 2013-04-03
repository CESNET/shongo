package cz.cesnet.shongo.controller.report;

/**
 * Type of internal errors where they can happen.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public enum InternalErrorType
{
    AUTHORIZATION("Authorization"),

    WORKER("Worker"),

    PREPROCESSOR("Preprocessor"),

    SCHEDULER("Scheduler"),

    EXECUTOR("Executor"),

    NOTIFICATION("Notification");

    /**
     * Name of the {@link InternalErrorType}.
     */
    private String name;

    /**
     * Constructor.
     *
     * @param name sets the {@link #name}
     */
    private InternalErrorType(String name)
    {
        this.name = name;
    }

    /**
     * @return {@link #name}
     */
    public String getName()
    {
        return name;
    }
}
