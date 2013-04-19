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
    @javax.persistence.DiscriminatorValue("CommandFailedReport")
    public static class CommandFailedReport extends cz.cesnet.shongo.controller.executor.ExecutableReport
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

        @javax.persistence.OneToOne(cascade = javax.persistence.CascadeType.ALL, orphanRemoval = true) @javax.persistence.JoinColumn(name = "jade_report_id")
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
        public Resolution getResolution()
        {
            return jadeReport.getResolution();
        }

        @javax.persistence.Transient
        @Override
        public boolean isVisibleToDomainAdminViaEmail()
        {
            return true;
        }

        @javax.persistence.Transient
        @Override
        public boolean isVisibleToResourceAdminViaEmail()
        {
            return true;
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
            return (CommandFailedReport) report;
        }
    }

    /**
     * Cannot modify room {@link #roomName}, because it has not been started yet.
     */
    @javax.persistence.Entity
    @javax.persistence.DiscriminatorValue("RoomNotStartedReport")
    public static class RoomNotStartedReport extends cz.cesnet.shongo.controller.executor.ExecutableReport
    {
        protected String roomName;

        public RoomNotStartedReport()
        {
        }

        public RoomNotStartedReport(String roomName)
        {
            setRoomName(roomName);
        }

        @javax.persistence.Column
        public String getRoomName()
        {
            return roomName;
        }

        public void setRoomName(String roomName)
        {
            this.roomName = roomName;
        }

        @javax.persistence.Transient
        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @javax.persistence.Transient
        @Override
        public Resolution getResolution()
        {
            return Resolution.TRY_AGAIN;
        }

        @javax.persistence.Transient
        @Override
        public boolean isVisibleToDomainAdminViaEmail()
        {
            return true;
        }

        @javax.persistence.Transient
        @Override
        public boolean isVisibleToResourceAdminViaEmail()
        {
            return true;
        }

        @javax.persistence.Transient
        @Override
        public String getMessage()
        {
            String message = "Cannot modify room ${room-name}, because it has not been started yet.";
            message = message.replace("${room-name}", (roomName == null ? "" : roomName));
            return message;
        }
    }

    /**
     * Exception for {@link RoomNotStartedReport}.
     */
    public static class RoomNotStartedException extends ReportException
    {
        public RoomNotStartedException(RoomNotStartedReport report)
        {
            this.report = report;
        }

        public RoomNotStartedException(Throwable throwable, RoomNotStartedReport report)
        {
            super(throwable);
            this.report = report;
        }

        public RoomNotStartedException(String roomName)
        {
            RoomNotStartedReport report = new RoomNotStartedReport();
            report.setRoomName(roomName);
            this.report = report;
        }

        public RoomNotStartedException(Throwable throwable, String roomName)
        {
            super(throwable);
            RoomNotStartedReport report = new RoomNotStartedReport();
            report.setRoomName(roomName);
            this.report = report;
        }

        public String getRoomName()
        {
            return getReport().getRoomName();
        }

        @Override
        public RoomNotStartedReport getReport()
        {
            return (RoomNotStartedReport) report;
        }
    }

    @Override
    protected void fillReportClasses()
    {
        addReportClass(CommandFailedReport.class);
        addReportClass(RoomNotStartedReport.class);
    }
}
