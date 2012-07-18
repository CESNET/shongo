package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.controller.Technology;

import java.util.HashMap;
import java.util.List;

/**
 * Represents a requested compartment in reservation request.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Compartment extends ComplexType
{
    /**
     * Map that represents a resource specification.
     */
    public static class ResourceSpecificationMap extends HashMap<String, Object>
    {
    }

    /**
     * Collection of requested persons for the compartment.
     */
    public final String PERSONS = "persons";

    /**
     * Collection of requested reosurces for the compartment.
     */
    public final String RESOURCES = "resources";

    /**
     * @return {@link #PERSONS}
     */
    public List<Person> getPersons()
    {
        return propertyStore.getCollection(PERSONS);
    }

    /**
     * @param persons sets the {@link #PERSONS}
     */
    private void setPersons(List<Person> persons)
    {
        propertyStore.setCollection(PERSONS, persons);
    }

    /**
     * Adds new person to the {@link #PERSONS}.
     *
     * @param name
     * @param email
     */
    public void addPerson(String name, String email)
    {
        propertyStore.addCollectionItem(PERSONS, new Person(name, email));
    }

    /**
     * @return {@link #RESOURCES}
     */
    public List<ResourceSpecificationMap> getResources()
    {
        return propertyStore.getCollection(RESOURCES);
    }

    /**
     * @param resources {@link #RESOURCES}
     */
    public void setResources(List<ResourceSpecificationMap> resources)
    {
        propertyStore.setCollection(RESOURCES, resources);
    }

    /**
     * Adds new external resources definition.
     *
     * @param technology
     * @param count
     * @param persons
     */
    public void addResource(Technology technology, int count, Person[] persons)
    {
        ResourceSpecificationMap resourceSpecificationMap = new ResourceSpecificationMap();
        resourceSpecificationMap.put("technology", technology);
        resourceSpecificationMap.put("count", count);
        resourceSpecificationMap.put("persons", persons);
        propertyStore.addCollectionItem(RESOURCES, resourceSpecificationMap);
    }
}
