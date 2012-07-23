package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.Fault;

/**
 * Domain controller faults.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public enum ControllerFault implements Fault
{
    PREPROCESSOR_FAILED(100, "Preprocessor failed"),

    OTHER(999, "%s");

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
