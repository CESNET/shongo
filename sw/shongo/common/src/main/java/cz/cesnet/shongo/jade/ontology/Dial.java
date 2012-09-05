package cz.cesnet.shongo.jade.ontology;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import jade.content.Concept;

/**
 * Command to dial a device.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class Dial extends ConnectorAgentAction
{
    private String number;

    public Dial()
    {
    }

    public Dial(String number)
    {
        this.number = number;
    }

    public String getNumber()
    {
        return number;
    }

    public void setNumber(String number)
    {
        this.number = number;
    }

    public Concept exec(CommonService connector) throws CommandException, CommandUnsupportedException
    {
        getEndpoint(connector).dial(new Alias(Technology.H323, AliasType.E164, number));
        return null;
    }
}
