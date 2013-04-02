package cz.cesnet.shongo.controller.scheduler.report;

import cz.cesnet.shongo.controller.report.Report;

import javax.persistence.Entity;
import javax.persistence.Transient;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link #getText()}
 */
@Entity
public class CheckingSpecificationAvailabilityReport extends Report
{
    /**
     * Constructor.
     */
    public CheckingSpecificationAvailabilityReport()
    {
    }

    @Override
    @Transient
    public String getText()
    {
        return String.format("Checking specification availability report.");
    }
}
