package cz.cesnet.shongo.controller.executor;

import cz.cesnet.shongo.report.*;

/**
 * Auto-generated implementation of {@link AbstractReportSet}.
 *
 * @author cz.cesnet.shongo.tool-report-generator
 */
public class ExecutorReportSet extends AbstractReportSet
{
    /**
     * Command {@link #command} failed: {@link #jadeReport}
     */
    @javax.persistence.Entity
    public static class CommandFailedReport extends cz.cesnet.shongo.controller.executor.ExecutorReport
    {
        protected String command;

        protected cz.cesnet.shongo.JadeReport jadeReport;

        public CommandFailedReport()
        {
        }

        public CommandFailedReport(String command, cz.cesnet.shongo.JadeReport jadeReport)
        {
            setCommand(command);
            setJadeReport(jadeReport);
        }

        @javax.persistence.Column
        public String getCommand()
        {
            return command;
        }

        public void setCommand(String command)
        {
            this.command = command;
        }

        @javax.persistence.OneToOne(orphanRemoval = true)
        public cz.cesnet.shongo.JadeReport getJadeReport()
        {
            return jadeReport;
        }

        public void setJadeReport(cz.cesnet.shongo.JadeReport jadeReport)
        {
            this.jadeReport = jadeReport;
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
            String message = "Command ${command} failed: ${jade-report}";
            message = message.replace("${command}", (command == null ? "" : command));
            message = message.replace("${jade-report}", (jadeReport == null ? "" : jadeReport.toString()));
            return message;
        }
    }

    /**
     * Exception for {@link CommandFailedReport}.
     */
    public static class CommandFailedException extends ReportException
    {
        protected CommandFailedReport report;

        public CommandFailedException(CommandFailedReport report)
        {
            this.report = report;
        }

        public CommandFailedException(Throwable throwable, CommandFailedReport report)
        {
            super(throwable);
            this.report = report;
        }

        public CommandFailedException(String command, cz.cesnet.shongo.JadeReport jadeReport)
        {
            CommandFailedReport report = new CommandFailedReport();
            report.setCommand(command);
            report.setJadeReport(jadeReport);
            this.report = report;
        }

        public CommandFailedException(Throwable throwable, String command, cz.cesnet.shongo.JadeReport jadeReport)
        {
            super(throwable);
            CommandFailedReport report = new CommandFailedReport();
            report.setCommand(command);
            report.setJadeReport(jadeReport);
            this.report = report;
        }

        public String getCommand()
        {
            return getReport().getCommand();
        }

        public cz.cesnet.shongo.JadeReport getJadeReport()
        {
            return getReport().getJadeReport();
        }

        @Override
        public CommandFailedReport getReport()
        {
            return report;
        }
    }

    /**
     * Cannot modify room, because it has not been created.
     */
    @javax.persistence.Entity
    public static class UsedRoomNotStartedReport extends cz.cesnet.shongo.controller.executor.ExecutorReport
    {
        public UsedRoomNotStartedReport()
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
            String message = "Cannot modify room, because it has not been created.";
            return message;
        }
    }

    /**
     * Exception for {@link UsedRoomNotStartedReport}.
     */
    public static class UsedRoomNotStartedException extends ReportException
    {
        protected UsedRoomNotStartedReport report;

        public UsedRoomNotStartedException(UsedRoomNotStartedReport report)
        {
            this.report = report;
        }

        public UsedRoomNotStartedException(Throwable throwable, UsedRoomNotStartedReport report)
        {
            super(throwable);
            this.report = report;
        }

        public UsedRoomNotStartedException()
        {
            UsedRoomNotStartedReport report = new UsedRoomNotStartedReport();
            this.report = report;
        }

        public UsedRoomNotStartedException(Throwable throwable)
        {
            super(throwable);
            UsedRoomNotStartedReport report = new UsedRoomNotStartedReport();
            this.report = report;
        }

        @Override
        public UsedRoomNotStartedReport getReport()
        {
            return report;
        }
    }
}
