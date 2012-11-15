package cz.cesnet.shongo.connector.api.ontology.actions.endpoint;

import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.ontology.ConnectorAgentAction;

/**
 * Command to set playback level.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class SetPlaybackLevel extends ConnectorAgentAction
{
    private int level;

    public int getLevel()
    {
        return level;
    }

    public void setLevel(int level)
    {
        this.level = level;
    }


    public SetPlaybackLevel()
    {
    }

    public SetPlaybackLevel(int level)
    {
        this.level = level;
    }

    @Override
    public Object exec(CommonService connector) throws CommandException, CommandUnsupportedException
    {
        getEndpoint(connector).setPlaybackLevel(level);
        return null;
    }

    public String toString()
    {
        return String.format("SetPlaybackLevel agent action (level: %d)", level);
    }
}
