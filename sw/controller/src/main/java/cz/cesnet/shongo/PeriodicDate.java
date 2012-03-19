package cz.cesnet.shongo;

/**
 * Represents periodical date and time
 *
 * @author Martin Srom
 */
public class PeriodicDate extends Date
{
    private String end;

    public PeriodicDate() {
    }

    public PeriodicDate(String date, String end) {
        super(date);
        setEnd(end);
    }
    
    public void setEnd(String end) {
        this.end = end;
    }

    public String getEnd() {
        return end;
    }

    public String toString() {
        return "PeriodicDate [" + getDate() + ", " + getEnd() + "]";
    }
}
