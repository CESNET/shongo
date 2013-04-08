package cz.cesnet.shongo.report;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public interface Report
{
    /**
     * @return report message
     */
    public Type getType();

    /**
     * @return report message
     */
    public String getMessage();

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
