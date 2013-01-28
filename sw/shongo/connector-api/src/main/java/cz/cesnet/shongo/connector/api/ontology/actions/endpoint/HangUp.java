package cz.cesnet.shongo.connector.api.ontology.actions.endpoint;

import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.ontology.ConnectorAgentAction;

/**
 * Command to hang up a given call.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class HangUp extends ConnectorAgentAction
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
    public Object exec(CommonService connector) throws CommandException, CommandUnsupportedException
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
