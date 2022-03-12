package cz.cesnet.shongo.controller.rest.models;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

@Data
@NoArgsConstructor
public class TimeInterval {

    public static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    private DateTime start;
    private DateTime end;

    public TimeInterval(Interval interval) {
        this.start = interval.getStart();
        this.end = interval.getEnd();
    }
}
