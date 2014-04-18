package cz.cesnet.shongo.generator.report;

import cz.cesnet.shongo.generator.Formatter;

import java.util.Collection;

/**
* TODO:
*
* @author Martin Srom <martin.srom@cesnet.cz>
*/
public class ParamDeclared extends Param
{
    private cz.cesnet.shongo.generator.xml.ReportParam reportParam;

    public ParamDeclared(Report report, cz.cesnet.shongo.generator.xml.ReportParam reportParam)
    {
        super(report);
        this.reportParam = reportParam;
    }

    public cz.cesnet.shongo.generator.xml.ReportParam getXml()
    {
        return reportParam;
    }

    @Override
    public String getOriginalName()
    {
        return reportParam.getName();
    }

    @Override
    public String getName()
    {
        String name = getOriginalName();
        if (name.equals("class")) {
            name = "class-name";
        }
        else if (name.equals("type")) {
            name = "type-name";
        }
        return name;
    }

    @Override
    public String getTypeName()
    {
        return reportParam.getType();
    }

    @Override
    public String getTypeKeyName()
    {
        return reportParam.getTypeKey();
    }

    @Override
    public String getTypeElementName()
    {
        return reportParam.getTypeElement();
    }

    @Override
    public String getValue()
    {
        return getVariableName();
    }

    public Collection<String> getPersistenceAnnotations()
    {
        String[] reportNameParts = report.getBaseClassName().split("\\.");
        String reportName = Formatter.splitCamelCase(reportNameParts[reportNameParts.length - 1]);
        String columnName = Formatter.splitCamelCase(getName());
        reportName = Formatter.formatConstant(reportName).toLowerCase();
        columnName = Formatter.formatConstant(columnName).toLowerCase();
        return getType().getPersistenceAnnotations(reportName, columnName);
    }

    public String getVariableName()
    {
        return Formatter.formatCamelCaseFirstLower(getName());
    }

    public String getGetterName()
    {
        return "get" + Formatter.formatCamelCaseFirstUpper(getName());
    }

    public String getGetterContent(boolean isPersistent)
    {
        if (isPersistent) {
            return getType().getPersistentGetterContent(getVariableName());
        }
        else {
            return getVariableName();
        }
    }

    public String getSetterContent(boolean isPersistent)
    {
        if (isPersistent) {
            return getType().getPersistentSetterContent(getVariableName());
        }
        else {
            return getVariableName();
        }
    }

    public String getSetterName()
    {
        return "set" + Formatter.formatCamelCaseFirstUpper(getName());
    }
}
