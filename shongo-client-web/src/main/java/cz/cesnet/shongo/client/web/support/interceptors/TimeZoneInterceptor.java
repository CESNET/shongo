package cz.cesnet.shongo.client.web.support.interceptors;

import com.google.common.base.Strings;
import cz.cesnet.shongo.client.web.ClientWebConfiguration;
import cz.cesnet.shongo.client.web.models.UserSession;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;
import org.springframework.web.servlet.view.InternalResourceView;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Interceptor for detection of timezone.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class TimeZoneInterceptor extends HandlerInterceptorAdapter
{
    private static Logger logger = LoggerFactory.getLogger(TimeZoneInterceptor.class);

    private final static String REQUEST_URL_SESSION_ATTRIBUTE = "SHONGO_REQUEST_URL";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception
    {
        // Skip resource handlers
        if (handler instanceof ResourceHttpRequestHandler) {
            return true;
        }
        if (response.getStatus() != HttpServletResponse.SC_OK) {
            return true;
        }

        UserSession userSession = UserSession.getInstance(request);

        String timeZoneValue = request.getParameter("time-zone");
        if (!Strings.isNullOrEmpty(timeZoneValue)) {
            // Get time zone
            DateTimeZone timeZone;
            if (timeZoneValue.matches("-?\\d+")) {
                timeZone = DateTimeZone.forOffsetMillis(Integer.valueOf(timeZoneValue) * 1000);
            }
            else {
                timeZone = DateTimeZone.forID(timeZoneValue);
            }
            // Set new time zone
            if (userSession.getTimeZone() == null) {
                userSession.setTimeZone(timeZone);
            }
            if (userSession.getHomeTimeZone() == null) {
                userSession.setHomeTimeZone(timeZone);
            }
            userSession.setDetectedTimeZone(timeZone);
            userSession.update(request, null);

            String requestUrl = (String) WebUtils.getSessionAttribute(request, REQUEST_URL_SESSION_ATTRIBUTE);
            if (requestUrl != null) {
                // Redirect to original request url
                WebUtils.setSessionAttribute(request, REQUEST_URL_SESSION_ATTRIBUTE, null);
                response.sendRedirect(requestUrl);
                return false;
            }
            else {
                // Redirect to current request url without time-zone-offset parameter
                String queryString = request.getQueryString();
                queryString = queryString.replaceAll("time-zone=[^&]*?($|[&;])", "");
                StringBuilder requestUriBuilder = new StringBuilder();
                requestUriBuilder.append(request.getRequestURI());
                if (!queryString.isEmpty()) {
                    requestUriBuilder.append("?");
                    requestUriBuilder.append(queryString);
                }
                response.sendRedirect(requestUriBuilder.toString());
                return false;
            }
        }
        // If timezone is not set retrieve it
        else if (userSession.getTimeZone() == null || userSession.getHomeTimeZone() == null) {
            // Skip handlers with ignore annotation
            if (handler instanceof HandlerMethod) {
                HandlerMethod handlerMethod = (HandlerMethod) handler;
                IgnoreDateTimeZone ignoreZone = handlerMethod.getMethodAnnotation(IgnoreDateTimeZone.class);
                if (ignoreZone != null) {
                    return true;
                }
            }

            // Store request url (if it is not already set by another request)
            String requestUrl = (String) WebUtils.getSessionAttribute(request, REQUEST_URL_SESSION_ATTRIBUTE);
            if (requestUrl == null) {
                StringBuilder previousUrl = new StringBuilder();
                previousUrl.append(request.getRequestURI());
                String queryString = request.getQueryString();
                if (queryString != null) {
                    previousUrl.append("?");
                    previousUrl.append(queryString);
                }
                WebUtils.setSessionAttribute(request, REQUEST_URL_SESSION_ATTRIBUTE, previousUrl.toString());
            }

            // Render view for resolving timezone
            InternalResourceView view = new InternalResourceView("/WEB-INF/views/timeZone.jsp");
            view.render(null, request, response);
            return false;
        }
        else {
            return true;
        }
    }
}

