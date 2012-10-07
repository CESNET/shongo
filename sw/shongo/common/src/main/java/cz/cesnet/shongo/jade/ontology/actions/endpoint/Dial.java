package cz.cesnet.shongo.jade.ontology.actions.endpoint;

import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.jade.ontology.ConnectorAgentAction;

/**
 * Command to dial a device.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class Dial extends ConnectorAgentAction
{
    // either alias or address will be used
    private Alias alias = null;
    private String address = null;

    public Dial()
    {
    }

    public Dial(String address)
    {
        this.address = address;
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

    public String getAddress()
    {
        return address;
    }

    public void setAddress(String address)
    {
        this.address = address;
    }

    public Object exec(CommonService connector) throws CommandException, CommandUnsupportedException
    {
        if (alias != null && address != null) {
            throw new IllegalStateException("Both alias and address set for the Dial command - should be just one.");
        }

        if (alias != null) {
            logger.info("Dialing alias {}", alias);
            return getEndpoint(connector).dial(alias);
        }
        else {
            logger.info("Dialing address {}", address);
            return getEndpoint(connector).dial(address);
        }
    }

    public String toString()
    {
        if (alias != null && address != null) {
            throw new IllegalStateException("Both alias and address set for the Dial command - should be just one.");
        }

        String target;
        if (alias != null) {
            target = "alias: " + alias;
        }
        else {
            target = "address: " + address;
        }
        return String.format("Dial agent action (%s)", target);
    }
}
