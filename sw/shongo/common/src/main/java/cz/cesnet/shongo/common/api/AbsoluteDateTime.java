package cz.cesnet.shongo.common.api;

/**
 * Represents an absolute Date/Time
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AbsoluteDateTime extends DateTime
{
    /**
     * ISO8601 Date/Time
     */
    private String dateTime;

    public String getDateTime()
    {
        return dateTime;
    }

    public void setDateTime(String dateTime)
    {
        this.dateTime = dateTime;
    }
}
