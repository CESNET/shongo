package cz.cesnet.shongo.connector.api.jade.recording;

import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.jade.ConnectorCommand;

/**
 * @see {@link cz.cesnet.shongo.connector.api.RecordingService#checkRecording}
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class CheckRecordings extends ConnectorCommand
{
    public CheckRecordings()
    {
    }

    @Override
    public Object execute(CommonService connector) throws CommandException, CommandUnsupportedException
    {
        getRecording(connector).checkRecordings();
        return null;
    }

    @Override
    public String toString()
    {
        return String.format(CheckRecordings.class.getSimpleName());
    }
}
