package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.report.ReportException;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class SchedulerException extends ReportException
{
    /**
     * Constructor.
     */
    public SchedulerException()
    {
    }

    /**
     * Contructor.
     *
     * @param report sets the {@link #report}
     */
    public SchedulerException(SchedulerReport report)
    {
        setReport(report);
    }

    /**
     * Constructor.
     *
     * @param throwable
     */
    public SchedulerException(Throwable throwable)
    {
        super(throwable);
    }

    @Override
    public SchedulerReport getReport()
    {
        return (SchedulerReport) report;
    }

    /**
     * @return {@link #report}
     */
    public SchedulerReport getTopReport()
    {
        SchedulerReport report = getReport();
        while (report.hasParentReport()) {
            report = report.getParentReport();
        }
        return report;
    }
}
