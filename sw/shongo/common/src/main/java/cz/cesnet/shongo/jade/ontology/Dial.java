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

    // FIXME: enforce the technology, alias type and number in the API, instead just a number
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
        logger.info(String.format("Dialing %s:%s", alias.getTechnology(), alias.getValue()));
        return getEndpoint(connector).dial(alias);
    }

    public String toString()
    {
        return String.format("Dial agent action (technology: %s, alias type: %s, value: %s)",
                alias.getTechnology(), alias.getType(), alias.getValue()
                );
    }
}
