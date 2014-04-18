<#list scope.reports as report>
    <#if !report.abstract>
    public static final String ${report.constantName} = "${report.id}";
    </#if>
</#list>

    /**
     * Set of report messages.
     */
    private static final ReportSetMessages MESSAGES = new ReportSetMessages() {{
<#list scope.getReports() as report>
    <#if !report.abstract>
        <#list report.getMessages() as message>
        addMessage(${report.constantName}, new Report.UserType[]{<#rt>
            <#list message.getFor() as type>Report.UserType.${type}<#if type_has_next>, </#if></#list>}, <#t>
            <#if message.lang?? && message.lang == "cs">Report.Language.CZECH<#else>Report.Language.ENGLISH</#if>, <#t>
            "${message.value.replaceAll('"', '\\\\"')}");<#lt>
        </#list>
    </#if>
</#list>
    }};

    public static String getMessage(String reportId, Report.UserType userType, Report.Language language, org.joda.time.DateTimeZone timeZone, java.util.Map<String, Object> parameters)
    {
        return MESSAGES.getMessage(reportId, userType, language, timeZone, parameters);
    }
