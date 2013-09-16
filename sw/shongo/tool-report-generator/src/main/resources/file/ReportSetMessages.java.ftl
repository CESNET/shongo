package ${scope.messagesClassPackage};

import cz.cesnet.shongo.report.*;

/**
* Auto-generated messages for ${scope.className}.
*
* @author cz.cesnet.shongo.tool-report-generator
*/
public class ${scope.messagesClassName}
{
<#list scope.reports as report>
<#if !report.abstract>
    public static final String ${report.constantName} = "${report.id}";
</#if>
</#list>

<#include "ReportSetMessages.ftl">
}