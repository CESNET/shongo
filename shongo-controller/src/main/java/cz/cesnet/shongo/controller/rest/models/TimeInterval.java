package cz.cesnet.shongo.controller.rest.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.joda.time.DateTime;
import org.joda.time.Interval;

@Data
public class TimeInterval
{

    public static final String ISO_8601_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = ISO_8601_PATTERN)
    private DateTime start;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = ISO_8601_PATTERN)
    private DateTime end;

    public static TimeInterval fromApi(Interval interval)
    {
        TimeInterval timeInterval = new TimeInterval();
        timeInterval.setStart(interval.getStart());
        timeInterval.setEnd(interval.getEnd());
        return timeInterval;
    }
}
