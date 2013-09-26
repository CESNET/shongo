package cz.cesnet.shongo.client.web.support;

import org.joda.time.DateTimeZone;
import org.springframework.context.MessageSource;

import java.util.Locale;

/**
 * {@link MessageSource} provided for specified {@link #locale}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class MessageProvider
{
    /**
     * {@link MessageSource} to be used for retrieving messages.
     */
    private final MessageSource messageSource;

    /**
     * {@link Locale} to be used for the {@link #messageSource}
     */
    private final Locale locale;

    /**
     * {@link DateTimeZone} to be used for message formatting.
     */
    private final DateTimeZone timeZone;

    /**
     * Constructor.
     *
     * @param messageSource sets the {@link #messageSource}
     * @param locale        sets the {@link #locale}
     * @param timeZone      sets the {@link #timeZone}
     */
    public MessageProvider(MessageSource messageSource, Locale locale, DateTimeZone timeZone)
    {
        this.messageSource = messageSource;
        this.locale = locale;
        this.timeZone = timeZone;
    }

    /**
     * Constructor.
     *
     * @param messageSource sets the {@link #messageSource}
     * @param locale        sets the {@link #locale}
     */
    public MessageProvider(MessageSource messageSource, Locale locale)
    {
        this(messageSource, locale, DateTimeZone.getDefault());
    }

    /**
     * @return {@link #messageSource}
     */
    public MessageSource getMessageSource()
    {
        return messageSource;
    }

    /**
     * @return {@link #locale}
     */
    public Locale getLocale()
    {
        return locale;
    }

    /**
     * @param code
     * @return message for given {@code code}
     */
    public String getMessage(String code)
    {
        return messageSource.getMessage(code, null, locale);
    }

    /**
     * @return {@link #timeZone}
     */
    public DateTimeZone getTimeZone()
    {
        return timeZone;
    }
}
