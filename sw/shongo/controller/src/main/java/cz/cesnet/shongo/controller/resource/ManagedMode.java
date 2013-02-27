package cz.cesnet.shongo.controller.resource;

import javax.persistence.Column;
import javax.persistence.Entity;

/**
 * Represents a device mode in which the device
 * is managed by a connector.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class ManagedMode extends Mode
{
    /**
     * Connector Jade agent name.
     */
    private String connectorAgentName;

    /**
     * @return {@link #connectorAgentName}
     */
    @Column
    @org.hibernate.annotations.Index(name="connector_agent_name")
    public String getConnectorAgentName()
    {
        return connectorAgentName;
    }

    /**
     * @param connectorAgentName Sets the {@link #connectorAgentName}
     */
    public void setConnectorAgentName(String connectorAgentName)
    {
        this.connectorAgentName = connectorAgentName;
    }
}
