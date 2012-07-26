package cz.cesnet.shongo.controller.api;

/**
 * Represents an information about known connector in a controlled domain.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Connector
{
    /**
     * Status of a connector.
     */
    public static enum Status
    {
        /**
         * Means that a connector is currently available to the controller.
         */
        AVAILABLE,

        /**
         * Means that a connector is currently not available to the controller.
         */
        NOT_AVAILABLE
    }

    /**
     * A unique connector name within the domain (Jade agent name).
     */
    private String name;

    /**
     * Identifier of a resource which is managed by the connector.
     */
    private String resourceIdentifier;

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
     * @return {@link #resourceIdentifier}
     */
    public String getResourceIdentifier()
    {
        return resourceIdentifier;
    }

    /**
     * @param resourceIdentifier sets the {@link #resourceIdentifier}
     */
    void setResourceIdentifier(String resourceIdentifier)
    {
        this.resourceIdentifier = resourceIdentifier;
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
