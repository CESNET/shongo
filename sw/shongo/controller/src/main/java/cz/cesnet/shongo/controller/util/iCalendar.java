package cz.cesnet.shongo.controller.util;

import net.fortuna.ical4j.model.*;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.component.VTimeZone;
import net.fortuna.ical4j.model.parameter.Cn;
import net.fortuna.ical4j.model.parameter.Role;
import net.fortuna.ical4j.model.property.*;

import java.net.URISyntaxException;

/**
 * Represents iCalendar.
 *
 * @see <a href="http://tools.ietf.org/html/rfc5545">iCalendar</a>
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class iCalendar
{
    /**
     * @see net.fortuna.ical4j.model.Calendar
     */
    private Calendar calendar;

    /**
     * @see net.fortuna.ical4j.model.TimeZoneRegistry
     */
    private TimeZoneRegistry timeZoneRegistry = TimeZoneRegistryFactory.getInstance().createRegistry();

    /**
     * Constructor.
     *
     * @param author
     * @param description
     * @param language
     */
    public iCalendar(String author, String description, String language)
    {
        this.calendar = new Calendar();
        this.calendar.getProperties().add(new ProdId("-//" + author + "//" + description + "//" + language));
        this.calendar.getProperties().add(Version.VERSION_2_0);
        this.calendar.getProperties().add(CalScale.GREGORIAN);
    }

    /**
     * Constructor.
     *
     * @param author
     * @param description
     */
    public iCalendar(String author, String description)
    {
        this(author, description, "EN");
    }

    /**
     * @param event to be aded to the {@link #calendar}
     */
    public void addEvent(Event event)
    {
        calendar.getComponents().add(event.event);
    }

    /**
     * @param domain
     * @param eventId
     * @param summary
     * @return newly added {@link cz.cesnet.shongo.controller.util.iCalendar.Event}
     */
    public Event addEvent(String domain, String eventId, String summary)
    {
        Event event = new Event(domain, eventId);
        event.setSummary(summary);
        addEvent(event);
        return event;
    }

    /**
     * @param dateTimeZone {@link org.joda.time.DateTimeZone}
     * @return {@link net.fortuna.ical4j.model.component.VTimeZone} from given {@code dateTimeZone}
     */
    private VTimeZone getVTimeZone(org.joda.time.DateTimeZone dateTimeZone)
    {
        TimeZone timezone = timeZoneRegistry.getTimeZone(dateTimeZone.getID());
        return timezone.getVTimeZone();
    }

    /**
     * @param dateTime {@link org.joda.time.DateTime}
     * @return {@link net.fortuna.ical4j.model.DateTime} from given {@code dateTime}
     */
    private DateTime getDateTime(org.joda.time.DateTime dateTime)
    {
        return new DateTime(dateTime.getMillis());
    }

    @Override
    public String toString()
    {
        return calendar.toString();
    }

    public class Event
    {
        private VEvent event;

        public Event(String domain, String eventId)
        {
            this.event = new VEvent();
            this.event.getProperties().add(new Uid(eventId + "@" + domain));
        }

        public void setInterval(org.joda.time.Interval interval, org.joda.time.DateTimeZone dateTimeZone)
        {
            VTimeZone timeZone = getVTimeZone(dateTimeZone);
            DateTime start = getDateTime(interval.getStart().withZone(dateTimeZone));
            DateTime end = getDateTime(interval.getEnd().withZone(dateTimeZone));

            PropertyList properties = event.getProperties();
            properties.remove(Property.TZID);
            properties.remove(Property.DTSTART);
            properties.remove(Property.DTEND);
            properties.add(timeZone.getTimeZoneId());
            properties.add(new DtStart(start));
            properties.add(new DtEnd(end));
        }

        public void setSummary(String summary)
        {
            PropertyList properties = event.getProperties();
            properties.remove(Property.SUMMARY);
            properties.add(new Summary(summary));
        }

        public void setDescription(String description)
        {
            PropertyList properties = event.getProperties();
            properties.remove(Property.DESCRIPTION);
            properties.add(new Description(description));
        }

        public void addOrganizer(String name, String email)
        {
            Organizer organizer;
            try {
                organizer = new Organizer("mailto:" + email);
                organizer.getParameters().add(Role.CHAIR);
                organizer.getParameters().add(new Cn(name));
            }
            catch (URISyntaxException exception) {
                throw new IllegalArgumentException(exception);
            }
            event.getProperties().add(organizer);
        }

        public void addAttendee(String name, String email)
        {
            Attendee attendee;
            try {
                attendee = new Attendee("mailto:" + email);
                attendee.getParameters().add(Role.REQ_PARTICIPANT);
                attendee.getParameters().add(new Cn(name));
            }
            catch (URISyntaxException exception) {
                throw new IllegalArgumentException(exception);
            }
            event.getProperties().add(attendee);
        }
    }
}
