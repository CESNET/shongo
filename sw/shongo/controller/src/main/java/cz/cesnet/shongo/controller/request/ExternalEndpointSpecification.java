package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.resource.Alias;
import org.apache.commons.lang.ObjectUtils;

import javax.persistence.*;
import java.util.*;

/**
 * Represents an external (not existing) {@link EndpointSpecification}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class ExternalEndpointSpecification extends EndpointSpecification
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
     * @param technologies sets the {@link #technologies}
     */
    public void setTechnologies(Set<Technology> technologies)
    {
        this.technologies = technologies;
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
     * Remove all technologies from the specification.
     */
    public void removeAllTechnologies()
    {
        technologies.clear();
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
     * @param aliases sets the {@link #aliases}
     */
    public void setAliases(List<Alias> aliases)
    {
        this.aliases = aliases;
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
    public boolean synchronizeFrom(Specification specification)
    {
        ExternalEndpointSpecification externalEndpointSpecification = (ExternalEndpointSpecification) specification;

        boolean modified = super.synchronizeFrom(specification);
        modified |= !ObjectUtils.equals(getTechnologies(), externalEndpointSpecification.getTechnologies())
                || !ObjectUtils.equals(getCount(), externalEndpointSpecification.getCount())
                || !ObjectUtils.equals(getAliases(), externalEndpointSpecification.getAliases());

        setTechnologies(externalEndpointSpecification.getTechnologies());
        setCount(externalEndpointSpecification.getCount());
        setAliases(externalEndpointSpecification.getAliases());

        return modified;
    }

    @Override
    protected void fillDescriptionMap(Map<String, Object> map)
    {
        super.fillDescriptionMap(map);

        map.put("technologies", technologies);
        map.put("count", count);
    }

    /*@Override
    public cz.cesnet.shongo.controller.api.ResourceSpecification toApi(Domain domain) throws FaultException
    {
        cz.cesnet.shongo.controller.api.ExternalEndpointSpecification api =
                new cz.cesnet.shongo.controller.api.ExternalEndpointSpecification();

        api.setCount(getCount());

        if (technologies.size() == 1) {
            api.setTechnology(technologies.iterator().next());
        }
        else {
            throw new TodoImplementException();
        }

        super.toApi(api);

        return api;
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.ResourceSpecification api, EntityManager entityManager,
            Domain domain) throws FaultException
    {
        cz.cesnet.shongo.controller.api.ExternalEndpointSpecification apiExternalEndpoint =
                (cz.cesnet.shongo.controller.api.ExternalEndpointSpecification) api;
        if (apiExternalEndpoint.isPropertyFilled(apiExternalEndpoint.TECHNOLOGY)) {
            technologies.clear();
            addTechnology(apiExternalEndpoint.getTechnology());
        }
        if (apiExternalEndpoint.isPropertyFilled(apiExternalEndpoint.COUNT)) {
            setCount(apiExternalEndpoint.getCount());
        }
        super.fromApi(api, entityManager, domain);
    }*/
}
