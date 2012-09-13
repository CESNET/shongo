package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.controller.resource.Resource;

import javax.persistence.Entity;
import javax.persistence.OneToOne;

/**
 * Represents a specific existing resource in the compartment.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class ExistingEndpointSpecification extends EndpointSpecification
{
    /**
     * Specific resource.
     */
    private Resource resource;

    /**
     * Constructor.
     */
    public ExistingEndpointSpecification()
    {
    }

    /**
     * Constructor.
     *
     * @param resource sets the {@link #resource}
     */
    public ExistingEndpointSpecification(Resource resource)
    {
        this.resource = resource;
    }

    /**
     * @return {@link #resource}
     */
    @OneToOne
    public Resource getResource()
    {
        return resource;
    }

    /**
     * @param resource sets the {@link #resource}
     */
    public void setResource(Resource resource)
    {
        this.resource = resource;
    }

    /*@Override
    public cz.cesnet.shongo.controller.api.ResourceSpecification toApi(Domain domain) throws FaultException
    {
        cz.cesnet.shongo.controller.api.ExistingResourceSpecification api =
                new cz.cesnet.shongo.controller.api.ExistingResourceSpecification();

        api.setResourceIdentifier(domain.formatIdentifier(resource.getId()));

        super.toApi(api);

        return api;
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.ResourceSpecification api, EntityManager entityManager,
            Domain domain) throws FaultException
    {
        cz.cesnet.shongo.controller.api.ExistingResourceSpecification apiDefiniteResource =
                (cz.cesnet.shongo.controller.api.ExistingResourceSpecification) api;
        if (apiDefiniteResource.isPropertyFilled(apiDefiniteResource.RESOURCE_IDENTIFIER)) {
            Long resourceId = domain.parseIdentifier(apiDefiniteResource.getResourceIdentifier());
            ResourceManager resourceManager = new ResourceManager(entityManager);
            setResource(resourceManager.get(resourceId));
        }
        super.fromApi(api, entityManager, domain);
    }*/
}
