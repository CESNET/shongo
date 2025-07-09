package cz.cesnet.shongo.client.web.support.resolvers;

import cz.cesnet.shongo.client.web.models.UserSession;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.SimpleLocaleContext;
import org.springframework.context.i18n.TimeZoneAwareLocaleContext;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Locale;
import java.util.TimeZone;

/**
 * {@link SessionLocaleResolver} which loads/stores the {@link Locale} from/to {@link UserSession}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class LocaleResolver extends SessionLocaleResolver
{
    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        return determineLocale(request);
    }

    @Override
    public LocaleContext resolveLocaleContext(final HttpServletRequest request) {
        return new SimpleLocaleContext(determineLocale(request));
    }


    @Override
    public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
        UserSession userSession = UserSession.getInstance(request);
        if (!locale.equals(userSession.getLocale())) {
            userSession.setLocale(locale);
            userSession.update(request, null);
        }
    }

    public Locale determineLocale (HttpServletRequest request) {
        UserSession userSession = UserSession.getInstance(request);
        Locale locale = userSession.getLocale();
        if (locale == null) {
            locale = determineDefaultLocale(request);
            userSession.setLocale(locale);
            userSession.update(request, null);
        }
        return locale;
    }
}
