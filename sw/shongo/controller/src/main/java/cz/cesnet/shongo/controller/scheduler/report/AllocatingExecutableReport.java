package cz.cesnet.shongo.controller.scheduler.report;

import cz.cesnet.shongo.controller.report.Report;

import javax.persistence.Entity;
import javax.persistence.Transient;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link #getText()}
 */
@Entity
public class AllocatingExecutableReport extends Report
{
    /**
     * Constructor.
     */
    public AllocatingExecutableReport()
    {
    }

    @Override
    @Transient
    public String getText()
    {
        return "Allocating new executable";
    }
}
