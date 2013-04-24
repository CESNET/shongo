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
    @javax.persistence.DiscriminatorValue("${report.getClassName()}")
    </#if>
    <#assign interfaces=[]/>
    <#if report.isApiFault()><#assign interfaces=interfaces + ["ApiFault"]/></#if>
    <#if report.isSerializable()><#assign interfaces=interfaces + ["SerializableReport"]/></#if>
    <#if report.getResourceIdParam()??><#assign interfaces=interfaces + ["ResourceReport"]/></#if>
    public static<#if report.isAbstract()> abstract</#if> class ${report.getClassName()} extends ${report.getBaseClassName()}<#rt>
    <#list interfaces as interface><#if interface_index == 0> implements <#else>, </#if>${interface}</#list><#lt>
    {
    <#list report.getDeclaredParams() as param>
        protected ${param.getTypeClassName()} ${param.getVariableName()};

    </#list>
        public ${report.getClassName()}()
        {
        }
    <#if (report.getAllDeclaredParams()?size > 0)>

        public ${report.getClassName()}(<@formatMethodParameters parameters=report.getAllDeclaredParams()/>)
        {
            <#list report.getAllDeclaredParams() as param>
            ${param.getSetterName()}(${param.getVariableName()});
            </#list>
        }
    </#if>
    <#list report.getDeclaredParams() as param>

        <#if report.isPersistent()>
            <#list param.getPersistenceAnnotations() as persistenceAnnotation>
        ${persistenceAnnotation}
            </#list>
        </#if>
        public ${param.getTypeClassName()} ${param.getGetterName()}()
        {
            return ${param.getVariableName()};
        }

        public void ${param.getSetterName()}(${param.getTypeClassName()} ${param.getVariableName()})
        {
            this.${param.getVariableName()} = ${param.getVariableName()};
        }
    </#list>
    <#list report.getTemporaryParams() as param>

        <#if report.isPersistent()>
        @javax.persistence.Transient
        </#if>
        public ${param.getTypeClassName()} ${param.getGetterName()}()
        {
            return ${param.getCode()};
        }
    </#list>
    <#if report.getResourceIdParam()??>

        @Override
        public String getResourceId()
        {
            return ${report.getResourceIdParam().getVariableName()};
        }
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
            return getMessage(MessageType.USER);
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
            <#list report.getAllDeclaredParams() as param>
            ${param.getVariableName()} = (${param.getTypeClassName()}) reportSerializer.getParameter("${param.getVariableName()}", ${param.getTypeClassName()}.class);
            </#list>
        }

        @Override
        public void writeParameters(ReportSerializer reportSerializer)
        {
            <#list report.getAllDeclaredParams() as param>
            reportSerializer.setParameter("${param.getVariableName()}", ${param.getVariableName()});
            </#list>
        }
        </#if>
        <#if report.getVisibleFlags()??>

            <#if report.isPersistent()>
        @javax.persistence.Transient
            </#if>
        @Override
        protected int getVisibleFlags()
        {
            return ${report.getVisibleFlags()};
        }
        </#if>

        <#if report.isPersistent()>
        @javax.persistence.Transient
        </#if>
        @Override
        public String getMessage(MessageType messageType)
        {
            StringBuilder message = new StringBuilder();
            switch (messageType) {
        <#if report.hasUserMessage()>
                case USER:
            <#list report.getUserMessage() as messageLine>
                    message.append(${messageLine});
            </#list>
                    break;
        </#if>
        <#if report.hasDomainAdminMessage()>
                case DOMAIN_ADMIN:
            <#list report.getDomainAdminMessage() as messageLine>
                    message.append(${messageLine});
            </#list>
                    break;
        </#if>
        <#if report.hasResourceAdminMessage()>
                case RESOURCE_ADMIN:
            <#list report.getResourceAdminMessage() as messageLine>
                    message.append(${messageLine});
            </#list>
                    break;
        </#if>
                default:
        <#list report.getMessage() as messageLine>
                    message.append(${messageLine});
        </#list>
                    break;
            }
            return message.toString();
        }
    </#if>
    <#if report.isPersistent()>
        <#assign persistencePreRemove = report.getPersistencePreRemove()/>
        <#if (persistencePreRemove?size > 0)>

        @javax.persistence.PreRemove
        public void preRemove()
        {
            <#list persistencePreRemove as persistencePreRemoveLine>
            ${persistencePreRemoveLine}
            </#list>
        }
        </#if>
    </#if>
    }
    <#if report.hasException()>

    /**
     * Exception for {@link ${report.getClassName()}}.
     */
    public static<#if report.isAbstract()> abstract</#if> class ${report.getExceptionClassName()} extends ${report.getExceptionBaseClassName()}<#if report.isApiFault()> implements ApiFaultException</#if>
    {
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

        public ${report.getExceptionClassName()}(<@formatMethodParameters parameters=report.getAllDeclaredParams()/>)
        {
            ${report.getClassName()} report = new ${report.getClassName()}();
            <#list report.getAllDeclaredParams() as param>
            report.${param.getSetterName()}(${param.getVariableName()});
            </#list>
            this.report = report;
        }

        public ${report.getExceptionClassName()}(Throwable throwable<#if (report.getAllDeclaredParams()?size > 0) >, </#if><@formatMethodParameters parameters=report.getAllDeclaredParams()/>)
        {
            super(throwable);
            ${report.getClassName()} report = new ${report.getClassName()}();
                <#list report.getAllDeclaredParams() as param>
            report.${param.getSetterName()}(${param.getVariableName()});
                </#list>
            this.report = report;
        }
        </#if>
        <#list report.getDeclaredParams() as param>

        public ${param.getTypeClassName()} ${param.getGetterName()}()
        {
            return getReport().${param.getGetterName()}();
        }
        </#list>

        @Override
        public ${report.getClassName()} getReport()
        {
            return (${report.getClassName()}) report;
        }
        <#if !report.isAbstract() && report.isApiFault()>
        @Override
        public ApiFault getApiFault()
        {
            return (${report.getClassName()}) report;
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
    ${param.getTypeClassName()} ${param.getVariableName()}<#if param_has_next>, </#if><#t>
    </#list>
</#macro>