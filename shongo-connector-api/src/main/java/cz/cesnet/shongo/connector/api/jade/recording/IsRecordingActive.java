package cz.cesnet.shongo.connector.api.jade.recording;

import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.jade.ConnectorCommand;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link cz.cesnet.shongo.connector.api.RecordingService#getActiveRecording}
 */
public class IsRecordingActive extends ConnectorCommand
{
    private String recordingId;

    public IsRecordingActive()
    {
    }

    public IsRecordingActive(String recordingId)
    {
        this.recordingId = recordingId;
    }

    public String getRecordingId()
    {
        return recordingId;
    }

    public void setRecordingId(String recordingId)
    {
        this.recordingId = recordingId;
    }

    @Override
    public Object execute(CommonService connector) throws CommandException, CommandUnsupportedException
    {
        return getRecording(connector).isRecordingActive(recordingId);
    }

    @Override
    public String toString()
    {
        return String.format(IsRecordingActive.class.getSimpleName() + " (recordingId: %s)", recordingId);
    }
}
