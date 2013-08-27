package cz.cesnet.shongo.client.web;

/**
 * {@link Breadcrumb} provider.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public interface BreadcrumbProvider
{
    /**
     * @param navigationPage {@link NavigationPage} determined by given {@code requestURI} or {@code null}
     * @param requestURI current request URL
     * @return new instance of {@link Breadcrumb} for given {@code navigationPage} and/or {@code requestURI}
     */
    public Breadcrumb createBreadcrumb(NavigationPage navigationPage, String requestURI);
}
