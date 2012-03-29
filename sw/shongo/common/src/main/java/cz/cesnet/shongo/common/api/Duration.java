package cz.cesnet.shongo.common.api;

/**
 * Represents a Date/Time duration/period
 *
 * @author Martin Srom
 */
public class Duration
{
    /** ISO8601 duration */
    private String duration;

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }
}
