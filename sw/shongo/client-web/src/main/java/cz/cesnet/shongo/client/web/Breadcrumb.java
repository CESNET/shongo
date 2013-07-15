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
     * Current {@link Page} for which the breadcrumb should be constructed.
     */
    private Page page;

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
     * @param page sets the {@link #page}
     * @param requestUrl sets the {@link #requestUrl}
     */
    public Breadcrumb(Page page, String requestUrl)
    {
        this.page = page;
        this.requestUrl = requestUrl;
    }

    /**
     * @return true whether {@link Breadcrumb} contains more than one {@link BreadcrumbItem}, false otherwise
     */
    public boolean isMultiple()
    {
        return this.page.getParentPage() != null;
    }

    @Override
    public Iterator<BreadcrumbItem> iterator()
    {
        if (items == null) {
            Map<String, String> attributes = this.page.parseUrlAttributes(requestUrl);

            items = new LinkedList<BreadcrumbItem>();
            Page page = this.page;
            while (page != null) {
                String titleCode = page.getTitleCode();
                if (titleCode != null) {
                    String url = page.getUrl(attributes);
                    items.add(0, new BreadcrumbItem(url, titleCode));
                }
                page = page.getParentPage();
            }
        }
        return items.iterator();
    }
}
