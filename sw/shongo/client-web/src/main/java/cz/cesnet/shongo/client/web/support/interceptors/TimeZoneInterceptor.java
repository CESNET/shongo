package cz.cesnet.shongo.client.web.support.interceptors;

import com.google.common.base.Strings;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;
import org.springframework.web.servlet.view.InternalResourceView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Interceptor for detection of timezone.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class TimeZoneInterceptor extends HandlerInterceptorAdapter
{
    private static Logger logger = LoggerFactory.getLogger(TimeZoneInterceptor.class);

    public final static String SESSION_TIME_ZONE = "timeZone";
    private final static String SESSION_REQUEST_URL = "previousUrl";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception
    {
        HttpSession session = request.getSession();
        DateTimeZone dateTimeZone = (DateTimeZone) session.getAttribute(SESSION_TIME_ZONE);
        // If timezone is not set retrieve it
        if (dateTimeZone == null) {
            String timeZoneOffset = request.getParameter("time-zone-offset");
            String requestUrl = (String) session.getAttribute(SESSION_REQUEST_URL);
            if (!Strings.isNullOrEmpty(timeZoneOffset)) {
                // Set new time zone
                dateTimeZone = DateTimeZone.forOffsetMillis(Integer.valueOf(timeZoneOffset) * 1000);
                setDateTimeZone(session, dateTimeZone);

                logger.debug("Set timezone {} to session {}.", dateTimeZone, session.getId());

                if (requestUrl != null) {
                    // Redirect to original request url
                    session.removeAttribute(SESSION_REQUEST_URL);
                    response.sendRedirect(requestUrl);
                    return false;
                }
                else {
                    // Do not redirect to any url
                    return true;
                }
            }
            else {
                // Skip resource handlers
                if (handler instanceof  ResourceHttpRequestHandler) {
                    return true;
                }
                // Skip handlers with ignore annotation
                if (handler instanceof HandlerMethod) {
                    HandlerMethod handlerMethod = (HandlerMethod) handler;
                    IgnoreDateTimeZone ignoreZone = handlerMethod.getMethodAnnotation(IgnoreDateTimeZone.class);
                    if (ignoreZone != null) {
                        return true;
                    }
                }

                // Store request url (if it is not already set by another request)
                if (requestUrl == null) {
                    StringBuilder previousUrl = new StringBuilder();
                    previousUrl.append(request.getRequestURI());
                    String queryString = request.getQueryString();
                    if (queryString != null) {
                        previousUrl.append("?");
                        previousUrl.append(queryString);
                    }
                    session.setAttribute(SESSION_REQUEST_URL, previousUrl.toString());
                }

                // Render view for resolving timezone
                InternalResourceView view = new InternalResourceView("/WEB-INF/views/timeZone.jsp");
                view.render(null, request, response);
                return false;
            }
        }
        else {
            return true;
        }
    }

    public static DateTimeZone getDateTimeZone(HttpSession session)
    {
        return (DateTimeZone) session.getAttribute(SESSION_TIME_ZONE);
    }

    public static void setDateTimeZone(HttpSession session, DateTimeZone dateTimeZone)
    {
        session.setAttribute(SESSION_TIME_ZONE, dateTimeZone);
    }
}

