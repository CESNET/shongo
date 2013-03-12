package cz.cesnet.shongo;

import cz.cesnet.shongo.fault.Fault;
import cz.cesnet.shongo.jade.JadeError;

public class Faults
{
<#assign reportCode = 0>
<#list reports as report>
    public static final int ${this.formatConstant(report.getId() + "-fault")} = ${reportCode};
    <#assign reportCode = reportCode + 1>
</#list>
<#list reports as report>
    <#assign reportName = this.formatCamelCase(report.getId() + "-fault")>
    <#assign reportIdentifier = this.formatIdentifier(report.getId() + "-fault")>

    /**
     * ${this.formatDescriptionAsJavaDoc(report.getDescription())}
     */
    public static class ${reportName} implements Fault
    {
        <#list report.params.getParam() as param>
        private ${this.formatParamType(param.getType())} ${this.formatIdentifier(param.getName())};
        </#list>
        <#list report.params.getParam() as param>
            <#assign paramType = this.formatParamType(param.getType())>
            <#assign paramName = this.formatCamelCase(param.getName())>
            <#assign paramIdentifier = this.formatIdentifier(param.getName())>

        public ${paramType} get${paramName}()
        {
            return ${paramIdentifier};
        }

        public void set${paramName}(${paramType} ${paramIdentifier})
        {
            this.${paramIdentifier} = ${paramIdentifier};
        }
        </#list>

        @Override
        public int getCode()
        {
            return ${this.formatConstant(report.getId() + "-fault")};
        }

        @Override
        public String getMessage()
        {
            String message = "${this.formatString(report.getDescription())}";
            <#list report.params.getParam() as param>
            message = message.replace("{${param.getName()}}", ${this.formatParamToString(param)});
            </#list>
            return message;
        }
    }

    /**
     * @return new instance of {@link ${reportName}}
     */
    public static ${reportName} create${reportName}(<@formatMethodParams params=report.params.getParam()/>)
    {
        ${reportName} ${reportIdentifier} = new ${reportName}();
        <#list report.params.getParam() as param>
        ${reportIdentifier}.set${this.formatCamelCase(param.getName())}(${this.formatIdentifier(param.getName())});
        </#list>
        return ${reportIdentifier};
    }
</#list>
}
<#---->
<#macro formatMethodParams params>
    <#list params as param>
        ${this.formatParamType(param.getType())} ${this.formatIdentifier(param.getName())}<#if param_has_next>, </#if><#t>
    </#list>
</#macro>