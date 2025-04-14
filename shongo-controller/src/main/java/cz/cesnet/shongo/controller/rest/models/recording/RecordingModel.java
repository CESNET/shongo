package cz.cesnet.shongo.controller.rest.models.recording;

import com.fasterxml.jackson.annotation.JsonFormat;
import cz.cesnet.shongo.controller.api.ResourceRecording;
import lombok.Data;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import static cz.cesnet.shongo.controller.rest.models.TimeInterval.ISO_8601_PATTERN;

/**
 * Represents a recording.
 *
 * @author Filip Karnis
 */
@Data
public class RecordingModel
{

    private String id;
    private String name;
    private String description;
    private String resourceId;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = ISO_8601_PATTERN)
    private DateTime beginDate;
    private Long duration;
    private Boolean isPublic;
    private String downloadUrl;
    private String viewUrl;
    private String editUrl;
    private String filename;

    public RecordingModel(ResourceRecording recording)
    {
        this.id = recording.getId();
        this.name = recording.getName();
        this.description = recording.getDescription();
        this.resourceId = recording.getResourceId();
        this.beginDate = recording.getBeginDate();
        Duration duration = recording.getDuration();
        if (duration == null || duration.isShorterThan(Duration.standardMinutes(1))) {
            this.duration = null;
        }
        else {
            this.duration = duration.getMillis();
        }
        this.isPublic = recording.isPublic();
        this.downloadUrl = recording.getDownloadUrl();
        this.viewUrl = recording.getViewUrl();
        this.editUrl = recording.getEditUrl();
        this.filename = recording.getFileName();
    }
}
