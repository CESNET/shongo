package cz.cesnet.shongo.controller.scheduler.reportnew;

import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.common.IdentifierFormat;
import cz.cesnet.shongo.controller.report.Report;
import cz.cesnet.shongo.controller.resource.Capability;
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
    @OneToOne
    @Access(AccessType.FIELD)
    private Resource resource;

    /**
     * {@link Resource} {@link cz.cesnet.shongo.controller.resource.Capability} which is checking for availability.
     */
    @OneToOne
    @Access(AccessType.FIELD)
    private Capability capability;

    /**
     * Constructor.
     */
    public AbstractResourceReport()
    {
    }

    /**
     * Constructor.
     *
     * @param resource ses the {@link #resource}
     */
    public AbstractResourceReport(Resource resource)
    {
        this.resource = resource;
    }

    /**
     * Constructor.
     *
     * @param capability sets the {@link #capability}
     */
    public AbstractResourceReport(Capability capability)
    {
        this.capability = capability;
    }

    /**
     * @param resource sets the {@link #resource}
     */
    @Transient
    public void setResource(Resource resource)
    {
        this.resource = resource;
        this.capability = null;
    }

    /**
     * @param capability sets the {@link #capability}
     */
    @Transient
    public void setResource(Capability capability)
    {
        this.resource = null;
        this.capability = capability;
    }

    @Transient
    public String getResourceDescription()
    {
        if (resource != null) {
            return formatResource(resource);
        }
        else if (capability != null) {
            return String.format("capability '%s' in %s ",
                    capability.getClass().getSimpleName(), formatResource(capability.getResource()));
        }
        else {
            throw new IllegalStateException("Resource or capability must be set.");
        }
    }

    protected final boolean hasResource()
    {
        return resource != null || capability != null;
    }

    /**
     * @param resource
     * @return formatted resource
     */
    public static String formatResource(Resource resource)
    {
        return String.format("%s '%s'",
                (resource instanceof DeviceResource ? "device" : "resource"),
                IdentifierFormat.formatGlobalId(resource));
    }
}
