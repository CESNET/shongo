package cz.cesnet.shongo.connector.api.ontology.actions.endpoint;

import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.ontology.ConnectorAgentAction;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class ShowMessage extends ConnectorAgentAction
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
    public Object exec(CommonService connector) throws CommandException, CommandUnsupportedException
    {
        getEndpoint(connector).showMessage(duration, text);
        return null;
    }

    public String toString()
    {
        return String.format("ShowMessage agent action (duration: %d, text: '%s')", duration, text);
    }
}
