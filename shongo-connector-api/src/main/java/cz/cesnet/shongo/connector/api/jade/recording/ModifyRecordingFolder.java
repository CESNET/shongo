package cz.cesnet.shongo.connector.api.jade.recording;

import cz.cesnet.shongo.api.RecordingFolder;
import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.jade.ConnectorCommand;

import java.util.Map;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link cz.cesnet.shongo.connector.api.RecordingService#modifyRecordingFolder}
 */
public class ModifyRecordingFolder extends ConnectorCommand
{
    private RecordingFolder recordingFolder;

    public ModifyRecordingFolder()
    {
    }

    public ModifyRecordingFolder(String recordingFolderId, Map<String, RecordingFolder.UserPermission> userPermissions)
    {
        this.recordingFolder = new RecordingFolder();
        this.recordingFolder.setId(recordingFolderId);
        this.recordingFolder.setUserPermissions(userPermissions);
    }

    public RecordingFolder getRecordingFolder()
    {
        return recordingFolder;
    }

    public void setRecordingFolder(RecordingFolder recordingFolder)
    {
        this.recordingFolder = recordingFolder;
    }

    @Override
    public Object execute(CommonService connector) throws CommandException, CommandUnsupportedException
    {
        getRecording(connector).modifyRecordingFolder(recordingFolder);
        return null;
    }

    @Override
    public String toString()
    {
        return String.format(ModifyRecordingFolder.class.getSimpleName() + " (recordingFolder: %s)", recordingFolder);
    }
}
