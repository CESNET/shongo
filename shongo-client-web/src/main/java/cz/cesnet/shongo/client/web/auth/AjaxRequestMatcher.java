package cz.cesnet.shongo.client.web.auth;

import org.apache.commons.lang.StringUtils;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.servlet.http.HttpServletRequest;

/**
 * {@link RequestMatcher} for AJAX requests.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AjaxRequestMatcher implements RequestMatcher
{
    @Override
    public boolean matches(HttpServletRequest request)
    {
        return isAjaxRequest(request);
    }

    /**
     * @param request to be checked
     * @return true whether given {@code request} is AJAX request,
     * false otherwise
     */
    public static boolean isAjaxRequest(HttpServletRequest request)
    {
        String ajaxHeaderValue = request.getHeader("x-requested-with");
        if(ajaxHeaderValue != null && StringUtils.equalsIgnoreCase("XMLHttpRequest", ajaxHeaderValue)) {
            return true;
        }
        String requestUri = request.getRequestURI();
        if (requestUri.endsWith("/data")) {
            return true;
        }
        return false;
    }
}
