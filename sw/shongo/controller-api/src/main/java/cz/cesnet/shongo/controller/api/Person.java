package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.annotation.Required;
import cz.cesnet.shongo.api.util.IdentifiedObject;

/**
 * Represents a person in Shongo.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Person extends IdentifiedObject
{
    /**
     * User-id of the person.
     */
    private String userId;

    /**
     * Name of the person.
     */
    private String name;

    /**
     * Organization of the person.
     */
    private String organization;

    /**
     * Email for the person.
     */
    private String email;

    /**
     * Constructor.
     */
    public Person()
    {
    }

    /**
     * Constructor.
     *
     * @param name  sets the {@link #name}
     * @param email sets the {@link #email}
     */
    public Person(String name, String email)
    {
        setName(name);
        setEmail(email);
    }

    /**
     * @return {@link #userId}
     */
    public String getUserId()
    {
        return userId;
    }

    /**
     * @param userId sets the {@link #userId}
     */
    public void setUserId(String userId)
    {
        this.userId = userId;
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
     * @return {@link #email}
     */
    @Required
    public String getEmail()
    {
        return email;
    }

    /**
     * @param email sets the {@link #name}
     */
    public void setEmail(String email)
    {
        this.email = email;
    }
}
