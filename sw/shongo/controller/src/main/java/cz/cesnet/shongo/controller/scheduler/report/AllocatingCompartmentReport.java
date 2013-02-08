package cz.cesnet.shongo.controller.scheduler.report;

import cz.cesnet.shongo.controller.report.Report;

import javax.persistence.Entity;
import javax.persistence.Transient;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link #getText()}
 */
@Entity
public class AllocatingCompartmentReport extends Report
{
    /**
     * Constructor.
     */
    public AllocatingCompartmentReport()
    {
    }

    @Override
    @Transient
    public String getText()
    {
        return String.format("Allocating compartment");
    }
}
