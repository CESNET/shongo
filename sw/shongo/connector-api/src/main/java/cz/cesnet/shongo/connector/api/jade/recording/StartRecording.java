package cz.cesnet.shongo.connector.api.jade.recording;

import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.jade.ConnectorCommand;

/**
 * @see {@link cz.cesnet.shongo.connector.api.RecordingService#startRecording}
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class StartRecording extends ConnectorCommand
{
    private String recordingFolderId;

    private Alias alias;

    public StartRecording()
    {
    }

    public StartRecording(String recordingFolderId, Alias alias)
    {
        this.recordingFolderId = recordingFolderId;
        this.alias = alias;
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

    @Override
    public Object execute(CommonService connector) throws CommandException, CommandUnsupportedException
    {
        return getRecording(connector).startRecording(recordingFolderId, alias);
    }

    @Override
    public String toString()
    {
        return String.format(StartRecording.class.getSimpleName() + " (recordingFolderId: %s, alias: %s)",
                recordingFolderId, alias);
    }
}
