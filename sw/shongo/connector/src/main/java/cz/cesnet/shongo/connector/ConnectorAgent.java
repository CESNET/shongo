package cz.cesnet.shongo.connector;

import cz.cesnet.shongo.jade.Agent;

/**
 * Jade Agent for Device Connector
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ConnectorAgent extends Agent
{
    @Override
    protected void setup()
    {
        super.setup();

        registerService("connector", "Connector Service");
    }
}
