package cz.cesnet.shongo.controller.api;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public enum Fault implements cz.cesnet.shongo.common.api.Fault
{
    OTHER(100, "%s"),
    TODO_IMPLEMENT(999, "TODO: Implement");

    private int code;
    private String string;

    private Fault(int code, String string)
    {
        this.code = code;
        this.string = string;
    }

    @Override
    public int getCode()
    {
        return code;
    }

    @Override
    public String getString()
    {
        return string;
    }
}
