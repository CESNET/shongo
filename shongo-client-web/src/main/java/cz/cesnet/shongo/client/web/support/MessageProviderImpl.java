package cz.cesnet.shongo.client.web.support;

import cz.cesnet.shongo.client.web.models.UserSession;
import org.joda.time.DateTimeZone;
import org.springframework.context.MessageSource;

import javax.servlet.http.HttpServletRequest;
import java.util.Locale;

/**
 * {@link org.springframework.context.MessageSource} provided for specified {@link #locale}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class MessageProviderImpl extends MessageProvider
{
    /**
     * {@link org.springframework.context.MessageSource} to be used for retrieving messages.
     */
    private final MessageSource messageSource;

    /**
     * {@link java.util.Locale} to be used for the {@link #messageSource}
     */
    private final Locale locale;

    /**
     * {@link org.joda.time.DateTimeZone} to be used for message formatting.
     */
    private final DateTimeZone timeZone;

    /**
     * Constructor.
     *
     * @param messageSource sets the {@link #messageSource}
     * @param locale        sets the {@link #locale}
     * @param timeZone      sets the {@link #timeZone}
     */
    public MessageProviderImpl(MessageSource messageSource, Locale locale, DateTimeZone timeZone)
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
    public MessageProviderImpl(MessageSource messageSource, Locale locale)
    {
        this(messageSource, locale, DateTimeZone.getDefault());
    }

    @Override
    public MessageSource getMessageSource()
    {
        return messageSource;
    }

    @Override
    public Locale getLocale()
    {
        return locale;
    }

    @Override
    public DateTimeZone getTimeZone()
    {
        return timeZone;
    }

    @Override
    public String getMessage(String code)
    {
        return messageSource.getMessage(code, null, locale);
    }

    /**
     * @param messageSource
     * @param request
     * @return {@link MessageProvider} from given {@code request} and {@code messageSource}
     */
    public static MessageProvider fromRequest(MessageSource messageSource, HttpServletRequest request)
    {
        UserSession userSession = UserSession.getInstance(request);
        return new MessageProviderImpl(messageSource, userSession.getLocale(), userSession.getTimeZone());
    }
}
