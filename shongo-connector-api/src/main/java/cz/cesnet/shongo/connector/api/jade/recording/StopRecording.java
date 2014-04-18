package cz.cesnet.shongo.connector.api.jade.recording;

import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.jade.ConnectorCommand;

/**
 * @see {@link cz.cesnet.shongo.connector.api.RecordingService#stopRecording}
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class StopRecording extends ConnectorCommand
{
    private String recordingId;

    public StopRecording()
    {
    }

    public StopRecording(String recordingId)
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
        getRecording(connector).stopRecording(recordingId);
        return null;
    }

    @Override
    public String toString()
    {
        return String.format(StopRecording.class.getSimpleName() + " (recordingId: %s)", recordingId);
    }
}
