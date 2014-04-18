package cz.cesnet.shongo.connector.api.jade.endpoint;

import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.jade.ConnectorCommand;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class ShowMessage extends ConnectorCommand
{
    private String text;
    private int duration;

    public ShowMessage()
    {
    }

    public ShowMessage(int duration, String text)
    {
        this.duration = duration;
        this.text = text;
    }

    public int getDuration()
    {
        return duration;
    }

    public void setDuration(int duration)
    {
        this.duration = duration;
    }

    public String getText()
    {
        return text;
    }

    public void setText(String text)
    {
        this.text = text;
    }

    @Override
    public Object execute(CommonService connector) throws CommandException, CommandUnsupportedException
    {
        logger.debug("Showing message {} for {}", text, duration);
        getEndpoint(connector).showMessage(duration, text);
        return null;
    }

    public String toString()
    {
        return String.format(ShowMessage.class.getSimpleName() + " (duration: %d, text: '%s')", duration, text);
    }
}
