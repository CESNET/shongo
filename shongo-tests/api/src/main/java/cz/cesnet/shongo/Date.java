package cz.cesnet.shongo;

import cz.cesnet.shongo.common.Type;

/**
 * Class representing date and time
 *
 * @author Martin Srom
 */
public class Date implements Type
{
    private String date;

    public Date() {
    }
    
    public Date(String date) {
        setDate(date);
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
