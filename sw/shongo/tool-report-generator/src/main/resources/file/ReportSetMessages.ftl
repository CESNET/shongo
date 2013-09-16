    /**
     * Set of report messages.
     */
    private static final ReportSetMessages MESSAGES = new ReportSetMessages() {{
<#list scope.getReports() as report>
    <#list report.getMessages() as message>
        addMessage(<#if scope.messagesClassName??>${report.constantName}<#else>"${report.id}"</#if>, new Report.UserType[]{<#rt>
            <#list message.getFor() as type>Report.UserType.${type}<#if type_has_next>, </#if></#list>}, <#t>
            <#if message.lang?? && message.lang == "cs">Report.Language.CZECH<#else>Report.Language.ENGLISH</#if>, <#t>
            "${message.value.replaceAll('"', '\\\\"')}");<#lt>
    </#list>
</#list>
    }};

    public static String getMessage(String reportId, Report.UserType userType, Report.Language language, java.util.Map<String, Object> parameters)
    {
        return MESSAGES.getMessage(reportId, userType, language, parameters);
    }
