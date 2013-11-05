package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.controller.executor.Endpoint;
import cz.cesnet.shongo.controller.executor.ExecutableManager;
import cz.cesnet.shongo.util.ObjectHelper;

import javax.persistence.*;

/**
 * Specification of a service for some the {@link #endpoint}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public abstract class EndpointServiceSpecification extends Specification
{
    /**
     * {@link Endpoint} for which the service should be allocated.
     */
    private Endpoint endpoint;

    /**
     * Specifies whether the service should be automatically enabled for the booked time slot.
     */
    private boolean enabled;

    /**
     * @return {@link #endpoint}
     */
    @ManyToOne
    @Access(AccessType.FIELD)
    public Endpoint getEndpoint()
    {
        return endpoint;
    }

    /**
     * @param endpoint sets the {@link #endpoint}
     */
    public void setEndpoint(Endpoint endpoint)
    {
        this.endpoint = endpoint;
    }

    /**
     * @return {@link #enabled}
     */
    @Column(nullable = false, columnDefinition = "boolean default false")
    public boolean isEnabled()
    {
        return enabled;
    }

    /**
     * @param enabled sets the {@link #enabled}
     */
    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    @Override
    public EndpointServiceSpecification clone()
    {
        return (EndpointServiceSpecification) super.clone();
    }

    @Override
    public boolean synchronizeFrom(Specification specification)
    {
        EndpointServiceSpecification endpointServiceSpecification = (EndpointServiceSpecification) specification;

        boolean modified = super.synchronizeFrom(specification);
        modified |= !ObjectHelper.isSame(getEndpoint(), endpointServiceSpecification.getEndpoint());

        setEndpoint(endpointServiceSpecification.getEndpoint());

        return modified;
    }

    @Override
    public cz.cesnet.shongo.controller.api.EndpointServiceSpecification toApi()
    {
        return (cz.cesnet.shongo.controller.api.EndpointServiceSpecification) super.toApi();
    }

    @Override
    public void toApi(cz.cesnet.shongo.controller.api.Specification specificationApi)
    {
        super.toApi(specificationApi);

        cz.cesnet.shongo.controller.api.EndpointServiceSpecification endpointServiceSpecificationApi =
                (cz.cesnet.shongo.controller.api.EndpointServiceSpecification) specificationApi;

        if (endpoint != null) {
            endpointServiceSpecificationApi.setEndpointId(EntityIdentifier.formatId(endpoint));
        }
        endpointServiceSpecificationApi.setEnabled(enabled);
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.Specification specificationApi, EntityManager entityManager)
    {
        super.fromApi(specificationApi,entityManager);

        cz.cesnet.shongo.controller.api.EndpointServiceSpecification endpointServiceSpecificationApi =
                (cz.cesnet.shongo.controller.api.EndpointServiceSpecification) specificationApi;

        String endpointId = endpointServiceSpecificationApi.getEndpointId();
        if (endpointId == null) {
            setEndpoint(null);
        }
        else {
            Long executableId = EntityIdentifier.parseId(cz.cesnet.shongo.controller.executor.Executable.class,
                    endpointId);
            ExecutableManager executableManager = new ExecutableManager(entityManager);
            setEndpoint((Endpoint) executableManager.get(executableId));
        }

        setEnabled(endpointServiceSpecificationApi.isEnabled());
    }
}
