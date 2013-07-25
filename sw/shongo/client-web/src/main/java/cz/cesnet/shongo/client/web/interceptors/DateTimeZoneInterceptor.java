package cz.cesnet.shongo.client.web.interceptors;

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
public class DateTimeZoneInterceptor extends HandlerInterceptorAdapter
{
    private static Logger logger = LoggerFactory.getLogger(DateTimeZoneInterceptor.class);

    private final static String SESSION_DATE_TIME_ZONE = "dateTimeZone";
    private final static String SESSION_REQUEST_URL = "previousUrl";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception
    {
        HttpSession httpSession = request.getSession();
        DateTimeZone dateTimeZone = (DateTimeZone) httpSession.getAttribute(SESSION_DATE_TIME_ZONE);
        // If timezone is not set retrieve it
        if (dateTimeZone == null) {
            String timeZoneOffset = request.getParameter("time-zone-offset");
            String requestUrl = (String) httpSession.getAttribute(SESSION_REQUEST_URL);
            if (timeZoneOffset != null) {
                // Set new time zone
                dateTimeZone = DateTimeZone.forOffsetMillis(Integer.valueOf(timeZoneOffset) * 1000);
                httpSession.setAttribute(SESSION_DATE_TIME_ZONE, dateTimeZone);

                logger.debug("Set timezone {} to session {}.", dateTimeZone, httpSession.getId());

                if (requestUrl != null) {
                    // Redirect to original request url
                    httpSession.removeAttribute(SESSION_REQUEST_URL);
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
                    httpSession.setAttribute(SESSION_REQUEST_URL, previousUrl.toString());
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
}

