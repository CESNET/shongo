package cz.cesnet.shongo.report;

/**
 * Represents a error/warning/information/debug message in Shongo which should be reported to user,
 * resource administrator and/or domain administrator.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class Report implements Reportable
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
     * @param messageType
     * @return report message of given {@code messageType}
     */
    public abstract String getMessage(MessageType messageType);

    /**
     * @return {@link MessageType#DOMAIN_ADMIN} report message
     */
    public final String getMessage()
    {
        return getMessage(Report.MessageType.DOMAIN_ADMIN);
    }

    /**
     * @return visibility flags (e.g., {@link #VISIBLE_TO_USER}, {@link #VISIBLE_TO_DOMAIN_ADMIN})
     */
    protected int getVisibleFlags()
    {
        return 0;
    }

    /**
     * @param visibleFlags
     * @return true whether this {@link Report} contains given {@code visibleFlags}
     */
    public final boolean isVisible(int visibleFlags)
    {
        return (getVisibleFlags() & visibleFlags) == visibleFlags;
    }

    /**
     * @return {@link Resolution}
     */
    public Resolution getResolution()
    {
        return Resolution.DEFAULT;
    }

    @Override
    public String toString()
    {
        return getMessage();
    }

    @Override
    public String getReportDescription(MessageType messageType)
    {
        return getMessage(messageType);
    }

    public static final int VISIBLE_TO_USER = 1;
    public static final int VISIBLE_TO_DOMAIN_ADMIN = 2;
    public static final int VISIBLE_TO_RESOURCE_ADMIN = 4;

    /**
     * Enumeration of all possible message types.
     */
    public static enum MessageType
    {
        /**
         * Message for normal user.
         */
        USER,

        /**
         * Message for domain administrator.
         */
        DOMAIN_ADMIN,

        /**
         * Message for resource administrator.
         */
        RESOURCE_ADMIN
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

    /**
     * Represents what does the {@link Report} mean to action which cause it.
     */
    public static enum Resolution
    {
        /**
         * Not specified.
         */
        DEFAULT,

        /**
         * Means that the action which cause the {@link Report} should be tried again.
         */
        TRY_AGAIN,

        /**
         * Means that the action which cause the {@link Report} should not be tried again
         */
        STOP
    }
}
