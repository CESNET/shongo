package cz.cesnet.shongo.client.web.support.tiles;

import org.springframework.web.servlet.view.AbstractUrlBasedView;
import org.springframework.web.servlet.view.UrlBasedViewResolver;

/**
 * {@link UrlBasedViewResolver} which allows exposing of beans.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class TilesViewResolver extends UrlBasedViewResolver
{
    private Boolean exposeContextBeansAsAttributes;
    private String[] exposedContextBeanNames;

    public void setExposeContextBeansAsAttributes(boolean exposeContextBeansAsAttributes)
    {
        this.exposeContextBeansAsAttributes = exposeContextBeansAsAttributes;
    }

    public void setExposedContextBeanNames(String[] exposedContextBeanNames)
    {
        this.exposedContextBeanNames = exposedContextBeanNames;
    }

    @Override
    protected AbstractUrlBasedView buildView(String viewName) throws Exception
    {
        AbstractUrlBasedView superView = super.buildView(viewName);
        if (superView instanceof TilesView) {
            TilesView view = (TilesView) superView;
            if (this.exposeContextBeansAsAttributes != null) {
                view.setExposeContextBeansAsAttributes(this.exposeContextBeansAsAttributes);
            }
            if (this.exposedContextBeanNames != null) {
                view.setExposedContextBeanNames(this.exposedContextBeanNames);
            }
        }
        return superView;
    }

}
