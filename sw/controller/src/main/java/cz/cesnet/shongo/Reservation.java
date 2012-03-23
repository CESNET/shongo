package cz.cesnet.shongo;

import cz.cesnet.shongo.common.Type;

/**
 * Represents reservation
 *
 * @author Martin Srom
 */
public class Reservation implements Type
{
    private String id;
    
    private ReservationType type;
    
    private Date date;

    private Duration duration;
    
    private String description;

    public Reservation() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ReservationType getType() {
        return type;
    }

    public void setType(ReservationType type) {
        this.type = type;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
