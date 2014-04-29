package cz.cesnet.shongo.client.web;

import org.apache.tiles.AttributeContext;
import org.apache.tiles.preparer.ViewPreparer;
import org.apache.tiles.request.Request;
import org.apache.tiles.request.servlet.ServletRequest;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.http.HttpServletRequest;
import java.net.URL;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class LayoutViewPreparer implements ViewPreparer
{
    private URL designLayoutUrl;

    @Override
    public void execute(Request request, AttributeContext attributeContext)
    {
        ServletRequest servletRequest = (ServletRequest) request;
        HttpServletRequest httpServletRequest = servletRequest.getRequest();
        WebApplicationContext applicationContext = (WebApplicationContext) httpServletRequest.getAttribute(
                org.springframework.web.servlet.DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE);
        final Design design = applicationContext.getBean(Design.class);
        java.util.Map<java.lang.String, java.lang.Object> requestScope = servletRequest.getRequestScope();
        requestScope.put("designLayoutUrl", design.getResourcesFolder() + "/layout.jsp");
        requestScope.put("url", new LayoutContextUrl());
        requestScope.put("version", "5.0.0");
    }

    public static class LayoutContextUrl
    {
        public String getHome()
        {
            return ClientWebUrl.HOME;
        }

        public String getChangelog()
        {
            return ClientWebUrl.CHANGELOG;
        }

        public String getResources()
        {
            return "/design";
        }
    }
}
