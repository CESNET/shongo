package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.annotation.Required;

/**
 * Represents a person in Shongo.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Person extends IdentifiedObject
{
    /**
     * Name of the person.
     */
    private String name;

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
