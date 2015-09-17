package cz.cesnet.shongo.controller.domains;


import org.slf4j.Logger;
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
    private Logger getLogger()
    {
        return InterDomainAgent.getInstance().getLogger();
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception
    {
        getLogger().debug("Inter domain request invoked: " + request.getRequestURI() + request);
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception
    {
        getLogger().debug("Inter domain request handled: " + request.getRequestURI());
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception
    {
        getLogger().debug("Inter domain request finished:" + request.getRequestURI());
    }
}
