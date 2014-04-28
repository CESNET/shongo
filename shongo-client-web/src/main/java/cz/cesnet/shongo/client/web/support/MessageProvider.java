package cz.cesnet.shongo.client.web.support;

import org.joda.time.DateTimeZone;
import org.springframework.context.MessageSource;

import java.util.Locale;

/**
 * {@link MessageSource} provided for single {@link Locale} and {@link DateTimeZone}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class MessageProvider
{
    /**
     * @return {@link MessageSource}
     */
    public abstract MessageSource getMessageSource();

    /**
     * @return {@link Locale}
     */
    public abstract Locale getLocale();

    /**
     * @param code
     * @return message for given {@code code}
     */
    public String getMessage(String code)
    {
        return getMessageSource().getMessage(code, null, getLocale());
    }

    /**
     * @param code
     * @return message for given {@code code}
     */
    public String getMessage(String code, Object... arguments)
    {
        return getMessageSource().getMessage(code, arguments, getLocale());
    }

    /**
     * @return {@link DateTimeZone}
     */
    public abstract DateTimeZone getTimeZone();
}
