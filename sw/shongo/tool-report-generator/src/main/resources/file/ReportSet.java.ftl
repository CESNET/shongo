package ${scope.getClassPackage()};

import cz.cesnet.shongo.report.*;

/**
 * Auto-generated implementation of {@link AbstractReportSet}.
 *
 * @author cz.cesnet.shongo.tool-report-generator
 */
public class ${scope.getClassName()} extends AbstractReportSet
{
<#assign apiReportCount = 0>
<#list scope.getReports() as report>
    <#if report.isApiFault()>
    public static final int ${report.getConstantName()} = ${report.getApiFaultCode()};
        <#assign apiReportCount = apiReportCount + 1>
    </#if>
</#list>
<#if (apiReportCount > 0)>

</#if>
<#list scope.getReports() as report>
    <#if report.getJavaDoc()??>
    /**
     * ${report.getJavaDoc()}
     */
    </#if>
    <#if report.isPersistent()>
    @javax.persistence.Entity
    </#if>
    <#assign interfaces=[]/>
    <#if report.isApiFault()><#assign interfaces=interfaces + ["ApiFault"]/></#if>
    <#if report.isSerializable()><#assign interfaces=interfaces + ["SerializableReport"]/></#if>
    public static<#if report.isAbstract()> abstract</#if> class ${report.getClassName()} extends ${report.getBaseClassName()}<#rt>
    <#list interfaces as interface><#if interface_index == 0> implements <#else>, </#if>${interface}</#list><#lt>
    {
    <#list report.getDeclaredParams() as param>
        protected ${param.getVariableType()} ${param.getVariableName()};

    </#list>
        public ${report.getClassName()}()
        {
        }

    <#if (report.getParams()?size > 0)>
        public ${report.getClassName()}(<@formatMethodParameters parameters=report.getParams()/>)
        {
            <#list report.getParams() as param>
            set${param.getMethodName()}(${param.getVariableName()});
            </#list>
        }

    </#if>
    <#list report.getDeclaredParams() as param>
        <#if report.isPersistent()>
        ${param.getPersistenceAnnotation()}
        </#if>
        public ${param.getVariableType()} get${param.getMethodName()}()
        {
            return ${param.getVariableName()};
        }

        public void set${param.getMethodName()}(${param.getVariableType()} ${param.getVariableName()})
        {
            this.${param.getVariableName()} = ${param.getVariableName()};
        }

    </#list>
    <#if (report.getDeclaredParams()?size > 0)>

    </#if>
    <#if !report.isAbstract()>
        <#if report.isPersistent()>
        @javax.persistence.Transient
        </#if>
        @Override
        public Type getType()
        {
            return ${report.getType()};
        }
        <#if report.getResolution()??>

            <#if report.isPersistent()>
        @javax.persistence.Transient
            </#if>
        @Override
        public Resolution getResolution()
        {
            return ${report.getResolution()};
        }
        </#if>
        <#if report.isApiFault()>

            <#if report.isPersistent()>
        @javax.persistence.Transient
            </#if>
        @Override
        public int getFaultCode()
        {
            return ${report.getConstantName()};
        }

        @Override
        public String getFaultString()
        {
            return getMessage();
        }

        @Override
        public Exception getException()
        {
            return new ${report.getExceptionClassName()}(this);
        }
        </#if>
        <#if report.isApiFault() || report.isSerializable()>

        @Override
        public void readParameters(ReportSerializer reportSerializer)
        {
            <#list report.getParams() as param>
            ${param.getVariableName()} = (${param.getVariableType()}) reportSerializer.getParameter("${param.getVariableName()}", ${param.getVariableType()}.class);
            </#list>
        }

        @Override
        public void writeParameters(ReportSerializer reportSerializer)
        {
            <#list report.getParams() as param>
            reportSerializer.setParameter("${param.getVariableName()}", ${param.getVariableName()});
            </#list>
        }
        </#if>
        <#if report.isVisibleToDomainAdminViaEmail()>

        <#if report.isPersistent()>
        @javax.persistence.Transient
        </#if>
        @Override
        public boolean isVisibleToDomainAdminViaEmail()
        {
            return true;
        }
        </#if>

        <#if report.isPersistent()>
        @javax.persistence.Transient
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
    </#if>
    }

    <#if report.hasException()>
    /**
     * Exception for {@link ${report.getClassName()}}.
     */
    public static<#if report.isAbstract()> abstract</#if> class ${report.getExceptionClassName()} extends ${report.getExceptionBaseClassName()}<#if report.isApiFault()> implements ApiFaultException</#if>
    {
        <#if !report.getBaseReport()?? || !report.getBaseReport().hasException()>
        protected ${report.getClassName()} report;

        </#if>
        <#if report.isAbstract()>
        public ${report.getExceptionClassName()}()
        {
        }

        public ${report.getExceptionClassName()}(Throwable throwable)
        {
            super(throwable);
        }
        <#else>
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
            ${report.getClassName()} report = new ${report.getClassName()}();
            <#list report.getParams() as param>
            report.set${param.getMethodName()}(${param.getVariableName()});
            </#list>
            this.report = report;
        }

        public ${report.getExceptionClassName()}(Throwable throwable<#if (report.getParams()?size > 0) >, </#if><@formatMethodParameters parameters=report.getParams()/>)
        {
            super(throwable);
            ${report.getClassName()} report = new ${report.getClassName()}();
                <#list report.getParams() as param>
            report.set${param.getMethodName()}(${param.getVariableName()});
                </#list>
            this.report = report;
        }
        </#if>
        <#list report.getDeclaredParams() as param>

        public ${param.getVariableType()} get${param.getMethodName()}()
        {
            return getReport().get${param.getMethodName()}();
        }
        </#list>

        @Override
        public ${report.getClassName()} getReport()
        {
        <#if report.getBaseReport()?? && report.getBaseReport().hasException()>
            return (${report.getClassName()}) report;
        <#else>
            return report;
        </#if>
        }
        <#if !report.isAbstract() && report.isApiFault()>
        @Override
        public ApiFault getApiFault()
        {
            return report;
        }
        </#if>
    }
    </#if>
    <#if report_has_next>

    </#if>
</#list>

    @Override
    protected void fillReportClasses()
    {
<#list scope.getReports() as report>
        addReportClass(${report.getClassName()}.class);
</#list>
    }
}
<#---->
<#macro formatMethodParameters parameters>
    <#list parameters as param>
    ${param.getVariableType()} ${param.getVariableName()}<#if param_has_next>, </#if><#t>
    </#list>
</#macro>