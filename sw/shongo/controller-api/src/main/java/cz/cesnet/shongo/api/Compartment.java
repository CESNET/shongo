package cz.cesnet.shongo.api;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Compartment extends ComplexType
{
    public List<Person> persons = new ArrayList<Person>();

    public void addPerson(String name, String email)
    {
        persons.add(Person.create(name, email));
    }
}
