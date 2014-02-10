package cz.cesnet.shongo.controller.util;

import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.junit.Test;

/**
 * Tests for {@link cz.cesnet.shongo.controller.util.iCalendar}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class iCalendarTest
{
    @Test
    public void test() throws Exception
    {
        iCalendar iCalendar = new iCalendar("cz.cesnet", "shongo");
        iCalendar.Event event = iCalendar.addEvent("meetings.cesnet.cz", "exe:1", "Testing meeting");
        event.setDescription("Long long long\nlong long long\nlong description");
        event.setInterval(Interval.parse("2014-02-10T15:30/2014-02-10T16:30"), DateTimeZone.forID("UTC"));
        event.addOrganizer("Martin Srom", "srom.martin@gmail.com");
        event.addOrganizer("Martin Srom", "martin.srom@cesnet.cz");
        event.addAttendee("Martin Srom", "martin.srom@mail.muni.cz");
        System.out.println(iCalendar);
    }
}
