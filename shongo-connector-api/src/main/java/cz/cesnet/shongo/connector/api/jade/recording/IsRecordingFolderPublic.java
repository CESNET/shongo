package cz.cesnet.shongo.connector.api.jade.recording;

import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.jade.ConnectorCommand;

/**
 * Created by opicak on 22.10.2014.
 */
public class IsRecordingFolderPublic extends ConnectorCommand {
    private String recordingFolderId;

    public IsRecordingFolderPublic()
    {
    }

    public IsRecordingFolderPublic(String recordingFolderId)
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
        return getRecording(connector).isRecordingFolderPublic(recordingFolderId);
    }

    @Override
    public String toString()
    {
        return String.format(IsRecordingFolderPublic.class.getSimpleName() + " (recordingFolderId: %s)", recordingFolderId);
    }
}
