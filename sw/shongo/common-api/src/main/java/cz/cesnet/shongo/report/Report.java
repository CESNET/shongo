package cz.cesnet.shongo.report;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class Report
{
    /**
     * @return name of the report
     */
    public String getName()
    {
        return getClass().getSimpleName();
    }

    /**
     * @return report message
     */
    public abstract Type getType();

    /**
     * @return report message
     */
    public abstract String getMessage();

    /**
     * Enumeration of all possible {@link Report} types.
     */
    public static enum Type
    {
        /**
         * Represent a failure.
         */
        ERROR,

        /**
         * Represents a warning.
         */
        WARNING,

        /**
         * Represents a information.
         */
        INFORMATION,

        /**
         * Represents a debug information.
         */
        DEBUG
    }
}
