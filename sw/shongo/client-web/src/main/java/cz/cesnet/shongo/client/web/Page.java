package cz.cesnet.shongo.client.web;

/**
 * Represents a page (or action).
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Page
{
    /**
     * Node URL (with attributes).
     */
    protected String url;

    /**
     * Node title message code for translation.
     */
    protected String titleCode;

    /**
     * Constructor.
     *
     * @param url       sets the {@link #url}
     * @param titleCode sets the {@link #titleCode}
     */
    public Page(String url, String titleCode)
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
     * @param url sets the {@link #url}
     */
    public void setUrl(String url)
    {
        this.url = url;
    }

    /**
     * @return {@link #titleCode}
     */
    public String getTitleCode()
    {
        return titleCode;
    }

    /**
     * @param titleCode sets the {@link #titleCode}
     */
    public void setTitleCode(String titleCode)
    {
        this.titleCode = titleCode;
    }
}
