package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.report.AbstractReportException;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class SchedulerException extends AbstractReportException
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
