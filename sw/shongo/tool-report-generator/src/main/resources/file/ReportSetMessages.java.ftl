package ${scope.messagesClassPackage};

import cz.cesnet.shongo.report.*;

/**
* Auto-generated messages for ${scope.className}.
*
* @author cz.cesnet.shongo.tool-report-generator
*/
public class ${scope.messagesClassName}
{
<#include "ReportSetMessages.ftl">

    /**
     * @param reportId
     * @param userType
     * @param language
     * @param parameters
     * @return message for the report with given {@code uniqueId}
     */
    public static String getMessage(String reportId, Report.UserType userType, Report.Language language, java.util.Map<String, Object> parameters)
    {
        return MESSAGES.getMessage(reportId, userType, language, parameters);
    }
}