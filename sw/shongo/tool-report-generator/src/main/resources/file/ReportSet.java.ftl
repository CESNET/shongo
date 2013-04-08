package ${scope.getClassPackage()};

import cz.cesnet.shongo.report.*;

/**
 * Auto-generated implementation of {@link AbstractReportSet}.
 *
 * @author cz.cesnet.shongo.tool-report-generator
 */
public class ${scope.getClassName()} extends AbstractReportSet
{
<#list scope.getReports() as report>
    <#if report.isApiFault()>
    public static final int ${report.getConstantName()} = ${report.getCode()};
    </#if>
</#list>
<#list scope.getReports() as report>
    /**
     * ${report.getJavaDoc()}
     */
    public static class ${report.getClassName()} implements Report<#if report.isApiFault()>, ApiFault</#if>
    {
    <#list report.getParams() as param>
        private ${param.getVariableType()} ${param.getVariableName()};
    </#list>
    <#list report.getParams() as param>

        public ${param.getVariableType()} get${param.getMethodName()}()
        {
            return ${param.getVariableName()};
        }

        public void set${param.getMethodName()}(${param.getVariableType()} ${param.getVariableName()})
        {
            this.${param.getVariableName()} = ${param.getVariableName()};
        }
    </#list>

        @Override
        public Type getType()
        {
            return ${report.getType()};
        }
    <#if report.isApiFault()>

        @Override
        public int getCode()
        {
            return ${report.getConstantName()};
        }
    </#if>

        @Override
        public String getMessage()
        {
            String message = "${report.getDescription()}";
            <#list report.getParams() as param>
            message = message.replace("<#noparse>$</#noparse>{${param.getName()}}", (${param.getVariableName()} == null ? "" : ${param.getVariableStringValue()}));
            </#list>
            return message;
        }
    }

    <#if report.hasException()>
    /**
     * Exception for {@link ${report.getClassName()}}.
     */
    public static class ${report.getExceptionClassName()} extends AbstractReportException<#if report.isApiFault()> implements ApiFault</#if>
    {
        private ${report.getClassName()} report;

        public ${report.getExceptionClassName()}(${report.getClassName()} report)
        {
            this.report = report;
        }

        public ${report.getExceptionClassName()}(Throwable throwable, ${report.getClassName()} report)
        {
            super(throwable);
            this.report = report;
        }

        public ${report.getExceptionClassName()}(<@formatMethodParameters parameters=report.getParams()/>)
        {
            report = new ${report.getClassName()}();
            <#list report.getParams() as param>
            report.set${param.getMethodName()}(${param.getVariableName()});
            </#list>
        }

        public ${report.getExceptionClassName()}(Throwable throwable, <@formatMethodParameters parameters=report.getParams()/>)
        {
            super(throwable);
            report = new ${report.getClassName()}();
            <#list report.getParams() as param>
            report.set${param.getMethodName()}(${param.getVariableName()});
            </#list>
        }
        <#list report.getParams() as param>

        public ${param.getVariableType()} get${param.getMethodName()}()
        {
            return report.get${param.getMethodName()}();
        }
        </#list>

        @Override
        public ${report.getClassName()} getReport()
        {
            return report;
        }
        <#if report.isApiFault()>

        @Override
        public int getCode()
        {
            return report.getCode();
        }
        </#if>
    }
    </#if>
</#list>
}
<#---->
<#macro formatMethodParameters parameters>
    <#list parameters as param>
    ${param.getVariableType()} ${param.getVariableName()}<#if param_has_next>, </#if><#t>
    </#list>
</#macro>