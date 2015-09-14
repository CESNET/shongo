package cz.cesnet.shongo.controller.domains;


import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class InterDomainControllerLogInterceptor implements HandlerInterceptor
{
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception
    {
        System.out.println("Invoked: " + request.getRequestURI());
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception
    {
        System.out.println("Post-handle: " + request.getRequestURI());
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception
    {
        System.out.println("After completion handle");
    }
}
