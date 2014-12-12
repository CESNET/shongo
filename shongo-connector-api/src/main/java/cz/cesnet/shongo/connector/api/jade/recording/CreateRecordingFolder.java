package cz.cesnet.shongo.connector.api.jade.recording;

import cz.cesnet.shongo.api.RecordingFolder;
import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.jade.ConnectorCommand;

import java.io.FileNotFoundException;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link cz.cesnet.shongo.connector.api.RecordingService#createRecordingFolder}
 */
public class CreateRecordingFolder extends ConnectorCommand<String>
{
    private RecordingFolder recordingFolder;

    public CreateRecordingFolder()
    {
    }

    public CreateRecordingFolder(RecordingFolder recordingFolder)
    {
        this.recordingFolder = recordingFolder;
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
    public String execute(CommonService connector) throws CommandException, CommandUnsupportedException
    {
        return getRecording(connector).createRecordingFolder(recordingFolder);
    }

    @Override
    public String toString()
    {
        return String.format(CreateRecordingFolder.class.getSimpleName() + " (recordingFolder: %s)", recordingFolder);
    }
}
