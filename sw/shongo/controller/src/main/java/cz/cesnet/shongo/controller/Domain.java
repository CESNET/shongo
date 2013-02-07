package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.controller.api.Status;

import java.util.regex.Pattern;

/**
 * Holds information about domain for which the controller is running.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Domain
{
    /**
     * Represents an unique domain name (e.g., "cz.cesnet")
     */
    private String name;

    /**
     * Represents a user-visible domain organization (e.g., "CESNET, z.s.p.o.").
     */
    private String organization;

    /**
     * Constructor.
     */
    public Domain()
    {
    }

    /**
     * Constructor.
     *
     * @param name sets the {@link #name}
     */
    public Domain(String name)
    {
        setName(name);
    }

    /**
     * Constructor.
     *
     * @param name         sets the {@link #name}
     * @param organization sets the {@link #organization}
     */
    public Domain(String name, String organization)
    {
        setName(name);
        setOrganization(organization);
    }

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
     * @return domain converted to API
     */
    public cz.cesnet.shongo.controller.api.Domain toApi()
    {
        cz.cesnet.shongo.controller.api.Domain apiDomain = new cz.cesnet.shongo.controller.api.Domain();
        apiDomain.setName(getName());
        apiDomain.setOrganization(getOrganization());
        apiDomain.setStatus(Status.AVAILABLE);
        return apiDomain;
    }

    /**
     * Local domain of {@link Controller#instance}.
     */
    private static Domain localDomain = null;

    /**
     * @return {@link #localDomain}
     */
    public static void setLocalDomain(Domain domain)
    {
        if (domain != null && localDomain != null) {
            throw new IllegalStateException("Local domain is already defined.");
        }
        localDomain = domain;
    }

    /**
     * @return {@link #localDomain}
     */
    public static Domain getLocalDomain()
    {
        if (localDomain == null) {
            throw new IllegalStateException("No local domain is defined.");
        }
        return localDomain;
    }

    /**
     * @return {@link #localDomain#getName()}
     */
    public static String getLocalDomainName()
    {
        if (localDomain == null) {
            throw new IllegalStateException("No local domain is defined.");
        }
        return localDomain.getName();
    }
}
