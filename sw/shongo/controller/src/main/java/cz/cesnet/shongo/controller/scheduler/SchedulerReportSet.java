package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.report.*;

/**
 * Auto-generated implementation of {@link AbstractReportSet}.
 *
 * @author cz.cesnet.shongo.tool-report-generator
 */
public class SchedulerReportSet extends AbstractReportSet
{
    /**
     * TODO:
     */
    @javax.persistence.Entity
    public static class UnknownErrorReport extends cz.cesnet.shongo.controller.scheduler.SchedulerReport
    {
        public UnknownErrorReport()
        {
        }

        @javax.persistence.Transient
        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @javax.persistence.Transient
        @Override
        public String getMessage()
        {
            String message = "TODO:";
            return message;
        }
    }

    /**
     * Exception for {@link UnknownErrorReport}.
     */
    public static class UnknownErrorException extends cz.cesnet.shongo.controller.scheduler.SchedulerException
    {
        protected UnknownErrorReport report;

        public UnknownErrorException(UnknownErrorReport report)
        {
            this.report = report;
        }

        public UnknownErrorException(Throwable throwable, UnknownErrorReport report)
        {
            super(throwable);
            this.report = report;
        }

        public UnknownErrorException()
        {
            UnknownErrorReport report = new UnknownErrorReport();
            this.report = report;
        }

        public UnknownErrorException(Throwable throwable)
        {
            super(throwable);
            UnknownErrorReport report = new UnknownErrorReport();
            this.report = report;
        }

        @Override
        public UnknownErrorReport getReport()
        {
            return report;
        }
    }

    @Override
    protected void fillReportClasses()
    {
        addReportClass(UnknownErrorReport.class);
    }
}
