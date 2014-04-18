package cz.cesnet.shongo.connector.api.jade.recording;

import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.jade.ConnectorCommand;

/**
 * @see {@link cz.cesnet.shongo.connector.api.RecordingService#listRecordings}
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ListRecordings extends ConnectorCommand
{
    private String recordingFolderId;

    public ListRecordings()
    {
    }

    public ListRecordings(String recordingFolderId)
    {
        this.recordingFolderId = recordingFolderId;
    }

    public String getRecordingFolderId()
    {
        return recordingFolderId;
    }

    public void setRecordingFolderId(String recordingFolderId)
    {
        this.recordingFolderId = recordingFolderId;
    }

    @Override
    public Object execute(CommonService connector) throws CommandException, CommandUnsupportedException
    {
        return getRecording(connector).listRecordings(recordingFolderId);
    }

    @Override
    public String toString()
    {
        return String.format(ListRecordings.class.getSimpleName() + " (recordingFolderId: %s)", recordingFolderId);
    }
}
