package cz.cesnet.shongo.connector.api.jade.recording;

import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.RecordingSettings;
import cz.cesnet.shongo.connector.api.jade.ConnectorCommand;

/**
 * @see {@link cz.cesnet.shongo.connector.api.RecordingService#startRecording}
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class StartRecording extends ConnectorCommand<String>
{
    private String recordingFolderId;

    private Alias alias;

    private RecordingSettings recordingSettings;

    public StartRecording()
    {
    }

    public StartRecording(String recordingFolderId, Alias alias, RecordingSettings recordingSettings)
    {
        this.recordingFolderId = recordingFolderId;
        this.alias = alias;
        this.recordingSettings = recordingSettings;
    }

    public String getRecordingFolderId()
    {
        return recordingFolderId;
    }

    public void setRecordingFolderId(String recordingFolderId)
    {
        this.recordingFolderId = recordingFolderId;
    }

    public Alias getAlias()
    {
        return alias;
    }

    public void setAlias(Alias alias)
    {
        this.alias = alias;
    }

    public RecordingSettings getRecordingSettings()
    {
        return recordingSettings;
    }

    public void setRecordingSettings(RecordingSettings recordingSettings)
    {
        this.recordingSettings = recordingSettings;
    }

    @Override
    public String execute(CommonService connector) throws CommandException, CommandUnsupportedException
    {
        return getRecording(connector).startRecording(recordingFolderId, alias, recordingSettings);
    }

    @Override
    public String toString()
    {
        return String.format(StartRecording.class.getSimpleName() + " (recordingFolderId: %s, alias: %s, settings: %s)",
                recordingFolderId, alias, recordingSettings);
    }
}
