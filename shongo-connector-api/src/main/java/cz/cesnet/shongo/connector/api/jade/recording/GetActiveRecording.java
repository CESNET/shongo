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
public class GetActiveRecording extends ConnectorCommand
{
    private Alias alias;

    public GetActiveRecording()
    {
    }

    public GetActiveRecording(Alias alias)
    {
        this.alias = alias;
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
        return getRecording(connector).getActiveRecording(alias);
    }

    @Override
    public String toString()
    {
        return String.format(GetActiveRecording.class.getSimpleName() + " (alias: %s)", alias);
    }
}
