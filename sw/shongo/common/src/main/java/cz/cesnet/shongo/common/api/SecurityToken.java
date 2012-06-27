package cz.cesnet.shongo.common.api;

import cz.cesnet.shongo.common.xmlrpc.StructType;

/**
 * Represents a security token
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class SecurityToken implements StructType
{
    private String test;

    public void setTest(String test)
    {
        this.test = test;
    }

    public String getTest()
    {
        return test;
    }
}
