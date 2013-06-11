package cz.cesnet.shongo.client.web.interceptors;

import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
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
            if (timeZoneOffset != null) {
                // Set new time zone
                dateTimeZone = DateTimeZone.forOffsetMillis(Integer.valueOf(timeZoneOffset) * 1000);
                httpSession.setAttribute(SESSION_DATE_TIME_ZONE, dateTimeZone);

                logger.debug("Set timezone {} to session {}.", dateTimeZone, httpSession.getId());

                // Redirect to original request url
                String requestUrl = (String) httpSession.getAttribute(SESSION_REQUEST_URL);
                if (requestUrl != null) {
                    httpSession.removeAttribute(SESSION_REQUEST_URL);
                    response.sendRedirect(requestUrl);
                    return false;
                }
                else {
                    return true;
                }
            }
            else {
                // Store request url
                StringBuilder previousUrl = new StringBuilder();
                previousUrl.append(request.getRequestURI());
                String queryString = request.getQueryString();
                if (queryString != null) {
                    previousUrl.append("?");
                    previousUrl.append(queryString);
                }
                httpSession.setAttribute(SESSION_REQUEST_URL, previousUrl.toString());

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

