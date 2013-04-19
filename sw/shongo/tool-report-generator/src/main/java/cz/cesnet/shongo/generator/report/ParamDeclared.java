package cz.cesnet.shongo.generator.report;

import cz.cesnet.shongo.generator.Formatter;

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
    public String getTypeElementName()
    {
        return reportParam.getTypeElement();
    }

    @Override
    public String getValue()
    {
        return getVariableName();
    }

    @Override
    public String getValueString()
    {
        return getType().getString(getVariableName());
    }

    public String getPersistenceAnnotation()
    {
        String columnName = Formatter.formatConstant(getName()).toLowerCase();
        return getType().getPersistenceAnnotation(columnName);
    }

    public String getVariableName()
    {
        return Formatter.formatCamelCaseFirstLower(getName());
    }

    public String getGetterName()
    {
        return "get" + Formatter.formatCamelCaseFirstUpper(getName());
    }

    public String getSetterName()
    {
        return "set" + Formatter.formatCamelCaseFirstUpper(getName());
    }
}
