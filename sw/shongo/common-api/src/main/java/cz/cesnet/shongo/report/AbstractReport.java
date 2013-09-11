package cz.cesnet.shongo.report;

import java.util.Map;

/**
 * Represents a error/warning/information/debug message in Shongo which should be reported to user,
 * resource administrator and/or domain administrator.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class AbstractReport implements Report, Reportable
{
    @Override
    public abstract String getUniqueId();

    @Override
    public String getName()
    {
        return getClass().getSimpleName();
    }

    @Override
    public abstract Type getType();

    @Override
    public abstract Map<String, Object> getParameters();

    @Override
    public abstract String getMessage(MessageType messageType, Language language);

    /**
     * @return {@link MessageType#DOMAIN_ADMIN} report message
     */
    public final String getMessage()
    {
        return getMessage(MessageType.DOMAIN_ADMIN, Language.ENGLISH);
    }

    @Override
    public int getVisibleFlags()
    {
        return 0;
    }

    /**
     * @param visibleFlags
     * @return true whether this {@link AbstractReport} contains given {@code visibleFlags}
     */
    public final boolean isVisible(int visibleFlags)
    {
        return (getVisibleFlags() & visibleFlags) == visibleFlags;
    }

    @Override
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
        return getMessage(messageType, Language.ENGLISH);
    }
}
