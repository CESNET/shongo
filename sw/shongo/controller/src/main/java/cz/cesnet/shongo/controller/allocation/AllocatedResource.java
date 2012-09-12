package cz.cesnet.shongo.controller.allocation;

import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.resource.Resource;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;

/**
 * Represents an allocated {@link Resource} in an {@link AllocatedCompartment}
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class AllocatedResource extends AllocatedItem
{
    /**
     * Resource that is allocated.
     */
    private Resource resource;

    /**
     * Constructor.
     */
    public AllocatedResource()
    {
    }

    /**
     * Constructor.
     *
     * @param resource sets the {@link #resource}
     */
    public AllocatedResource(Resource resource)
    {
        setResource(resource);
    }

    /**
     * @return {@link #resource}
     */
    @ManyToOne
    @Access(AccessType.FIELD)
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

    @Override
    protected cz.cesnet.shongo.controller.api.AllocatedItem createApi()
    {
        return new cz.cesnet.shongo.controller.api.AllocatedResource();
    }

    @Override
    protected void toApi(cz.cesnet.shongo.controller.api.AllocatedItem api, Domain domain)
    {
        cz.cesnet.shongo.controller.api.AllocatedResource apiAllocatedResource =
                (cz.cesnet.shongo.controller.api.AllocatedResource) api;
        apiAllocatedResource.setIdentifier(domain.formatIdentifier(resource.getId()));
        apiAllocatedResource.setName(resource.getName());
        super.toApi(api, domain);
    }
}
