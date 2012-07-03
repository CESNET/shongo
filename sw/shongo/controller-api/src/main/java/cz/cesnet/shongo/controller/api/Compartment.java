package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.controller.Technology;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * TODO:
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
     * List of requested persons for the compartment.
     */
    private List<Person> persons = new ArrayList<Person>();

    /**
     * List of requested reosurces for the compartment.
     */
    private List<ResourceSpecificationMap> resources = new ArrayList<ResourceSpecificationMap>();

    /**
     * @return {@link #persons}
     */
    public List<Person> getPersons()
    {
        return persons;
    }

    /**
     * @param persons sets the {@link #persons}
     */
    public void setPersons(List<Person> persons)
    {
        this.persons = persons;
    }

    /**
     * Adds new person to the {@link #persons}.
     *
     * @param name
     * @param email
     */
    public void addPerson(String name, String email)
    {
        getPersons().add(new Person(name, email));
    }

    /**
     * @return {@link #resources}
     */
    public List<ResourceSpecificationMap> getResources()
    {
        return resources;
    }

    /**
     * @param resources {@link #resources}
     */
    public void setResources(List<ResourceSpecificationMap> resources)
    {
        this.resources = resources;
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
        resources.add(resourceSpecificationMap);
    }
}
