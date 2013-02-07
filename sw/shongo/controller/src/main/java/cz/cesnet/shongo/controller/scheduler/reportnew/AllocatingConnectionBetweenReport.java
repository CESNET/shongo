package cz.cesnet.shongo.controller.scheduler.reportnew;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.executor.Endpoint;
import cz.cesnet.shongo.controller.scheduler.reportnew.AbstractConnectionReport;

import javax.persistence.*;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link #getText()}
 */
@Entity
public class AllocatingConnectionBetweenReport extends AbstractConnectionReport
{
    /**
     * Set of technologies.
     */
    private Technology technology;

    /**
     * Constructor.
     */
    public AllocatingConnectionBetweenReport()
    {
    }

    /**
     * Constructor.
     *
     * @param endpointFrom
     * @param endpointTo
     */
    public AllocatingConnectionBetweenReport(Endpoint endpointFrom, Endpoint endpointTo,
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
                getEndpointFrom().getReportDescription(), getEndpointTo().getReportDescription(), technology.getName());
    }
}
