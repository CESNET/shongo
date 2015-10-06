package cz.cesnet.shongo.controller.domains;


import org.slf4j.Logger;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Map;

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
        getLogger().debug("Inter domain request invoked: " + printRequest(request));
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception
    {
        getLogger().debug("Inter domain request handled: " + printRequest(request));
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception
    {
        getLogger().debug("Inter domain request finished: " + printRequest(request));
    }

    private String printRequest(HttpServletRequest request)
    {
        StringBuilder params = new StringBuilder(request.getRequestURI());
        params.append("?");
        for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
            if (!params.toString().endsWith("?")) {
                params.append("&");
            }
            params.append(entry.getKey());
            params.append("=");
            params.append(Arrays.toString(request.getParameterMap().get(entry.getKey())));
        }
        return params.toString();
    }
}
