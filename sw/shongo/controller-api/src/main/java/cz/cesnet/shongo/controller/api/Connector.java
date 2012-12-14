package cz.cesnet.shongo.controller.api;

/**
 * Represents an information about known connector in a controlled domain.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Connector
{
    /**
     * A unique connector name within the domain (Jade agent name).
     */
    private String name;

    /**
     * Id of a resource which is managed by the connector.
     */
    private String resourceId;

    /**
     * Status of the connector.
     */
    private Status status;

    /**
     * @return {@link #name}
     */
    public String getName()
    {
        return name;
    }

    /**
     * @param name sets the {@link #name}
     */
    void setName(String name)
    {
        this.name = name;
    }

    /**
     * @return {@link #resourceId}
     */
    public String getResourceId()
    {
        return resourceId;
    }

    /**
     * @param resourceId sets the {@link #resourceId}
     */
    void setResourceId(String resourceId)
    {
        this.resourceId = resourceId;
    }

    /**
     * @return {@link #status}
     */
    public Status getStatus()
    {
        return status;
    }

    /**
     * @param status sets the {@link #status}
     */
    public void setStatus(Status status)
    {
        this.status = status;
    }
}
