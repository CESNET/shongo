package cz.cesnet.shongo.connector;

import org.apache.commons.configuration.CombinedConfiguration;
import org.apache.commons.configuration.tree.NodeCombiner;
import org.apache.commons.configuration.tree.UnionCombiner;
import org.joda.time.Duration;
import org.joda.time.Period;

/**
 * Configuration for the {@link Connector}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ConnectorConfiguration extends CombinedConfiguration
{
    /**
     * Configuration parameters names.
     */
    public static final String CONTROLLER_HOST = "controller.host";
    public static final String CONTROLLER_PORT = "controller.port";
    public static final String CONTROLLER_CONNECTION_CHECK_PERIOD = "controller.connection-check-period";
    public static final String JADE_HOST = "jade.host";
    public static final String JADE_PORT = "jade.port";

    /**
     * Constructor.
     */
    public ConnectorConfiguration()
    {
        NodeCombiner nodeCombiner = new UnionCombiner();
        nodeCombiner.addListNode("instance");
        nodeCombiner.addListNode("participant");
        setNodeCombiner(nodeCombiner);
    }

    /**
     * @see {@link #getString(String)}
     */
    public Duration getDuration(String key)
    {
        String value = getString(key);
        if (value == null) {
            return null;
        }
        return Period.parse(value).toStandardDuration();
    }

    /**
     * @return timeout to receive response when performing commands from agent
     */
    public Duration getJadeCommandTimeout()
    {
        return getDuration("jade.command-timeout");
    }
}
