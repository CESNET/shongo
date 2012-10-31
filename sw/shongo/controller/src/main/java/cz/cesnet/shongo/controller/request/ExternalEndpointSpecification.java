package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.executor.Endpoint;
import cz.cesnet.shongo.controller.executor.EndpointProvider;
import cz.cesnet.shongo.controller.executor.ExternalEndpoint;
import cz.cesnet.shongo.controller.resource.Alias;
import cz.cesnet.shongo.fault.FaultException;
import cz.cesnet.shongo.fault.TodoImplementException;
import org.apache.commons.lang.ObjectUtils;

import javax.persistence.*;
import java.util.*;

/**
 * Represents an external (not existing in resource database) {@link EndpointSpecification}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class ExternalEndpointSpecification extends EndpointSpecification implements EndpointProvider
{
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
        this.aliases.clear();
        for (Alias alias : aliases) {
            this.aliases.add(alias);
        }
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

        if (!technologies.equals(externalEndpointSpecification.getTechnologies())) {
            setTechnologies(externalEndpointSpecification.getTechnologies());
            modified = true;
        }
        if (!ObjectUtils.equals(getAliases(), externalEndpointSpecification.getAliases())) {
            setAliases(externalEndpointSpecification.getAliases());
            modified = true;
        }

        return modified;
    }

    @Override
    public Endpoint createEndpoint()
    {
        return new ExternalEndpoint(this);
    }

    @Override
    protected cz.cesnet.shongo.controller.api.Specification createApi()
    {
        return new cz.cesnet.shongo.controller.api.ExternalEndpointSpecification();
    }

    @Override
    public void toApi(cz.cesnet.shongo.controller.api.Specification specificationApi, Domain domain)
    {
        cz.cesnet.shongo.controller.api.ExternalEndpointSpecification externalEndpointSpecificationApi =
                (cz.cesnet.shongo.controller.api.ExternalEndpointSpecification) specificationApi;
        if (technologies.size() == 1) {
            externalEndpointSpecificationApi.setTechnology(technologies.iterator().next());
        }
        else {
            throw new TodoImplementException("Allow multiple technologies in external endpoint specification in API.");
        }
        if (aliases.size() == 1) {
            externalEndpointSpecificationApi.setAlias(aliases.iterator().next().toApi());
        }
        else {
            throw new TodoImplementException("Allow multiple aliases in external endpoint specification in API.");
        }
        super.toApi(specificationApi, domain);
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.Specification specificationApi, EntityManager entityManager,
            Domain domain)
            throws FaultException
    {
        cz.cesnet.shongo.controller.api.ExternalEndpointSpecification externalEndpointSpecificationApi =
                (cz.cesnet.shongo.controller.api.ExternalEndpointSpecification) specificationApi;
        if (externalEndpointSpecificationApi.isPropertyFilled(externalEndpointSpecificationApi.TECHNOLOGY)) {
            technologies.clear();
            addTechnology(externalEndpointSpecificationApi.getTechnology());
        }
        if (externalEndpointSpecificationApi.isPropertyFilled(externalEndpointSpecificationApi.ALIAS)) {
            aliases.clear();
            Alias alias = new Alias();
            alias.fromApi(externalEndpointSpecificationApi.getAlias());
            addAlias(alias);
        }
        super.fromApi(specificationApi, entityManager, domain);
    }

    @Override
    protected void fillDescriptionMap(Map<String, Object> map)
    {
        super.fillDescriptionMap(map);

        map.put("technologies", technologies);
        map.put("aliases", aliases);
    }
}
