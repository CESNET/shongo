package cz.cesnet.shongo.controller.scheduler.report;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.report.Report;

import javax.persistence.*;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class NoAvailableAliasReport extends Report
{
    /**
     * List of {@link cz.cesnet.shongo.controller.scheduler.report.TechnologySet}s.
     */
    private Technology technology;

    /**
     * Constructor.
     */
    public NoAvailableAliasReport()
    {
    }

    /**
     * Constructor.
     *
     * @param technology
     */
    public NoAvailableAliasReport(Technology technology)
    {
        this.setTechnology(technology);
    }

    /**
     * @return {@link #technology}
     */
    @Column
    @Enumerated(EnumType.STRING)
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
        return String.format("No available alias was found for the following specification:\n"
                + " Technology: %s", technology.getName());
    }
}
