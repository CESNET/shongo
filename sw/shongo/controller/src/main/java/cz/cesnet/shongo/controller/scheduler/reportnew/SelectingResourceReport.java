package cz.cesnet.shongo.controller.scheduler.reportnew;

import cz.cesnet.shongo.controller.resource.Capability;
import cz.cesnet.shongo.controller.resource.Resource;

import javax.persistence.Entity;
import javax.persistence.Transient;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link #getText()}
 */
@Entity
public class SelectingResourceReport extends AbstractResourceReport
{
    /**
     * Constructor.
     */
    public SelectingResourceReport()
    {
    }

    /**
     * Constructor.
     *
     * @param resource sets the {@link #resource}
     */
    public SelectingResourceReport(Resource resource)
    {
        super(resource);
    }

    /**
     * Constructor.
     *
     * @param capability sets the {@link #capability}
     */
    public SelectingResourceReport(Capability capability)
    {
        super(capability);
    }

    @Override
    @Transient
    public String getText()
    {
        if (hasResource()) {
            return String.format("Selected %s.", getResourceDescription());
        }
        else {
            return "Selecting resource";
        }
    }
}
