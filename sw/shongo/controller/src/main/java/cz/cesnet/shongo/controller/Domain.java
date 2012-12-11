package cz.cesnet.shongo.controller;

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
     * @param id
     * @return formatted shongo-id from given database id
     */
    public String formatId(Long id)
    {
        if (id == null) {
            throw new IllegalArgumentException("Cannot format identifier because id is null!");
        }
        return String.format("shongo:%s:%d", getName(), id.longValue());
    }

    /**
     * @param id
     * @return parse database id from shongo-id
     */
    public Long parseId(String id)
    {
        if (Pattern.matches("\\d+", id)) {
            return Long.parseLong(id);
        }
        String prefix = String.format("shongo:%s:", getName());
        if (!id.startsWith(prefix)) {
            throw new IllegalArgumentException(String.format("The identifier '%s' doesn't belong to domain '%s'!",
                    id, getName()));
        }
        return Long.parseLong(id.substring(prefix.length(), id.length()));
    }

    /**
     * @return domain converted to API
     */
    public cz.cesnet.shongo.controller.api.Domain toApi()
    {
        cz.cesnet.shongo.controller.api.Domain apiDomain = new cz.cesnet.shongo.controller.api.Domain();
        apiDomain.setName(getName());
        apiDomain.setOrganization(getOrganization());
        apiDomain.setStatus(cz.cesnet.shongo.controller.api.Domain.Status.AVAILABLE);
        return apiDomain;
    }
}
