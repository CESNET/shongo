package cz.cesnet.shongo.connector.api.jade.recording;

import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.jade.ConnectorCommand;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link cz.cesnet.shongo.connector.api.RecordingService#createRecordingFolder}
 */
public class CreateRecordingFolder extends ConnectorCommand
{
    private String description;

    public CreateRecordingFolder()
    {
    }

    public CreateRecordingFolder(String description)
    {
        this.description = description;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    @Override
    public Object execute(CommonService connector) throws CommandException, CommandUnsupportedException
    {
        return getRecording(connector).createRecordingFolder(description);
    }

    @Override
    public String toString()
    {
        return String.format(CreateRecordingFolder.class.getSimpleName() + " (description: %s)", description);
    }
}
