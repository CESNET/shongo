package cz.cesnet.shongo.client.web.support.interceptors;

import com.google.common.base.Strings;
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

    private final static String REQUEST_URL_SESSION_ATTRIBUTE = "previousUrl";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception
    {
        UserSession userSession = UserSession.getInstance(request);

        // If timezone is not set retrieve it
        if (userSession.getTimeZone() == null) {
            String timeZoneOffset = request.getParameter("time-zone-offset");
            String requestUrl = (String) WebUtils.getSessionAttribute(request, REQUEST_URL_SESSION_ATTRIBUTE);
            if (!Strings.isNullOrEmpty(timeZoneOffset)) {
                // Set new time zone
                DateTimeZone dateTimeZone = DateTimeZone.forOffsetMillis(Integer.valueOf(timeZoneOffset) * 1000);
                userSession.setTimeZone(dateTimeZone);
                userSession.update(request, null);

                logger.debug("Set timezone {} to session {}.", dateTimeZone, userSession.toString());

                if (requestUrl != null) {
                    // Redirect to original request url
                    WebUtils.setSessionAttribute(request, REQUEST_URL_SESSION_ATTRIBUTE, null);
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
                    WebUtils.setSessionAttribute(request, REQUEST_URL_SESSION_ATTRIBUTE, previousUrl.toString());
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

