package ${package};

import cz.cesnet.shongo.fault.Fault;
import cz.cesnet.shongo.fault.FaultException;
import cz.cesnet.shongo.fault.jade.CommandFailure;
import ${base_package}.${base_name};

public class ${name}FaultSet extends ${base_name}
{
<#list reports as report>
    <#assign reportCode = reportCodes[report.getId()]>
    public static final int ${this.formatConstant(report.getId() + "-fault")} = ${reportCode};
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

        @Override
        public FaultException createException()
        {
            return new FaultException(this);
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

    /**
     * @return new instance of {@link ${reportName}}
     */
    public static <T> T throw${reportName}(<@formatMethodParams params=report.params.getParam()/>) throws FaultException
    {
        ${reportName} ${reportIdentifier} = create${reportName}(<@formatMethodCallParams params=report.params.getParam()/>);
        throw ${reportIdentifier}.createException();
    }
</#list>

    @Override
    protected void fillFaults()
    {
        super.fillFaults();
        <#list reports as report>
        addFault(${this.formatConstant(report.getId() + "-fault")}, ${this.formatCamelCase(report.getId() + "-fault")}.class);
        </#list>
    }
}
<#---->
<#macro formatMethodParams params>
    <#list params as param>
        ${this.formatParamType(param.getType())} ${this.formatIdentifier(param.getName())}<#if param_has_next>, </#if><#t>
    </#list>
</#macro>
<#macro formatMethodCallParams params>
    <#list params as param>
    ${this.formatIdentifier(param.getName())}<#if param_has_next>, </#if><#t>
    </#list>
</#macro>