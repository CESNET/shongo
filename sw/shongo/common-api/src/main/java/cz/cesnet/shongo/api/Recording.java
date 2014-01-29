package cz.cesnet.shongo.api;


import org.joda.time.DateTime;
import org.joda.time.Period;

/**
 * Represents a recording in multipoint device or endpoint recording server.
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class Recording extends IdentifiedComplexType
{
    /**
     * Identifier of recording folder whether the {@link Recording} is located.
     */
    private String recordingFolderId;

    /**
     * Name of the recording.
     */
    private String name;

    /**
     * Name of the recording file for download.
     */
    private String fileName;

    /**
     * Description.
     */
    private  String description;

    /**
     * URL to view recording.
     */
    private String url;

    /**
     * URL to download recording.
     */
    private String downloadableUrl;

    /**
     * URL for editing recording.
     */
    private String editableUrl;

    /**
     * Time of the beginning of the recording.
     */
    private DateTime beginDate;

    /**
     * Time of the end of the recording.
     */
    private Period duration;

    /**
     * @return {@link #recordingFolderId}
     */
    public String getRecordingFolderId()
    {
        return recordingFolderId;
    }

    /**
     * @param recordingFolderId sets the {@link #recordingFolderId}
     */
    public void setRecordingFolderId(String recordingFolderId)
    {
        this.recordingFolderId = recordingFolderId;
    }

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
     * @return {@link #fileName}
     */
    public String getFileName()
    {
        return fileName;
    }

    /**
     * @param fileName sets the {@link #fileName}
     */
    public void setFileName(String fileName)
    {
        this.fileName = fileName;
    }

    /**
     * @param description sets the {@link #description}
     */
    public void setDescription(String description)
    {
        this.description = description;
    }

    /**
     * @return {@link #description}
     */
    public String getDescription()
    {
        return description;
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
     * @return {@link #editableUrl}
     */
    public String getEditableUrl()
    {
        return editableUrl;
    }

    /**
     * @param editableUrl sets the {@link #editableUrl}
     */
    public void setEditableUrl(String editableUrl)
    {
        this.editableUrl = editableUrl;
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
     * @return {@link #duration}
     */
    public Period getDuration()
    {
        return duration;
    }

    /**
     * @param duration sets the {@link #duration}
     */
    public void setDuration(Period duration)
    {
        this.duration = duration;
    }

    public static final String RECORDING_FOLDER_ID = "recordingFolderId";
    public static final String NAME = "name";
    public static final String DESCRIPTION = "description";
    public static final String URL = "url";
    public static final String DOWNLOADABLEURL = "downloadableUrl";
    public static final String EDITABLEURL = "editableUrl";
    public static final String BEGINDATE = "beginDate";
    public static final String DURATION = "duration";
    public static final String FILENAME = "filename";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(RECORDING_FOLDER_ID, recordingFolderId);
        dataMap.set(NAME, name);
        dataMap.set(DESCRIPTION,description);
        dataMap.set(URL, url);
        dataMap.set(DOWNLOADABLEURL, downloadableUrl);
        dataMap.set(EDITABLEURL, editableUrl);
        dataMap.set(BEGINDATE, beginDate);
        dataMap.set(DURATION, duration);
        dataMap.set(FILENAME,fileName);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        recordingFolderId = dataMap.getStringRequired(RECORDING_FOLDER_ID);
        name = dataMap.getString(NAME);
        description = dataMap.getString(DESCRIPTION);
        url = dataMap.getString(URL);
        downloadableUrl = dataMap.getString(DOWNLOADABLEURL);
        editableUrl = dataMap.getString(EDITABLEURL);
        beginDate = dataMap.getDateTime(BEGINDATE);
        duration = dataMap.getPeriod(DURATION);
        fileName = dataMap.getString(FILENAME);
    }

    @Override
    public String toString()
    {
        return "Recording{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", url='" + url + '\'' +
                ", downloadableUrl='" + downloadableUrl + '\'' +
                ", editableUrl='" + editableUrl + '\'' +
                ", beginDate=" + beginDate +
                ", duration=" + duration +
                ", fileName=" + fileName +
                '}';
    }
}
