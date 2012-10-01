package cz.cesnet.shongo.jade.ontology;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;

/**
 * Command to dial a device.
 *
 * TODO: generalize to be able to hold an IP address as well
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class Dial extends ConnectorAgentAction
{
    private Alias alias;

    public Dial()
    {
    }

    public Dial(Alias alias)
    {
        this.alias = alias;
    }

    // FIXME: interpret as address (i.e., IP address)
    public Dial(String h323Number)
    {
        alias = new Alias(Technology.H323, AliasType.E164, h323Number);
    }

    public Alias getAlias()
    {
        return alias;
    }

    public void setAlias(Alias alias)
    {
        this.alias = alias;
    }

    public Object exec(CommonService connector) throws CommandException, CommandUnsupportedException
    {
        logger.info("Dialing {}", alias);
        return getEndpoint(connector).dial(alias);
    }

    public String toString()
    {
        return String.format("Dial agent action (alias: %s)", alias);
    }
}
