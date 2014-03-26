package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.report.*;

/**
* Auto-generated messages for ExecutionReportSet.
*
* @author cz.cesnet.shongo.tool-report-generator
*/
public class ExecutionReportMessages
{
    public static final String COMMAND_FAILED = "command-failed";
    public static final String RECORDING_UNAVAILABLE = "recording-unavailable";
    public static final String ROOM_NOT_STARTED = "room-not-started";

    /**
     * Set of report messages.
     */
    private static final ReportSetMessages MESSAGES = new ReportSetMessages() {{
        addMessage(COMMAND_FAILED, new Report.UserType[]{}, Report.Language.ENGLISH, "Command ${command} failed: ${jadeReportMessage(jadeReport)}");
        addMessage(RECORDING_UNAVAILABLE, new Report.UserType[]{}, Report.Language.ENGLISH, "${reason}");
        addMessage(ROOM_NOT_STARTED, new Report.UserType[]{}, Report.Language.ENGLISH, "Cannot modify room ${roomName}, because it has not been started yet.");
    }};

    public static String getMessage(String reportId, Report.UserType userType, Report.Language language, org.joda.time.DateTimeZone timeZone, java.util.Map<String, Object> parameters)
    {
        return MESSAGES.getMessage(reportId, userType, language, timeZone, parameters);
    }
}