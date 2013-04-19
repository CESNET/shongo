package cz.cesnet.shongo.generator.report;

import cz.cesnet.shongo.generator.Formatter;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class Param
{
    protected Report report;

    public Param(Report report)
    {
        this.report = report;
    }

    public abstract String getOriginalName();

    public String getName()
    {
        return getOriginalName();
    }

    public abstract String getTypeName();

    public String getTypeElementName()
    {
        return null;
    }

    public Type getType()
    {
        return Type.getType(getTypeName());
    }

    public String getTypeClassName()
    {
        return getType().getClassName(getTypeElementName());
    }

    public abstract String getValue();

    public String getValueString()
    {
        return getType().getString(getValue());
    }
}
