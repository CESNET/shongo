package cz.cesnet.shongo.api;

/**
 * Domain controller faults.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public enum ControllerFault implements Fault
{
    OTHER(100, "%s"),
    TODO_IMPLEMENT(999, "TODO: Implement");

    private int code;
    private String string;

    private ControllerFault(int code, String string)
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
