package cz.cesnet.shongo.common;

/**
 * Represents a duration or period.
 *
 * @author Martin Srom
 */
public class Period
{
    private int year = 0;
    private int month = 0;
    private int day = 0;
    private int hour = 0;
    private int minute = 0;
    private int second = 0;
    private int week = 0;

    /**
     * Constructor
     *
     * @param period ISO8601 Period
     */
    public Period(String period)
    {
        fromString(period);
    }

    /**
     * Set Period from ISO8601
     *
     * @param period
     */
    public void fromString(String period)
    {
        throw new RuntimeException("TODO: Implement Period.fromString ISO8601");
    }

    /**
     * Get period as ISO8601
     *
     * @return string of ISO8601 Period
     */
    public String toString()
    {
        throw new RuntimeException("TODO: Implement Period.toString ISO8601");
    }

}
