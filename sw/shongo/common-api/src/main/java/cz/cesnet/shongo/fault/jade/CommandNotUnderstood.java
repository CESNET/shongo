package cz.cesnet.shongo.fault.jade;

/**
 * Represents a {@link CommandFailure} which happens when requester sends an action
 * which is not defined in the ontology.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class CommandNotUnderstood extends CommandFailure
{
    @Override
    public String getMessage()
    {
        return "Command was not understood.";
    }
}
