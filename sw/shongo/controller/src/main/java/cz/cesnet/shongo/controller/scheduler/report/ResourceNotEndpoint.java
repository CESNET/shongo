package cz.cesnet.shongo.controller.scheduler.report;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.report.Report;
import cz.cesnet.shongo.controller.resource.Resource;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link #getText()}
 */
@Entity
public class ResourceNotEndpoint extends AbstractResourceReport
{
    /**
     * Constructor.
     */
    public ResourceNotEndpoint()
    {
    }

    /**
     * Constructor.
     *
     * @param resource
     */
    public ResourceNotEndpoint(Resource resource)
    {
        super(resource);
    }

    @Override
    @Transient
    public String getText()
    {
        return String.format("Resource %s is not endpoint.", getResourceAsString());
    }
}
