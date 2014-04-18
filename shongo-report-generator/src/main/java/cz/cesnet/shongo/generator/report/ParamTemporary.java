package cz.cesnet.shongo.generator.report;

import cz.cesnet.shongo.generator.Formatter;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ParamTemporary extends Param
{
    private cz.cesnet.shongo.generator.xml.ReportParamTemporary reportParamTemporary;

    public ParamTemporary(Report report, cz.cesnet.shongo.generator.xml.ReportParamTemporary reportParamTemporary)
    {
        super(report);
        this.reportParamTemporary = reportParamTemporary;
    }

    public cz.cesnet.shongo.generator.xml.ReportParamTemporary getXml()
    {
        return reportParamTemporary;
    }

    @Override
    public String getOriginalName()
    {
        return reportParamTemporary.getName();
    }

    @Override
    public String getTypeName()
    {
        return reportParamTemporary.getType();
    }

    @Override
    public String getValue()
    {
        return getGetterName() + "()";
    }

    public String getGetterName()
    {
        return "get" + Formatter.formatCamelCaseFirstUpper(getName());
    }

    public String getCode()
    {
        return new ParamReplace(reportParamTemporary.getValue(), report, new ParamReplace.Context()
        {
            @Override
            public String processParam(Param param)
            {
                return param.getValue();
            }
        }).getString();
    }
}
