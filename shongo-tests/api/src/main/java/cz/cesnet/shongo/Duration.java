package cz.cesnet.shongo;

import cz.cesnet.shongo.common.Type;

/**
 * Class representing duration
 *
 * @author Martin Srom
 */
public class Duration implements Type
{
    private String duration;

    public Duration() {
    }

    public Duration(String duration) {
        setDuration(duration);
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getDuration() {
        return duration;
    }

    public String toString() {
        return "Duration [" + getDuration() + "]";
    }
}
