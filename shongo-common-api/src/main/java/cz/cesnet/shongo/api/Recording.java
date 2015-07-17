package cz.cesnet.shongo.api;

import org.joda.time.DateTime;
import org.joda.time.Duration;

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
    private String description;

    /**
     * URL to download recording.
     */
    private String downloadUrl;

    /**
     * Size of downloadable recording in Bytes.
     */
    private long size;

    /**
     * URL to view recording.
     */
    private String viewUrl;

    /**
     * URL for editing recording.
     */
    private String editUrl;

    /**
     * Time of the beginning of the recording.
     */
    private DateTime beginDate;

    /**
     * Time of the end of the recording.
     */
    private Duration duration;

    /**
     * Is recording public in AC server.
     */
    private Boolean isPublic;

    /**
     * @see cz.cesnet.shongo.api.Recording.State
     */
    private State state;

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
     * @return {@link #downloadUrl}
     */
    public String getDownloadUrl()
    {
        return downloadUrl;
    }

    /**
     * @param downloadUrl sets the {@link #downloadUrl}
     */
    public void setDownloadUrl(String downloadUrl)
    {
        this.downloadUrl = downloadUrl;
    }

    /**
     * @return {@link #viewUrl}
     */
    public String getViewUrl()
    {
        return viewUrl;
    }

    /**
     * @param viewUrl sets the {@link #viewUrl}
     */
    public void setViewUrl(String viewUrl)
    {
        this.viewUrl = viewUrl;
    }

    /**
     * @return {@link #editUrl}
     */
    public String getEditUrl()
    {
        return editUrl;
    }

    /**
     * @param editUrl sets the {@link #editUrl}
     */
    public void setEditUrl(String editUrl)
    {
        this.editUrl = editUrl;
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
    public Duration getDuration()
    {
        return duration;
    }

    /**
     * @param duration sets the {@link #duration}
     */
    public void setDuration(Duration duration)
    {
        this.duration = duration;
    }

    public Boolean isPublic() {
        return isPublic;
    }

    public void setPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }

    public long getSize()
    {
        return size;
    }

    public void setSize(long size)
    {
        this.size = size;
    }

    /**
     * @return {@link #state}
     */
    public State getState()
    {
        return state;
    }

    /**
     * @param state sets the {@link #state}
     */
    public void setState(State state)
    {
        this.state = state;
    }

    public static final String RECORDING_FOLDER_ID = "recordingFolderId";
    public static final String NAME = "name";
    public static final String DESCRIPTION = "description";
    public static final String DOWNLOAD_URL = "downloadUrl";
    public static final String VIEW_URL = "viewUrl";
    public static final String EDITABLE_URL = "editUrl";
    public static final String BEGIN_DATE = "beginDate";
    public static final String DURATION = "duration";
    public static final String IS_PUBLIC = "isPublic";
    public static final String FILENAME = "filename";
    public static final String STATE = "state";
//    public static final String SIZE = "size";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(RECORDING_FOLDER_ID, recordingFolderId);
        dataMap.set(NAME, name);
        dataMap.set(DESCRIPTION, description);
        dataMap.set(DOWNLOAD_URL, downloadUrl);
        dataMap.set(VIEW_URL, viewUrl);
        dataMap.set(EDITABLE_URL, editUrl);
        dataMap.set(BEGIN_DATE, beginDate);
        dataMap.set(DURATION, duration);
        dataMap.set(IS_PUBLIC,isPublic);
        dataMap.set(FILENAME, fileName);
        dataMap.set(STATE, state);
//        dataMap.set(SIZE, size);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        recordingFolderId = dataMap.getStringRequired(RECORDING_FOLDER_ID);
        name = dataMap.getString(NAME);
        description = dataMap.getString(DESCRIPTION);
        downloadUrl = dataMap.getString(DOWNLOAD_URL);
        viewUrl = dataMap.getString(VIEW_URL);
        editUrl = dataMap.getString(EDITABLE_URL);
        beginDate = dataMap.getDateTime(BEGIN_DATE);
        duration = dataMap.getDuration(DURATION);
        isPublic = dataMap.getBoolean(IS_PUBLIC);
        fileName = dataMap.getString(FILENAME);
        state = dataMap.getEnum(STATE, State.class);
//        size = dataMap.getLongPrimitive(SIZE);
    }

    @Override
    public String toString()
    {
        return "Recording{" +
                "recordingFolderId='" + recordingFolderId + '\'' +
                ", name='" + name + '\'' +
                ", fileName='" + fileName + '\'' +
                ", description='" + description + '\'' +
                ", downloadUrl='" + downloadUrl + '\'' +
                ", viewUrl='" + viewUrl + '\'' +
                ", editUrl='" + editUrl + '\'' +
                ", beginDate=" + beginDate +
                ", duration=" + duration +
                ", state=" + state +
                ", size=" + size +
                '}';
    }

    /**
     * Available states of {@link cz.cesnet.shongo.api.Recording}.
     */
    @jade.content.onto.annotations.Element(name = "RecordingState")
    public enum State
    {
        /**
         * Recording hasn't been started yet.
         */
        NOT_STARTED,

        /**
         * Recording was not processed yet. The recording isn't available for downloading yet.
         */
        NOT_PROCESSED,

        /**
         * Recording was processed. The recording isn't available for downloading yet.
         */
        PROCESSED,

        /**
         * The recording is available for downloading.
         */
        AVAILABLE
    }
}
