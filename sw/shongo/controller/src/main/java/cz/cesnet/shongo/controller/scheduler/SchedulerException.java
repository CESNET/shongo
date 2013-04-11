package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.report.ReportException;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class SchedulerException extends ReportException
{
    /**
     * Constructor.
     */
    public SchedulerException()
    {
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

    /**
     * @return {@link cz.cesnet.shongo.report.Report}
     */
    public abstract SchedulerReport getReport();
}
