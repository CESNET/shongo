package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.api.Technology;
import cz.cesnet.shongo.controller.resource.Alias;

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
     * List of aliases that can be used to reference the external endpoint.
     */
    private List<Alias> aliases = new ArrayList<Alias>();

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
     * @param alias
     */
    public ExternalEndpointSpecification(Technology technology, Alias alias)
    {
        if (technology == null) {
            throw new IllegalArgumentException("Technology cannot be null!");
        }
        if (alias.getTechnology() == null) {
            alias.setTechnology(technology);
        }
        else if (!technology.equals(alias.getTechnology())) {
            throw new IllegalArgumentException("Cannot use alias for technology '" + alias.getTechnology().getName()
                    + "' for an external endpoint with technology '" + technology.getName() + "!");
        }
        addTechnology(technology);

        addAlias(alias);
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

    /**
     * @return {@link #aliases}
     */
    @OneToMany(cascade = CascadeType.ALL)
    @Access(AccessType.FIELD)
    public List<Alias> getAliases()
    {
        return aliases;
    }

    /**
     * @param alias alias to be added to the {@link #aliases}
     */
    public void addAlias(Alias alias)
    {
        aliases.add(alias);
    }

    /**
     * @param alias alias to be removed from the {@link #aliases}
     */
    public void removeAlias(Alias alias)
    {
        aliases.remove(alias);
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
