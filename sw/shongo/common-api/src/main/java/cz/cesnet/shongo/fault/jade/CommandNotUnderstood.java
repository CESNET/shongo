package cz.cesnet.shongo.fault.jade;

import cz.cesnet.shongo.fault.old.CommonFault;

/**
 * Represents a {@link CommandFailure} which happens when requester sends an action
 * which is not defined in the ontology.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class CommandNotUnderstood extends CommandFailure
{
    @Override
    public int getCode()
    {
        return CommonFault.JADE_COMMAND_NOT_UNDERSTOOD;
    }

    @Override
    public String getMessage()
    {
        return "Command was not understood.";
    }
}
