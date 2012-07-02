package cz.cesnet.shongo.api;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Person extends ComplexType
{
    public String name;

    public String email;

    public static Person create(String name, String email)
    {
        Person person = new Person();
        person.name = name;
        person.email = email;
        return person;
    }
}
