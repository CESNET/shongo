package cz.cesnet.shongo.client.web.support.tiles;

import org.springframework.web.context.support.ContextExposingHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * {@link org.springframework.web.servlet.view.tiles3.TilesView} which allows exposing of beans.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class TilesView extends org.springframework.web.servlet.view.tiles3.TilesView
{
    private boolean exposeContextBeansAsAttributes = false;
    private Set<String> exposedContextBeanNames;

    public void setExposeContextBeansAsAttributes(boolean exposeContextBeansAsAttributes)
    {
        this.exposeContextBeansAsAttributes = exposeContextBeansAsAttributes;
    }

    public void setExposedContextBeanNames(String[] exposedContextBeanNames)
    {
        this.exposedContextBeanNames = new HashSet<String>(Arrays.asList(exposedContextBeanNames));
    }

    protected HttpServletRequest getRequestToExpose(HttpServletRequest originalRequest)
    {
        if (this.exposeContextBeansAsAttributes || this.exposedContextBeanNames != null) {
            return new ContextExposingHttpServletRequest(originalRequest, getWebApplicationContext(),
                    this.exposedContextBeanNames);
        }
        return originalRequest;
    }

    @Override
    protected void renderMergedOutputModel(Map<String, Object> model, HttpServletRequest request,
            HttpServletResponse response) throws Exception
    {
        HttpServletRequest requestToExpose = getRequestToExpose(request);
        exposeModelAsRequestAttributes(model, requestToExpose);
        super.renderMergedOutputModel(model, requestToExpose, response);
    }
}
