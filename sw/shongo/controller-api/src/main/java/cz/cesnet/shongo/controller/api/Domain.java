package cz.cesnet.shongo.controller.api;

/**
 * Information about controlled or foreign domain.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Domain
{
    /**
     * Status of a domain.
     */
    public static enum Status
    {
        /**
         * Means that domain is currently available to the controller.
         */
        AVAILABLE,

        /**
         * Means that domain is currently not available to the controller.
         */
        NOT_AVAILABLE
    }

    /**
     * Represents an unique domain name (e.g., "cz.cesnet")
     */
    private String name;

    /**
     * Represents a user-visible domain organization (e.g., "CESNET, z.s.p.o.").
     */
    private String organization;

    /**
     * Status of the domain.
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
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * @return {@link #organization}
     */
    public String getOrganization()
    {
        return organization;
    }

    /**
     * @param organization sets the {@link #organization}
     */
    public void setOrganization(String organization)
    {
        this.organization = organization;
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
