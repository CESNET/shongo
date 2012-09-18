package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.annotation.Required;

/**
 * {@link Specification} for {@link Person}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class PersonSpecification extends Specification
{
    /**
     * The requested person.
     */
    public static final String PERSON = "PERSON";

    /**
     * Constructor.
     */
    public PersonSpecification()
    {
    }

    /**
     * Constructor.
     *
     * @param name sets the {@link Person#name} for the {@link #PERSON}
     * @param email sets the {@link Person#email} for the {@link #PERSON}
     */
    public PersonSpecification(String name, String email)
    {
        setPerson(new Person(name, email));
    }

    /**
     * @return {@link #PERSON}
     */
    @Required
    public String getPerson()
    {
        return getPropertyStorage().getValue(PERSON);
    }

    /**
     * @param person sets the {@link #PERSON}
     */
    public void setPerson(Person person)
    {
        getPropertyStorage().setValue(PERSON, person);
    }
}
