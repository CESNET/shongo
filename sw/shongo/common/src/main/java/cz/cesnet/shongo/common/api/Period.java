package cz.cesnet.shongo.common.api;

/**
 * Represents a Date/Time duration/period
 *
 * @author Martin Srom
 */
public class Period
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
}
