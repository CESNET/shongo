package cz.cesnet.shongo.jade;

/**
 * Represents a command which can be processed on a local JADE agent.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class LocalCommand
{
    /**
     * @return name of the {@link LocalCommand} which can be used for printing
     */
    public String getName()
    {
        return getClass().getSimpleName();
    }

    /**
     * Process this {@link LocalCommand} on an given {@code localAgent}
     *
     * @param localAgent which should be used for processing this {@link LocalCommand}
     */
    public abstract void process(Agent localAgent) throws LocalCommandException;
}
