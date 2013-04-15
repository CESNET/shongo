package cz.cesnet.shongo.report;

/**
 * Represents a error/warning/information/debug message in Shongo which should be reported to user,
 * resource administrator and/or domain administrator.
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
     * @return true whether this {@link Report} should be sent to domain administrator by email,
     *         false otherwise
     */
    public boolean isVisibleToDomainAdminViaEmail()
    {
        return false;
    }

    @Override
    public String toString()
    {
        return getMessage();
    }

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
