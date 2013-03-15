package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.executor.Endpoint;
import cz.cesnet.shongo.controller.executor.EndpointProvider;
import cz.cesnet.shongo.controller.executor.ExternalEndpoint;
import cz.cesnet.shongo.controller.resource.Alias;
import cz.cesnet.shongo.fault.old.OldFaultException;
import cz.cesnet.shongo.fault.TodoImplementException;
import org.apache.commons.lang.ObjectUtils;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents an external (not existing in resource database) {@link EndpointSpecification}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class ExternalEndpointSpecification extends EndpointSpecification implements EndpointProvider
{
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
        else if (!technology.equals(alias.getTechnology())) {
            throw new IllegalArgumentException("Cannot use alias for technology '" + alias.getTechnology().getName()
                    + "' for an external endpoint with technology '" + technology.getName() + "!");
        }
        addTechnology(technology);

        addAlias(alias);
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

        if (!ObjectUtils.equals(getAliases(), externalEndpointSpecification.getAliases())) {
            setAliases(externalEndpointSpecification.getAliases());
            modified = true;
        }

        return modified;
    }

    @Override
    @Transient
    public Endpoint getEndpoint()
    {
        return new ExternalEndpoint(this);
    }

    @Override
    protected cz.cesnet.shongo.controller.api.Specification createApi()
    {
        return new cz.cesnet.shongo.controller.api.ExternalEndpointSpecification();
    }

    @Override
    public void toApi(cz.cesnet.shongo.controller.api.Specification specificationApi)
    {
        cz.cesnet.shongo.controller.api.ExternalEndpointSpecification externalEndpointSpecificationApi =
                (cz.cesnet.shongo.controller.api.ExternalEndpointSpecification) specificationApi;
        for (Technology technology : getTechnologies()) {
            externalEndpointSpecificationApi.addTechnology(technology);
        }
        if (aliases.size() > 0) {
            if (aliases.size() == 1) {
                externalEndpointSpecificationApi.setAlias(aliases.iterator().next().toApi());
            }
            else {
                throw new TodoImplementException("Allow multiple aliases in external endpoint specification in API.");
            }
        }
        super.toApi(specificationApi);
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.Specification specificationApi, EntityManager entityManager)
            throws OldFaultException
    {
        cz.cesnet.shongo.controller.api.ExternalEndpointSpecification externalEndpointSpecificationApi =
                (cz.cesnet.shongo.controller.api.ExternalEndpointSpecification) specificationApi;
        // Create technologies
        for (Technology technology : externalEndpointSpecificationApi.getTechnologies()) {
            if (specificationApi.isPropertyItemMarkedAsNew(
                    cz.cesnet.shongo.controller.api.DeviceResource.TECHNOLOGIES, technology)) {
                addTechnology(technology);
            }
        }
        // Delete technologies
        Set<Technology> technologies = specificationApi.getPropertyItemsMarkedAsDeleted(
                cz.cesnet.shongo.controller.api.DeviceResource.TECHNOLOGIES);
        for (Technology technology : technologies) {
            removeTechnology(technology);
        }
        if (externalEndpointSpecificationApi.isPropertyFilled(externalEndpointSpecificationApi.ALIAS)) {
            aliases.clear();
            Alias alias = new Alias();
            alias.fromApi(externalEndpointSpecificationApi.getAlias());
            addAlias(alias);
        }

        super.fromApi(specificationApi, entityManager);
    }

    @Override
    protected void fillDescriptionMap(Map<String, Object> map)
    {
        super.fillDescriptionMap(map);

        map.put("aliases", aliases);
    }
}
