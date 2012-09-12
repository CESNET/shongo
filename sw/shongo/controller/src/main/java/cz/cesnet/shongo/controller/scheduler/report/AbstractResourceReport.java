package cz.cesnet.shongo.controller.scheduler.report;

import cz.cesnet.shongo.controller.report.Report;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.resource.Resource;

import javax.persistence.*;

/**
 * Represents a {@link Report} for {@link Resource} processed by scheduler.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public abstract class AbstractResourceReport extends Report
{
    /**
     * Identification of resource.
     */
    private Resource resource;

    /**
     * Constructor.
     */
    public AbstractResourceReport()
    {
    }

    /**
     * Constructor.
     *
     * @param resource
     */
    public AbstractResourceReport(Resource resource)
    {
        this.resource = resource;
    }

    /**
     * @return {@link #resource}
     */
    @OneToOne
    @Access(AccessType.FIELD)
    public Resource getResource()
    {
        return resource;
    }

    @Transient
    public String getResourceAsString()
    {
        return formatResource(resource);
    }

    /**
     * @param resource
     * @return formatted resource
     */
    public static String formatResource(Resource resource)
    {
        if (resource == null) {
            return "null";
        }
        return String.format("%s(id: %d)",
                (resource instanceof DeviceResource ? "device" : "resource"),
                resource.getId());
    }
}
