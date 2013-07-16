package cz.cesnet.shongo.api;

import FIPA.DateTime;

/**
 * Represents a recording multipoint device or endpoint recording server.
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class Recording extends IdentifiedComplexType
{
    /**
     * Name of the recording.
     */
    private String name;

    /**
     * URL to view recording.
     */
    private String url;

    /**
     * URL to download recording.
     */
    private String downloadableUrl;

    /**
     * Time of the beginning of the recording.
     */
    private DateTime beginDate;

    /**
     * Time of the end of the recording.
     */
    private DateTime endDate;

    /**
     * @return {@link #name}
     */
    public String getName()
    {
        return name;
    }

    /**
     * @param name sets the {@link #name}
     */
    public void setName(String name)
    {
        this.name = name;
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
     * @return  {@link #downloadableUrl}
     */
    public String getDownloadableUrl()
    {
        return downloadableUrl;
    }

    /**
     * @param downloadableUrl sets the {@link #downloadableUrl}
     */
    public void setDownloadableUrl(String downloadableUrl)
    {
        this.downloadableUrl = downloadableUrl;
    }

    /**
     * @return {@link #beginDate}
     */
    public DateTime getBeginDate()
    {
        return beginDate;
    }

    /**
     * @param beginDate sets the {@link #beginDate}
     */
    public void setBeginDate(DateTime beginDate)
    {
        this.beginDate = beginDate;
    }

    /**
     * @return {@link #endDate}
     */
    public DateTime getEndDate()
    {
        return endDate;
    }

    /**
     * @param endDate sets the {@link #endDate}
     */
    public void setEndDate(DateTime endDate)
    {
        this.endDate = endDate;
    }
}
