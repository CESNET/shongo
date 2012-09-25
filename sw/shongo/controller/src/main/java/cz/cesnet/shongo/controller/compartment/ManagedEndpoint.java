package cz.cesnet.shongo.controller.compartment;

/**
 * Represents an {@link Endpoint} which is managed by a connector.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public interface ManagedEndpoint
{
    /**
     * @return name of connector agent which is managing this endpoint
     */
    public String getConnectorAgentName();
}
