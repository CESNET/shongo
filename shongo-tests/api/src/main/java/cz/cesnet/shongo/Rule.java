package cz.cesnet.shongo;

import cz.cesnet.shongo.common.Type;

/**
 * Class representing periodic date rule
 *
 * @author Martin Srom
 */
public class Rule implements Type
{
    public enum Type {
        Extra,
        Enable,
        Disable
    }

    private String date;
    
    private Type type;

    public Rule() {
    }

    public Rule(Type type, String date) {
        setDate(date);
    }
    
    public void setType(Type type) {
        this.type = type;
    }
    
    public Type getType() {
        return type;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDate() {
        return date;
    }
    
    public String toString() {
        return "Date [" + getDate() + "]";
    }
}
