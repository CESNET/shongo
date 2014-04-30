package cz.cesnet.shongo.client.web;

import freemarker.template.Template;
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
    private Design design;

    @Override
    public void execute(Request request, AttributeContext attributeContext)
    {
        ServletRequest servletRequest = (ServletRequest) request;
        HttpServletRequest httpServletRequest = servletRequest.getRequest();
        if (design == null) {
            WebApplicationContext applicationContext = (WebApplicationContext) httpServletRequest.getAttribute(
                    org.springframework.web.servlet.DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE);
            design = applicationContext.getBean(Design.class);
        }
        java.util.Map<java.lang.String, java.lang.Object> requestScope = servletRequest.getRequestScope();
        requestScope.put("designLayout", new DesignLayout(design, httpServletRequest));
    }

    /**
     * Design layout which can be rendered from JSP.
     */
    public static class DesignLayout
    {
        /**
         * @see cz.cesnet.shongo.client.web.Design
         */
        private Design design;

        /**
         * @see javax.servlet.http.HttpServletRequest
         */
        private HttpServletRequest request;

        /**
         * Constructor.
         *
         * @param design sets the {@link #design}
         * @param request sets the {@link #request}
         */
        public DesignLayout(Design design, HttpServletRequest request)
        {
            this.design = design;
            this.request = request;
        }

        /**
         * @param head
         * @param content
         * @return rendered layout
         */
        public String render(String head, String title, String content)
        {
            Template template = design.getTemplate(Design.LAYOUT_FILE_NAME);
            return design.renderTemplate(template, design.createLayoutContext(request, head, title, content));
        }
    }
}
