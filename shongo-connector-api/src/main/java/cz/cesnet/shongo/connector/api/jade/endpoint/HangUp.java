package cz.cesnet.shongo.connector.api.jade.endpoint;

import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.jade.ConnectorCommand;

/**
 * Command to hang up a given call.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class HangUp extends ConnectorCommand
{
    private String callId;

    public HangUp()
    {
    }

    public HangUp(String callId)
    {
        this.callId = callId;
    }

    public String getCallId()
    {
        return callId;
    }

    public void setCallId(String callId)
    {
        this.callId = callId;
    }

    @Override
    public Object execute(CommonService connector) throws CommandException, CommandUnsupportedException
    {
        logger.debug("Hanging up call {}", callId);
        getEndpoint(connector).hangUp(callId);
        return null;
    }

    public String toString()
    {
        return String.format(HangUp.class.getSimpleName() + " (callId: %s)", callId);
    }
}
