package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.report.*;

/**
* Auto-generated messages for ExecutorReportSet.
*
* @author cz.cesnet.shongo.tool-report-generator
*/
public class ExecutableStateReportMessages
{
    /**
     * Set of report messages.
     */
    private static final ReportSetMessages MESSAGES = new ReportSetMessages() {{
        addMessage("command-failed", new Report.UserType[]{}, Report.Language.ENGLISH, "Command ${command} failed: ${jadeReportMessage(jadeReport)}");
        addMessage("room-not-started", new Report.UserType[]{}, Report.Language.ENGLISH, "Cannot modify room ${roomName}, because it has not been started yet.");
    }};

    public static String getMessage(String reportId, Report.UserType userType, Report.Language language, java.util.Map<String, Object> parameters)
    {
        return MESSAGES.getMessage(reportId, userType, language, parameters);
    }
}