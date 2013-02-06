package cz.cesnet.shongo.controller.scheduler.report;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.executor.Endpoint;

import javax.persistence.*;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link #getText()}
 */
@Entity
public class CreatingConnectionBetweenReport extends AbstractConnectionReport
{
    /**
     * Set of technologies.
     */
    private Technology technology;

    /**
     * Constructor.
     */
    public CreatingConnectionBetweenReport()
    {
    }

    /**
     * Constructor.
     *
     * @param endpointFrom
     * @param endpointTo
     */
    public CreatingConnectionBetweenReport(Endpoint endpointFrom, Endpoint endpointTo,
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
    public State getState()
    {
        return State.INFO;
    }

    @Override
    @Transient
    public String getText()
    {
        return String.format("Creating connection between %s and %s in technology %s...",
                getEndpointFrom().getReportDescription(), getEndpointTo().getReportDescription(), technology.getName());
    }
}
