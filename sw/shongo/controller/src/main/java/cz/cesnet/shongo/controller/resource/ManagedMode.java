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
     * Url for connector.
     */
    private String connectorUrl;

    /**
     * @return {@link #connectorUrl}
     */
    @Column
    public String getConnectorUrl()
    {
        return connectorUrl;
    }

    /**
     * @param connectorUrl Sets the {@link #connectorUrl}
     */
    public void setConnectorUrl(String connectorUrl)
    {
        this.connectorUrl = connectorUrl;
    }
}
