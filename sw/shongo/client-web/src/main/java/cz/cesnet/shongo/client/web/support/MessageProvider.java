package cz.cesnet.shongo.client.web.support;

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
     * Constructor.
     *
     * @param messageSource sets the {@link #messageSource}
     * @param locale        sets the {@link #locale}
     */
    public MessageProvider(MessageSource messageSource, Locale locale)
    {
        this.messageSource = messageSource;
        this.locale = locale;
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
}
