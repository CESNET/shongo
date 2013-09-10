package cz.cesnet.shongo.client.web.support;

/**
 * Represents a link in {@link Breadcrumb} navigation.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class BreadcrumbItem
{
    /**
     * Link URL.
     */
    private final String url;

    /**
     * Link title message code for translation.
     */
    private final String titleCode;

    /**
     * Constructor.
     *
     * @param url sets the {@link #url}
     * @param titleCode sets the {@link #titleCode}
     */
    public BreadcrumbItem(String url, String titleCode)
    {
        this.url = url;
        this.titleCode = titleCode;
    }

    /**
     * @return {@link #url}
     */
    public String getUrl()
    {
        return url;
    }

    /**
     * @return {@link #titleCode}
     */
    public String getTitleCode()
    {
        return titleCode;
    }
}
