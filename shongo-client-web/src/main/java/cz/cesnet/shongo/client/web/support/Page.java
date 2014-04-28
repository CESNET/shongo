package cz.cesnet.shongo.client.web.support;

/**
 * Represents a page (or action).
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Page
{
    /**
     * Page URL (with attributes).
     */
    protected String url;

    /**
     * Page title message code for translation.
     */
    protected String titleCode;

    /**
     * Page title message arguments.
     */
    protected Object[] titleArguments;

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
     * Constructor.
     *
     * @param url       sets the {@link #url}
     * @param titleCode sets the {@link #titleCode}
     * @param titleArguments sets the {@link #titleArguments}
     */
    public Page(String url, String titleCode, Object[] titleArguments)
    {
        this.url = url;
        this.titleCode = titleCode;
        this.titleArguments = titleArguments;
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

    /**
     * @return {@link #titleArguments}
     */
    public Object[] getTitleArguments()
    {
        return titleArguments;
    }

    /**
     * @param titleArguments sets the {@link #titleArguments}
     */
    public void setTitleArguments(Object[] titleArguments)
    {
        this.titleArguments = titleArguments;
    }
}
