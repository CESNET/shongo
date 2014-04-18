package cz.cesnet.shongo.report;

import org.joda.time.DateTimeZone;

import java.util.Map;

/**
 * Represents a error/warning/information/debug message in Shongo which should be reported to user,
 * resource administrator and/or domain administrator.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class AbstractReport implements Report
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
    public abstract String getMessage(UserType userType, Language language, DateTimeZone timeZone);

    /**
     * @see #getMessage(cz.cesnet.shongo.report.Report.UserType, cz.cesnet.shongo.report.Report.Language)
     */
    public final String getMessage(UserType userType, Language language)
    {
        return getMessage(userType, language, DateTimeZone.getDefault());
    }

    /**
     * @see #getMessage(cz.cesnet.shongo.report.Report.UserType, cz.cesnet.shongo.report.Report.Language)
     */
    public final String getMessage(UserType userType)
    {
        return getMessage(userType, Language.ENGLISH, DateTimeZone.getDefault());
    }

    /**
     * @return {@link cz.cesnet.shongo.report.Report.UserType#DOMAIN_ADMIN} report message
     */
    public final String getMessage()
    {
        return getMessage(UserType.DOMAIN_ADMIN, Language.ENGLISH, DateTimeZone.getDefault());
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
}
