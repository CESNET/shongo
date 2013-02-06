package cz.cesnet.shongo.controller.scheduler.report;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.report.Report;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link #getText()}
 */
@Entity
public class ResourceNotFoundReport extends Report
{
    /**
     * Set of technologies.
     */
    private Set<Technology> technologies = new HashSet<Technology>();

    /**
     * Constructor.
     */
    public ResourceNotFoundReport()
    {
    }

    /**
     * Constructor.
     *
     * @param technologies sets the {@link #technologies}
     */
    public ResourceNotFoundReport(Set<Technology> technologies)
    {
        this.technologies = technologies;
    }

    /**
     * @return {@link #technologies}
     */
    @ElementCollection
    @Enumerated(EnumType.STRING)
    @Access(AccessType.FIELD)
    public Set<Technology> getTechnologies()
    {
        return technologies;
    }

    @Override
    @Transient
    public State getState()
    {
        return State.ERROR;
    }

    @Override
    @Transient
    public String getText()
    {
        return String.format("No available resource was found for the following specification:\n"
                + " Technologies: %s", Technology.formatTechnologies(technologies));
    }
}
