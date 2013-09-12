    /**
     * Set of report messages.
     */
    private static final ReportSetMessages MESSAGES = new ReportSetMessages() {{
<#list scope.getReports() as report>
    <#list report.getMessages() as message>
        addMessage("${report.id}", new Report.UserType[]{<#rt>
            <#list message.getFor() as type>Report.UserType.${type}<#if type_has_next>, </#if></#list>}, <#t>
            <#if message.lang?? && message.lang == "cs">Report.Language.CZECH<#else>Report.Language.ENGLISH</#if>, <#t>
            "${message.value.replaceAll('"', '\\\\"')}");<#lt>
    </#list>
</#list>
    }};
