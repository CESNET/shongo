package cz.cesnet.shongo.controller.scheduler.reportnew;

import cz.cesnet.shongo.controller.report.Report;

import javax.persistence.Entity;
import javax.persistence.Transient;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link #getText()}
 */
@Entity
public class FindingAvailableResourceReport extends Report
{
    /**
     * Constructor.
     */
    public FindingAvailableResourceReport()
    {
    }

    @Override
    @Transient
    public String getText()
    {
        return "Finding available resource";
    }
}
