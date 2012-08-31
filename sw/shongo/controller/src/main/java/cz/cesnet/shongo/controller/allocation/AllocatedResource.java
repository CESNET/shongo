package cz.cesnet.shongo.controller.allocation;

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
}
