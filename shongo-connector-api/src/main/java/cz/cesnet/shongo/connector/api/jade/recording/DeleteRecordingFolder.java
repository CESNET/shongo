package cz.cesnet.shongo.connector.api.jade.recording;

import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.jade.ConnectorCommand;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link cz.cesnet.shongo.connector.api.RecordingService#deleteRecordingFolder}
 */
public class DeleteRecordingFolder extends ConnectorCommand
{
    private String recordingFolderId;

    public DeleteRecordingFolder()
    {
    }

    public DeleteRecordingFolder(String recordingFolderId)
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
        getRecording(connector).deleteRecordingFolder(recordingFolderId);
        return null;
    }

    @Override
    public String toString()
    {
        return String.format(DeleteRecordingFolder.class.getSimpleName() +
                " (recordingFolderId: %s)", recordingFolderId);
    }
}
