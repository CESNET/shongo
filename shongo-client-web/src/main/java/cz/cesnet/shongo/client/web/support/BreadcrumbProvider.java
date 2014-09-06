package cz.cesnet.shongo.client.web.support;

import javax.servlet.http.HttpServletRequest;

/**
 * {@link Breadcrumb} provider.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public interface BreadcrumbProvider
{
    /**
     * @param navigationPage {@link NavigationPage} determined by given {@code request} or {@code null}
     * @param request current request
     * @return new instance of {@link Breadcrumb} for given {@code navigationPage} and/or {@code request}
     */
    public Breadcrumb createBreadcrumb(NavigationPage navigationPage, HttpServletRequest request);
}
