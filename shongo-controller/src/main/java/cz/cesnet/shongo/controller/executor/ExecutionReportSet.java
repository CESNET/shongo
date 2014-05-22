package cz.cesnet.shongo.controller.executor;

import cz.cesnet.shongo.report.*;

/**
 * Auto-generated implementation of {@link AbstractReportSet}.
 *
 * @author cz.cesnet.shongo.tool-report-generator
 */
public class ExecutionReportSet extends AbstractReportSet
{
    /**
     * Command {@link #command} failed: {@link #jadeReport}
     */
    @javax.persistence.Entity
    @javax.persistence.DiscriminatorValue("CommandFailedReport")
    public static class CommandFailedReport extends cz.cesnet.shongo.controller.executor.ExecutionReport
    {
        protected String command;

        protected cz.cesnet.shongo.JadeReport jadeReport;

        public CommandFailedReport()
        {
        }

        @javax.persistence.Transient
        @Override
        public String getUniqueId()
        {
            return "command-failed";
        }

        public CommandFailedReport(String command, cz.cesnet.shongo.JadeReport jadeReport)
        {
            setCommand(command);
            setJadeReport(jadeReport);
        }

        @javax.persistence.Column(length = cz.cesnet.shongo.api.AbstractComplexType.DEFAULT_COLUMN_LENGTH)
        public String getCommand()
        {
            return command;
        }

        public void setCommand(String command)
        {
            this.command = command;
        }

        @javax.persistence.OneToOne(cascade = javax.persistence.CascadeType.ALL, orphanRemoval = true, fetch = javax.persistence.FetchType.LAZY)
        @javax.persistence.Access(javax.persistence.AccessType.FIELD)
        @javax.persistence.JoinColumn(name = "jade_report_id")
        public cz.cesnet.shongo.JadeReport getJadeReport()
        {
            return cz.cesnet.shongo.PersistentObject.getLazyImplementation(jadeReport);
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
        public int getVisibleFlags()
        {
            return VISIBLE_TO_DOMAIN_ADMIN | VISIBLE_TO_RESOURCE_ADMIN;
        }

        @javax.persistence.Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("command", command);
            parameters.put("jadeReport", jadeReport);
            return parameters;
        }

        @javax.persistence.Transient
        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return cz.cesnet.shongo.controller.ExecutionReportMessages.getMessage("command-failed", userType, language, timeZone, getParameters());
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
     * {@link #reason}
     */
    @javax.persistence.Entity
    @javax.persistence.DiscriminatorValue("RecordingUnavailableReport")
    public static class RecordingUnavailableReport extends cz.cesnet.shongo.controller.executor.ExecutionReport
    {
        protected String reason;

        public RecordingUnavailableReport()
        {
        }

        @javax.persistence.Transient
        @Override
        public String getUniqueId()
        {
            return "recording-unavailable";
        }

        public RecordingUnavailableReport(String reason)
        {
            setReason(reason);
        }

        @javax.persistence.Column(length = cz.cesnet.shongo.api.AbstractComplexType.DEFAULT_COLUMN_LENGTH)
        public String getReason()
        {
            return reason;
        }

        public void setReason(String reason)
        {
            this.reason = reason;
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
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("reason", reason);
            return parameters;
        }

        @javax.persistence.Transient
        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return cz.cesnet.shongo.controller.ExecutionReportMessages.getMessage("recording-unavailable", userType, language, timeZone, getParameters());
        }
    }

    /**
     * Exception for {@link RecordingUnavailableReport}.
     */
    public static class RecordingUnavailableException extends ReportException
    {
        public RecordingUnavailableException(RecordingUnavailableReport report)
        {
            this.report = report;
        }

        public RecordingUnavailableException(Throwable throwable, RecordingUnavailableReport report)
        {
            super(throwable);
            this.report = report;
        }

        public RecordingUnavailableException(String reason)
        {
            RecordingUnavailableReport report = new RecordingUnavailableReport();
            report.setReason(reason);
            this.report = report;
        }

        public RecordingUnavailableException(Throwable throwable, String reason)
        {
            super(throwable);
            RecordingUnavailableReport report = new RecordingUnavailableReport();
            report.setReason(reason);
            this.report = report;
        }

        public String getReason()
        {
            return getReport().getReason();
        }

        @Override
        public RecordingUnavailableReport getReport()
        {
            return (RecordingUnavailableReport) report;
        }
    }

    /**
     * Cannot modify room {@link #roomName}, because it has not been started yet.
     */
    @javax.persistence.Entity
    @javax.persistence.DiscriminatorValue("RoomNotStartedReport")
    public static class RoomNotStartedReport extends cz.cesnet.shongo.controller.executor.ExecutionReport
    {
        protected String roomName;

        public RoomNotStartedReport()
        {
        }

        @javax.persistence.Transient
        @Override
        public String getUniqueId()
        {
            return "room-not-started";
        }

        public RoomNotStartedReport(String roomName)
        {
            setRoomName(roomName);
        }

        @javax.persistence.Column(length = cz.cesnet.shongo.api.AbstractComplexType.DEFAULT_COLUMN_LENGTH)
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
        public int getVisibleFlags()
        {
            return VISIBLE_TO_DOMAIN_ADMIN | VISIBLE_TO_RESOURCE_ADMIN;
        }

        @javax.persistence.Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("roomName", roomName);
            return parameters;
        }

        @javax.persistence.Transient
        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return cz.cesnet.shongo.controller.ExecutionReportMessages.getMessage("room-not-started", userType, language, timeZone, getParameters());
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
        addReportClass(RecordingUnavailableReport.class);
        addReportClass(RoomNotStartedReport.class);
    }
}
