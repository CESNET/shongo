package cz.cesnet.shongo.connector.api.jade.endpoint;

import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.jade.ConnectorCommand;

/**
 * Command to set microphone(s) level.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class SetMicrophoneLevel extends ConnectorCommand
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


    public SetMicrophoneLevel()
    {
    }

    public SetMicrophoneLevel(int level)
    {
        this.level = level;
    }

    @Override
    public Object execute(CommonService connector) throws CommandException, CommandUnsupportedException
    {
        logger.debug("Setting microphone level {}", level);
        getEndpoint(connector).setMicrophoneLevel(level);
        return null;
    }

    public String toString()
    {
        return String.format(SetMicrophoneLevel.class.getSimpleName() + " (level: %d)", level);
    }
}
