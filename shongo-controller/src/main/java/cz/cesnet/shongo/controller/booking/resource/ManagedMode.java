package cz.cesnet.shongo.controller.booking.resource;

import cz.cesnet.shongo.api.AbstractComplexType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;

/**
 * Represents a device mode in which the device
 * is managed by a connector.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
@Table(indexes = { @Index(name = "connector_agent_name", columnList = "connector_agent_name") })
public class ManagedMode extends Mode
{
    /**
     * Connector Jade agent name.
     */
    private String connectorAgentName;

    /**
     * @return {@link #connectorAgentName}
     */
    @Column(length = AbstractComplexType.DEFAULT_COLUMN_LENGTH)
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
