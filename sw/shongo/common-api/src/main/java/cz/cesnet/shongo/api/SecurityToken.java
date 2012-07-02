package cz.cesnet.shongo.api;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class SecurityToken extends ComplexType
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
