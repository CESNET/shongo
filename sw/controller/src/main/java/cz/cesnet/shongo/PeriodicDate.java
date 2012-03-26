package cz.cesnet.shongo;

/**
 * Represents periodical date and time
 *
 * @author Martin Srom
 */
public class PeriodicDate extends Date
{
    private String end;
    
    private Rule[] rules;

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

    public void setRules(Rule[] rules) {
        this.rules = rules;
    }

    public Rule[] getRules() {
        return rules;
    }

    public String toString() {
        return "PeriodicDate [" + getDate() + ", " + getEnd() + "]";
    }
}
