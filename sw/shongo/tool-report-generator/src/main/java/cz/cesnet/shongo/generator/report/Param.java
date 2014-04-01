package cz.cesnet.shongo.generator.report;

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

    public String getTypeKeyName()
    {
        return null;
    }

    public String getTypeElementName()
    {
        return null;
    }

    public Type getType()
    {
        return Type.getType(getTypeName(), getTypeKeyName(), getTypeElementName());
    }

    public String getTypeClassName()
    {
        return getType().getClassName();
    }

    public abstract String getValue();

    public String getValueMessage()
    {
        return getType().getMessage(getValue());
    }
}
