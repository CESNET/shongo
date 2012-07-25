package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.Technology;

import java.util.List;

/**
 * Represents a requested compartment in reservation request.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Compartment extends IdentifiedChangeableObject
{
    /**
     * Collection of requested persons for the compartment.
     */
    public static final String PERSONS = "persons";

    /**
     * Collection of requested reosurces for the compartment.
     */
    public static final String RESOURCES = "resources";

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

    /**
     * Adds new person to the {@link #PERSONS}.
     *
     * @param name
     * @param email
     */
    public void addPerson(String name, String email)
    {
        addPerson(new Person(name, email));
    }

    /**
     * @return {@link #RESOURCES}
     */
    public List<ResourceSpecification> getResources()
    {
        return getPropertyStorage().getCollection(RESOURCES, List.class);
    }

    /**
     * @param resources {@link #RESOURCES}
     */
    public void setResources(List<ResourceSpecification> resources)
    {
        getPropertyStorage().setCollection(RESOURCES, resources);
    }

    /**
     * Adds new requested resource.
     *
     * @param resourceSpecification
     */
    public void addResource(ResourceSpecification resourceSpecification)
    {
        getPropertyStorage().addCollectionItem(RESOURCES, resourceSpecification, List.class);
    }

    /**
     * Adds new requested external resource(s).
     *
     * @param technology
     * @param count
     * @param persons
     */
    public void addResource(Technology technology, int count, Person[] persons)
    {
        ExternalEndpointSpecification externalEndpointSpecification = new ExternalEndpointSpecification();
        externalEndpointSpecification.setTechnology(technology);
        externalEndpointSpecification.setCount(count);
        for (Person person : persons) {
            externalEndpointSpecification.addPerson(person);
        }
        addResource(externalEndpointSpecification);
    }
}
