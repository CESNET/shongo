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
     * Current {@link NavigationNode} for which the breadcrumb should be constructed.
     */
    private NavigationNode navigationNode;

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
     * @param navigationNode sets the {@link #navigationNode}
     * @param requestUrl sets the {@link #requestUrl}
     */
    public Breadcrumb(NavigationNode navigationNode, String requestUrl)
    {
        this.navigationNode = navigationNode;
        this.requestUrl = requestUrl;
    }

    /**
     * @return true whether {@link Breadcrumb} contains more than one {@link BreadcrumbItem}, false otherwise
     */
    public boolean isMultiple()
    {
        return this.navigationNode.getParentNode() != null;
    }

    @Override
    public Iterator<BreadcrumbItem> iterator()
    {
        if (items == null) {
            Map<String, String> attributes = this.navigationNode.parseUrlAttributes(requestUrl);

            items = new LinkedList<BreadcrumbItem>();
            NavigationNode navigationNode = this.navigationNode;
            while (navigationNode != null) {
                String titleCode = navigationNode.getTitleCode();
                if (titleCode != null) {
                    String url = navigationNode.getUrl(attributes);
                    items.add(0, new BreadcrumbItem(url, titleCode));
                }
                navigationNode = navigationNode.getParentNode();
            }
        }
        return items.iterator();
    }
}
