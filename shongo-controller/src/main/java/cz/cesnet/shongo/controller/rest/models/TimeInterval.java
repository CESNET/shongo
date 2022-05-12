package cz.cesnet.shongo.controller.rest.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

@Data
public class TimeInterval
{

    private static final String ISO_8601_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    public static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormat.forPattern(ISO_8601_PATTERN);

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
