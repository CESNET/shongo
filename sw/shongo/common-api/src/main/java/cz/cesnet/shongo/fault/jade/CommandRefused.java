package cz.cesnet.shongo.fault.jade;

import cz.cesnet.shongo.fault.CommonFault;
import jade.lang.acl.ACLMessage;

/**
 * Represents a {@link CommandFailure} which happens when a requester receives a {@link ACLMessage#REFUSE} response
 * (it can happen when a receiver send improper agent action).
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class CommandRefused extends CommandFailure
{
    @Override
    public int getCode()
    {
        return CommonFault.JADE_COMMAND_REFUSED;
    }

    @Override
    public String getMessage()
    {
        return "Command '%s' was refused.";
    }
}
