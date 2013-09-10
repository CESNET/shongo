package cz.cesnet.shongo.client.web.support.interceptors;

import cz.cesnet.shongo.client.web.support.Breadcrumb;
import cz.cesnet.shongo.client.web.support.BreadcrumbProvider;
import cz.cesnet.shongo.client.web.ClientWebNavigation;
import cz.cesnet.shongo.client.web.support.NavigationPage;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Interceptor for detection of current {@link cz.cesnet.shongo.client.web.support.NavigationPage}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class NavigationInterceptor extends HandlerInterceptorAdapter
{
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception
    {
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            Object controller = handlerMethod.getBean();

            // Create breadcrumb if it doesn't exist (it may exist when forward is processing)
            Breadcrumb breadcrumb = (Breadcrumb) request.getAttribute(Breadcrumb.REQUEST_ATTRIBUTE_BREADCRUMB);
            if (breadcrumb == null) {
                // Determine navigation page by current URL
                NavigationPage navigationPage = null;
                RequestMapping requestMapping = handlerMethod.getMethodAnnotation(RequestMapping.class);
                String[] values = requestMapping.value();
                if (values.length > 0) {
                    navigationPage = ClientWebNavigation.findByUrl(values[0]);
                }

                // Create breadcrumb
                if (controller instanceof BreadcrumbProvider) {
                    // Create breadcrumb by controller
                    BreadcrumbProvider breadcrumbProvider = (BreadcrumbProvider) controller;
                    breadcrumb = breadcrumbProvider.createBreadcrumb(navigationPage, request.getRequestURI());
                }
                else if (navigationPage != null) {
                    // Create breadcrumb from navigation page
                    breadcrumb = new Breadcrumb(navigationPage, request.getRequestURI());
                }
            }
            if (breadcrumb != null) {
                request.setAttribute(Breadcrumb.REQUEST_ATTRIBUTE_BREADCRUMB, breadcrumb);
            }
        }
        return true;
    }

    /**
     * @param request
     * @return current {@link NavigationPage} or null
     */
    public static NavigationPage getNavigationPage(HttpServletRequest request)
    {
        Breadcrumb breadcrumb = (Breadcrumb) request.getAttribute(Breadcrumb.REQUEST_ATTRIBUTE_BREADCRUMB);
        if (breadcrumb != null) {
            return breadcrumb.getNavigationPage();
        }
        return null;
    }
}

