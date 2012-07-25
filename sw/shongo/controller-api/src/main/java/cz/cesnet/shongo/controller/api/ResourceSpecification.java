package cz.cesnet.shongo.controller.api;

import java.util.List;

/**
 * Specification of requested resource.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ResourceSpecification extends IdentifiedChangeableObject
{
    /**
     * Collection of requested persons for the resource.
     */
    public static final String PERSONS = "persons";

    /**
     * @return {@link #PERSONS}
     */
    public List<Person> getPersons()
    {
        return getPropertyStorage().getCollection(PERSONS, List.class);
    }

    /**
     * @param persons sets the {@link #PERSONS}
     */
    public void setPersons(List<Person> persons)
    {
        getPropertyStorage().setCollection(PERSONS, persons);
    }

    /**
     * @param person person to be added to the {@link #PERSONS}
     */
    public void addPerson(Person person)
    {
        getPropertyStorage().addCollectionItem(PERSONS, person, List.class);
    }
}
