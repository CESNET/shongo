package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.executor.Endpoint;
import cz.cesnet.shongo.controller.executor.EndpointProvider;
import cz.cesnet.shongo.controller.executor.ExternalEndpointSet;
import org.apache.commons.lang.ObjectUtils;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Transient;
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

    @Override
    public boolean synchronizeFrom(Specification specification)
    {
        ExternalEndpointSetSpecification externalEndpointSpecification =
                (ExternalEndpointSetSpecification) specification;

        boolean modified = super.synchronizeFrom(specification);
        modified |= !ObjectUtils.equals(getCount(), externalEndpointSpecification.getCount());

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
    public void toApi(cz.cesnet.shongo.controller.api.Specification specificationApi)
    {
        cz.cesnet.shongo.controller.api.ExternalEndpointSetSpecification externalEndpointSetSpecificationApi =
                (cz.cesnet.shongo.controller.api.ExternalEndpointSetSpecification) specificationApi;
        externalEndpointSetSpecificationApi.setCount(getCount());
        for (Technology technology : getTechnologies()) {
            externalEndpointSetSpecificationApi.addTechnology(technology);
        }
        super.toApi(specificationApi);
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.Specification specificationApi, EntityManager entityManager)
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

        super.fromApi(specificationApi, entityManager);
    }
}
