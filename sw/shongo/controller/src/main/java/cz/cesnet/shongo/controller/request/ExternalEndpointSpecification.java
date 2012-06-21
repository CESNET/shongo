package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.common.Person;
import cz.cesnet.shongo.controller.resource.Technology;

import javax.persistence.*;
import java.util.*;

/**
 * Represents an external endpoint(s) that is/are specified to compartment.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class ExternalEndpointSpecification extends ResourceSpecification
{
    /**
     * Number of external endpoints of the same type.
     */
    private int count = 1;

    /**
     * Set of technologies for external endpoints.
     */
    private Set<Technology> technologies = new HashSet<Technology>();

    /**
     * Constructor.
     */
    public ExternalEndpointSpecification()
    {
    }

    /**
     * Constructor.
     *
     * @param technology
     */
    public ExternalEndpointSpecification(Technology technology)
    {
        addTechnology(technology);
    }

    /**
     * Constructor.
     *
     * @param technology
     * @param requestedPerson
     */
    public ExternalEndpointSpecification(Technology technology, Person requestedPerson)
    {
        addTechnology(technology);
        addRequestedPerson(requestedPerson);
    }

    /**
     * Constructor.
     *
     * @param technology
     * @param count
     */
    public ExternalEndpointSpecification(Technology technology, int count)
    {
        addTechnology(technology);
        setCount(count);
    }

    /**
     * @return {@link #count}
     */
    @Column(name = "same_count")
    public int getCount()
    {
        return count;
    }

    /**
     * @param count sets the {@link #count}
     */
    public void setCount(int count)
    {
        this.count = count;
    }

    /**
     * @return {@link #technologies}
     */
    @ElementCollection
    @Enumerated(EnumType.STRING)
    @Access(AccessType.FIELD)
    public Set<Technology> getTechnologies()
    {
        return Collections.unmodifiableSet(technologies);
    }

    /**
     * @param technology technology to be added to the {@link #technologies}
     */
    public void addTechnology(Technology technology)
    {
        technologies.add(technology);
    }

    /**
     * @param technology technology to be removed from the {@link #technologies}
     */
    public void removeTechnology(Technology technology)
    {
        technologies.remove(technology);
    }

    @Override
    protected void fillDescriptionMap(Map<String, String> map)
    {
        super.fillDescriptionMap(map);

        addCollectionToMap(map, "technologies", technologies);
        if (count != 1) {
            map.put("count", Integer.toString(count));
        }
    }
}
