package cz.cesnet.shongo.connector.api.jade.endpoint;

import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.jade.ConnectorCommand;

/**
 * Command to dial a device.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class Dial extends ConnectorCommand
{
    private Alias alias = null;

    public Dial()
    {
    }

    public Dial(Alias alias)
    {
        this.alias = alias;
    }

    public Alias getAlias()
    {
        return alias;
    }

    public void setAlias(Alias alias)
    {
        this.alias = alias;
    }

    public Object execute(CommonService connector) throws CommandException, CommandUnsupportedException
    {
        if (alias == null) {
            throw new IllegalStateException("Alias should be set.");
        }
        logger.debug("Dialing {}", alias);
        return getEndpoint(connector).dial(alias);
    }

    public String toString()
    {
        return String.format(Dial.class.getSimpleName() + " (alias: %s)", alias.toString());
    }
}
