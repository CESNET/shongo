package cz.cesnet.shongo.controller.scheduler.report;

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
public class ResourceReport extends Report
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
    public ResourceReport()
    {
    }

    /**
     * Constructor.
     *
     * @param resource ses the {@link #resource}
     */
    public ResourceReport(Resource resource)
    {
        setResource(resource);
    }

    /**
     * Constructor.
     *
     * @param resource ses the {@link #resource}
     */
    public ResourceReport(Resource resource, State state)
    {
        setResource(resource);
        setState(state);
    }

    /**
     * Constructor.
     *
     * @param capability sets the {@link #capability}
     */
    public ResourceReport(Capability capability)
    {
        setResource(capability);
    }

    /**
     * Constructor.
     *
     * @param capability ses the {@link #resource}
     */
    public ResourceReport(Capability capability, State state)
    {
        setResource(capability);
        setState(state);
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

    /**
     * @return description of the resource
     */
    @Transient
    public String getResourceDescription(boolean uppercase)
    {
        String resourceDescription;
        if (resource != null) {
            resourceDescription = formatResource(resource);
        }
        else if (capability != null) {
            resourceDescription = String.format("capability '%s' in %s",
                    capability.getClass().getSimpleName(), formatResource(capability.getResource()));
        }
        else {
            throw new IllegalStateException("Resource or capability must be set.");
        }
        if (uppercase) {
            return resourceDescription.substring(0, 1).toUpperCase() + resourceDescription.substring(1);
        }
        else {
            return resourceDescription;
        }
    }

    /**
     * @return true if the report contains any resource (or capability),
     *         false otherwise
     */
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

    @Override
    @Transient
    public String getText()
    {
        String text = getResourceDescription(true);
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }
}
