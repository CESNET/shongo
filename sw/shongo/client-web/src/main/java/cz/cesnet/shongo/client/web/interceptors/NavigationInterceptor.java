package cz.cesnet.shongo.client.web.interceptors;

import cz.cesnet.shongo.client.web.Breadcrumb;
import cz.cesnet.shongo.client.web.ClientWebNavigation;
import cz.cesnet.shongo.client.web.NavigationNode;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Interceptor for detection of current {@link cz.cesnet.shongo.client.web.NavigationNode}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class NavigationInterceptor extends HandlerInterceptorAdapter
{
    @Resource
    private ClientWebNavigation navigation;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception
    {
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            RequestMapping requestMapping = handlerMethod.getMethodAnnotation(RequestMapping.class);
            String[] values = requestMapping.value();
            if (values.length > 0) {
                // Create breadcrumb if it doesn't exist (it may exist when forward is processing)
                Breadcrumb breadcrumb = (Breadcrumb) request.getAttribute(Breadcrumb.REQUEST_ATTRIBUTE_BREADCRUMB);
                if (breadcrumb == null) {
                    NavigationNode navigationNode = navigation.findByUrl(values[0]);
                    if (navigationNode != null) {
                        breadcrumb = new Breadcrumb(navigationNode, request.getRequestURI());
                        request.setAttribute(Breadcrumb.REQUEST_ATTRIBUTE_BREADCRUMB, breadcrumb);
                    }
                }
            }
        }
        return true;
    }
}

