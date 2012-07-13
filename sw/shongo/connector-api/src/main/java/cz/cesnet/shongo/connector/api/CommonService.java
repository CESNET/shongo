package cz.cesnet.shongo.connector.api;

/**
 * Common connector API.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public interface CommonService
{
    /**
     * Get information about connector.
     * @return information about the connector
     */
    ConnectorInfo getConnectorInfo();

}
