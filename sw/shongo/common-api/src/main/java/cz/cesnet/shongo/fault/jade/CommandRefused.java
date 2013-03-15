package cz.cesnet.shongo.fault.jade;

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
    public String getMessage()
    {
        return "Command was refused.";
    }
}
