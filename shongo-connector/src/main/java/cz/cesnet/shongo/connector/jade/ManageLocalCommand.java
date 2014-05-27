package cz.cesnet.shongo.connector.jade;

import cz.cesnet.shongo.connector.api.ConnectorConfiguration;
import cz.cesnet.shongo.connector.api.ConnectorInitException;
import cz.cesnet.shongo.jade.Agent;
import cz.cesnet.shongo.jade.LocalCommand;
import cz.cesnet.shongo.jade.LocalCommandException;

/**
 * {@link LocalCommand} for starting managing a device by a connector agent.
 * <p/>
 * Initializes the agent to manage a device and connects to it.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class ManageLocalCommand extends LocalCommand
{
    /**
     * @see ConnectorConfiguration
     */
    private final ConnectorConfiguration connectorConfiguration;

    public ManageLocalCommand(ConnectorConfiguration connectorConfiguration)
    {
        this.connectorConfiguration = connectorConfiguration;
    }

    public ConnectorConfiguration getConnectorConfiguration()
    {
        return connectorConfiguration;
    }

    @Override
    public void process(Agent localAgent) throws LocalCommandException
    {
        if (!(localAgent instanceof ConnectorAgent)) {
            throw new IllegalArgumentException("Manage command works only with instances of " + ConnectorAgent.class);
        }
        ConnectorAgent connectorAgent = (ConnectorAgent) localAgent;
        try {
            connectorAgent.manage(connectorConfiguration);
        }
        catch (ConnectorInitException exception) {
            throw new LocalCommandException("Error initializing the connector", exception);
        }
    }
}
