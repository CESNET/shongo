package cz.cesnet.shongo.client.web;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Represents a breadcrumb navigation.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Breadcrumb implements Iterable<BreadcrumbItem>
{
    /**
     * Request attribute in which the breadcrumb is stored.
     */
    public final static String REQUEST_ATTRIBUTE_BREADCRUMB = "breadcrumb";

    /**
     * Current {@link NavigationPage} for which the breadcrumb should be constructed.
     */
    private NavigationPage navigationPage;

    /**
     * Current request URL.
     */
    private String requestUrl;

    /**
     * List of constructed {@link BreadcrumbItem}s.
     */
    private List<BreadcrumbItem> items;

    /**
     * Constructor.
     *
     * @param navigationPage sets the {@link #navigationPage}
     * @param requestUrl sets the {@link #requestUrl}
     */
    public Breadcrumb(NavigationPage navigationPage, String requestUrl)
    {
        this.navigationPage = navigationPage;
        this.requestUrl = requestUrl;
    }

    /**
     * @return true whether {@link Breadcrumb} contains more than one {@link BreadcrumbItem}, false otherwise
     */
    public boolean isMultiple()
    {
        return this.navigationPage.getParentNavigationPage() != null;
    }

    /**
     * @return current {@link #navigationPage}
     */
    public NavigationPage getNavigationPage()
    {
        return navigationPage;
    }

    @Override
    public Iterator<BreadcrumbItem> iterator()
    {
        if (items == null) {
            Map<String, String> attributes = this.navigationPage.parseUrlAttributes(requestUrl, true);

            items = new LinkedList<BreadcrumbItem>();
            NavigationPage navigationPage = this.navigationPage;
            while (navigationPage != null) {
                String titleCode = navigationPage.getTitleCode();
                if (titleCode != null) {
                    String url = navigationPage.getUrl(attributes);
                    items.add(0, new BreadcrumbItem(url, titleCode));
                }
                navigationPage = navigationPage.getParentNavigationPage();
            }
        }
        return items.iterator();
    }
}
