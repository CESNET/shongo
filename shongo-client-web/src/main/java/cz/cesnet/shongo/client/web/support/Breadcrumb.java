package cz.cesnet.shongo.client.web.support;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Represents a breadcrumb navigation.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Breadcrumb implements Iterable<Page>
{
    /**
     * Current {@link NavigationPage} for which the breadcrumb should be constructed.
     */
    private NavigationPage navigationPage;

    /**
     * Current request URL.
     */
    private String requestUrl;

    /**
     * List of constructed {@link Page}s.
     */
    private List<Page> pages;

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
     * @return true whether {@link Breadcrumb} contains more than one {@link Page}, false otherwise
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

    /**
     * @return url to previous page
     */
    public String getBackUrl()
    {
        if (pages == null) {
            buildPages();
        }
        if (pages.size() < 2) {
            return null;
        }
        return pages.get(pages.size() - 2).getUrl();
    }

    /**
     * Build {@link #pages}
     */
    private void buildPages()
    {
        Map<String, String> attributes = this.navigationPage.parseUrlAttributes(requestUrl, true);

        pages = new LinkedList<Page>();
        NavigationPage navigationPage = this.navigationPage;
        while (navigationPage != null) {
            String titleCode = navigationPage.getTitleCode();
            if (titleCode != null) {
                String url = navigationPage.getUrl(attributes);
                Object[] titleArguments = navigationPage.getTitleArguments();
                pages.add(0, new Page(url, titleCode, titleArguments));
            }
            navigationPage = navigationPage.getParentNavigationPage();
        }
    }

    /**
     * @param page to be added to the {@link #pages}
     */
    public void addPage(Page page)
    {
        if (pages == null) {
            buildPages();
        }
        pages.add(page);
    }

    /**
     * @param pages to be added to the {@link #pages}
     */
    public void addPages(List<Page> pages)
    {
        if (this.pages == null) {
            buildPages();
        }
        this.pages.addAll(pages);
    }

    /**
     * @param index
     * @param pages to be added to the {@link #pages}
     */
    public void addPages(int index, List<Page> pages)
    {
        if (this.pages == null) {
            buildPages();
        }
        this.pages.addAll(index, pages);
    }

    /**
     * @return size of the {@link #pages}
     */
    public int getPagesCount()
    {
        if (pages == null) {
            buildPages();
        }
        return pages.size();
    }

    @Override
    public Iterator<Page> iterator()
    {
        if (pages == null) {
            buildPages();
        }
        return pages.iterator();
    }
}
