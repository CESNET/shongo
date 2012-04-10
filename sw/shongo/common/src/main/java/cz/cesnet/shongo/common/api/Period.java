package cz.cesnet.shongo.common.api;

import cz.cesnet.shongo.common.xmlrpc.AtomicType;

/**
 * Represents a Date/Time duration/period
 *
 * @author Martin Srom
 */
public class Period implements AtomicType
{
    /**
     * ISO8601 duration
     */
    private String period;

    public String getPeriod()
    {
        return period;
    }

    public void setPeriod(String period)
    {
        this.period = period;
    }

    @Override
    public void fromString(String string)
    {
        setPeriod(string);
    }
}
