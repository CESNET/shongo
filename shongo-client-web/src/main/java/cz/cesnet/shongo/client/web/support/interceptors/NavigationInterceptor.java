package cz.cesnet.shongo.client.web.support.interceptors;

import cz.cesnet.shongo.client.web.support.BackUrl;
import cz.cesnet.shongo.client.web.support.Breadcrumb;
import cz.cesnet.shongo.client.web.support.BreadcrumbProvider;
import cz.cesnet.shongo.client.web.ClientWebNavigation;
import cz.cesnet.shongo.client.web.support.NavigationPage;
import org.apache.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Interceptor for detection of current {@link cz.cesnet.shongo.client.web.support.NavigationPage}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class NavigationInterceptor extends HandlerInterceptorAdapter
{
    /**
     * Request attribute in which the {@link Breadcrumb} is stored.
     */
    public final static String BREADCRUMB_REQUEST_ATTRIBUTE = "breadcrumb";

    /**
     * Request attribute in which the current request URL is stored.
     */
    public final static String REQUEST_URL_REQUEST_ATTRIBUTE = "requestUrl";

    /**
     * Request attribute in which the {@link BackUrl} is stored.
     */
    public final static String BACK_URL_REQUEST_ATTRIBUTE = "backUrl";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception
    {
        // Skip resource handlers
        if (handler instanceof ResourceHttpRequestHandler) {
            return true;
        }

        // Add back URL and redirect back without the back URL parameter
        String backUrl = request.getParameter("back-url");
        if (backUrl != null && response.getStatus() != HttpStatus.SC_NOT_FOUND) {
            BackUrl.addUrl(request, backUrl);
            String requestUrl = request.getRequestURI();
            String queryString = request.getQueryString();
            queryString = queryString.replaceAll("&?back-url=[^&]*?($|[&;])", "");
            StringBuilder requestUriBuilder = new StringBuilder();
            requestUriBuilder.append(requestUrl);
            if (!queryString.isEmpty()) {
                requestUriBuilder.append("?");
                requestUriBuilder.append(queryString);
            }
            response.sendRedirect(requestUriBuilder.toString());
            return false;
        }

        // Breadcrumb
        Breadcrumb breadcrumb = null;
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            Object controller = handlerMethod.getBean();

            // Create breadcrumb if it doesn't exist (it may exist when forward is processing)
            breadcrumb = (Breadcrumb) request.getAttribute(BREADCRUMB_REQUEST_ATTRIBUTE);
            if (breadcrumb == null) {
                // Determine navigation page by current URL
                NavigationPage navigationPage = null;
                RequestMapping requestMapping = handlerMethod.getMethodAnnotation(RequestMapping.class);
                String[] values = requestMapping.value();
                if (values.length == 1) {
                    navigationPage = ClientWebNavigation.findByUrl(values[0]);
                }
                else if (values.length > 1) {
                    String requestUrl = request.getRequestURI();
                    for (String value : values) {
                        NavigationPage possibleNavigationPage = ClientWebNavigation.findByUrl(value);
                        if (possibleNavigationPage.matchesUrl(requestUrl)) {
                            navigationPage = possibleNavigationPage;
                            break;
                        }
                    }
                }

                // Create breadcrumb
                if (controller instanceof BreadcrumbProvider) {
                    // Create breadcrumb by controller
                    BreadcrumbProvider breadcrumbProvider = (BreadcrumbProvider) controller;
                    breadcrumb = breadcrumbProvider.createBreadcrumb(navigationPage, request);
                }
                else if (navigationPage != null) {
                    // Create breadcrumb from navigation page
                    breadcrumb = new Breadcrumb(navigationPage, request.getRequestURI());
                }
            }
        }
        if (breadcrumb != null) {
            request.setAttribute(BREADCRUMB_REQUEST_ATTRIBUTE, breadcrumb);
        }

        // Request URL
        String servletPath = request.getServletPath();
        String requestUri = request.getServletPath();
        String queryString = request.getQueryString();
        StringBuilder requestUriBuilder = new StringBuilder();
        requestUriBuilder.append(requestUri);
        if (queryString != null && !queryString.isEmpty()) {
            requestUriBuilder.append("?");
            requestUriBuilder.append(queryString);
        }
        request.setAttribute(REQUEST_URL_REQUEST_ATTRIBUTE, requestUriBuilder.toString());

        // Back URL
        request.setAttribute(BACK_URL_REQUEST_ATTRIBUTE, BackUrl.getInstance(request, breadcrumb));

        return true;
    }
}

