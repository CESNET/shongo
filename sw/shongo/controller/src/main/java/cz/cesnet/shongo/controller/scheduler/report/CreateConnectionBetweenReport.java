package cz.cesnet.shongo.controller.scheduler.report;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.allocation.AllocatedEndpoint;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link #toString()}
 */
@Entity
public class CreateConnectionBetweenReport extends AbstractConnectionReport
{
    /**
     * Set of technologies.
     */
    private Technology technology;

    /**
     * Constructor.
     */
    public CreateConnectionBetweenReport()
    {
    }

    /**
     * Constructor.
     *
     * @param endpointFrom
     * @param endpointTo
     */
    public CreateConnectionBetweenReport(AllocatedEndpoint endpointFrom, AllocatedEndpoint endpointTo,
            Technology technology)
    {
        super(endpointFrom, endpointTo);
        this.technology = technology;
    }

    /**
     * @return {@link #technology}
     */
    @Column
    @Enumerated(EnumType.STRING)
    @Access(AccessType.FIELD)
    public Technology getTechnology()
    {
        return technology;
    }

    /**
     * @param technology sets the {@link #technology}
     */
    public void setTechnology(Technology technology)
    {
        this.technology = technology;
    }

    @Override
    @Transient
    public String getText()
    {
        return String.format("Creating connection between %s and %s in technology %s...",
                getEndpointFrom(), getEndpointTo(), technology.getName());
    }
}
