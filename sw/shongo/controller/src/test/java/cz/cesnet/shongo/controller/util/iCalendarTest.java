package cz.cesnet.shongo.controller.util;

import cz.cesnet.shongo.controller.EmailSender;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.junit.Test;

/**
 * Tests for {@link iCalendar}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class iCalendarTest
{
    private static final String DOMAIN = "shongo-test.cesnet.cz";
    private static final String EVENT_ID = "3";
    private static final String ORGANIZER = "no-reply@shongo-test.com";
    private static final String ATTENDEE = "srom.martin@gmail.com";

    @Test
    public void testCreate() throws Exception
    {
        iCalendar calendar = new iCalendar();
        calendar.setMethod(iCalendar.Method.CREATE);
        iCalendar.Event event = calendar.addEvent(DOMAIN, EVENT_ID, "Testing meeting 1");
        event.setOrganizer(ORGANIZER);
        event.setSequence(0);
        event.setDescription("Long long long\nlong long long\nlong description");
        event.setInterval(Interval.parse("2014-02-11T15:30/2014-02-11T16:30"), DateTimeZone.forID("UTC"));
        event.addAttendee("Martin Srom", ATTENDEE);
        finish("create", calendar);
    }

    @Test
    public void testEdit() throws Exception
    {
        iCalendar calendar = new iCalendar();
        calendar.setMethod(iCalendar.Method.UPDATE);
        iCalendar.Event event = calendar.addEvent(DOMAIN, EVENT_ID, "Testing meeting 2");
        event.setOrganizer(ORGANIZER);
        event.setSequence(1);
        event.setDescription("Short description");
        event.setInterval(Interval.parse("2014-02-11T16:30/2014-02-11T17:30"), DateTimeZone.forID("UTC"));
        event.addAttendee("Martin Srom", ATTENDEE);
        finish("modify", calendar);
    }

    @Test
    public void testCancel() throws Exception
    {
        iCalendar calendar = new iCalendar();
        calendar.setMethod(iCalendar.Method.CANCEL);
        iCalendar.Event event = calendar.addEvent(DOMAIN, EVENT_ID, "Testing meeting 2");
        event.setOrganizer(ORGANIZER);
        event.setSequence(2);
        event.setInterval(Interval.parse("2014-02-11T16:30/2014-02-11T17:30"), DateTimeZone.forID("UTC"));
        event.addAttendee("Martin Srom", ATTENDEE);
        finish("delete", calendar);
    }

    private void finish(String subject, iCalendar calendar) throws Exception
    {
        System.out.println(calendar);

        // Send email
        //EmailSender emailSender = new EmailSender(<fill>);
        //EmailSender.Email email = new EmailSender.Email("srom.martin@gmail.com", subject, "ahoj\nahoj\n\nahoj");
        //email.addAttachment("invite.ics", calendar.toString());
        //emailSender.sendEmail(email);
    }
}
