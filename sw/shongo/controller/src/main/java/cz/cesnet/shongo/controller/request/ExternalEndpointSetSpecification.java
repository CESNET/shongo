package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.executor.Endpoint;
import cz.cesnet.shongo.controller.executor.EndpointProvider;
import cz.cesnet.shongo.controller.executor.ExternalEndpointSet;
import cz.cesnet.shongo.fault.FaultException;
import cz.cesnet.shongo.fault.TodoImplementException;
import org.apache.commons.lang.ObjectUtils;

import javax.persistence.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Represents an external (not existing) {@link cz.cesnet.shongo.controller.request.EndpointSpecification}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class ExternalEndpointSetSpecification extends ParticipantSpecification implements EndpointProvider
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
    public ExternalEndpointSetSpecification()
    {
    }

    /**
     * Constructor.
     *
     * @param technology
     */
    public ExternalEndpointSetSpecification(Technology technology)
    {
        addTechnology(technology);
    }

    /**
     * Constructor.
     *
     * @param technology
     * @param count
     */
    public ExternalEndpointSetSpecification(Technology technology, int count)
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
    @Override
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

    @Override
    public boolean synchronizeFrom(Specification specification)
    {
        ExternalEndpointSetSpecification externalEndpointSpecification =
                (ExternalEndpointSetSpecification) specification;

        boolean modified = super.synchronizeFrom(specification);
        modified |= !ObjectUtils.equals(getCount(), externalEndpointSpecification.getCount());

        if (!technologies.equals(externalEndpointSpecification.getTechnologies())) {
            setTechnologies(externalEndpointSpecification.getTechnologies());
            modified = true;
        }
        setCount(externalEndpointSpecification.getCount());

        return modified;
    }

    @Override
    @Transient
    public Endpoint getEndpoint()
    {
        return new ExternalEndpointSet(this);
    }

    @Override
    protected cz.cesnet.shongo.controller.api.Specification createApi()
    {
        return new cz.cesnet.shongo.controller.api.ExternalEndpointSetSpecification();
    }

    @Override
    public void toApi(cz.cesnet.shongo.controller.api.Specification specificationApi, Domain domain)
    {
        cz.cesnet.shongo.controller.api.ExternalEndpointSetSpecification externalEndpointSetSpecificationApi =
                (cz.cesnet.shongo.controller.api.ExternalEndpointSetSpecification) specificationApi;
        externalEndpointSetSpecificationApi.setCount(getCount());
        for (Technology technology : getTechnologies()) {
            externalEndpointSetSpecificationApi.addTechnology(technology);
        }
        super.toApi(specificationApi, domain);
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.Specification specificationApi, EntityManager entityManager,
            Domain domain)
            throws FaultException
    {
        cz.cesnet.shongo.controller.api.ExternalEndpointSetSpecification externalEndpointSetSpecificationApi =
                (cz.cesnet.shongo.controller.api.ExternalEndpointSetSpecification) specificationApi;
        // Create technologies
        for (Technology technology : externalEndpointSetSpecificationApi.getTechnologies()) {
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
        if (externalEndpointSetSpecificationApi.isPropertyFilled(externalEndpointSetSpecificationApi.COUNT)) {
            setCount(externalEndpointSetSpecificationApi.getCount());
        }
        super.fromApi(specificationApi, entityManager, domain);
    }

    @Override
    protected void fillDescriptionMap(Map<String, Object> map)
    {
        super.fillDescriptionMap(map);

        map.put("technologies", technologies);
        map.put("count", count);
    }
}
