package cz.cesnet.shongo.controller.util;

import cz.cesnet.shongo.TodoImplementException;
import net.fortuna.ical4j.model.*;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.component.VTimeZone;
import net.fortuna.ical4j.model.parameter.Cn;
import net.fortuna.ical4j.model.parameter.Role;
import net.fortuna.ical4j.model.property.*;
import org.joda.time.DateTimeZone;

import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;

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
     * List of {@link iCalendar.Event}s.
     */
    private List<Event> events = new LinkedList<Event>();

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

        TimeZoneRegistry registry = new TimeZoneRegistryImpl("zoneinfo-outlook/");
        org.joda.time.DateTimeZone dateTimeZone = DateTimeZone.getDefault();
        TimeZone zone = registry.getTimeZone(dateTimeZone.getID());
        if(zone != null) {
            this.calendar.getComponents().add(zone.getVTimeZone());
        }
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
     * Constructor.
     */
    public iCalendar()
    {
        this("CESNET", "Shongo", "EN");
    }

    /**
     * @param method to be set to all {@link #events}
     */
    public void setMethod(Method method)
    {
        PropertyList properties = this.calendar.getProperties();
        removeProperty(properties, Property.METHOD);
        switch (method) {
            case CREATE:
                properties.add(net.fortuna.ical4j.model.property.Method.REQUEST);
                break;
            case UPDATE:
                properties.add(net.fortuna.ical4j.model.property.Method.REQUEST);
                break;
            case CANCEL:
                properties.add(net.fortuna.ical4j.model.property.Method.CANCEL);
                break;
            default:
                throw new TodoImplementException(method);
        }
        for (Event event : events) {
            event.updateStatus();
        }
    }

    /**
     * @param sequence to be set for all {@link #events}
     */
    public void setSequence(int sequence)
    {
        for (Event event : events) {
            event.setSequence(sequence);
        }
    }

    /**
     * @param organizer to be set for all {@link #events}
     */
    public void setOrganizer(String organizer)
    {
        for (Event event : events) {
            event.setOrganizer(organizer);
        }
    }

    /**
     * @param event to be aded to the {@link #calendar}
     */
    public void addEvent(Event event)
    {
        events.add(event);
        calendar.getComponents().add(event.event);
    }

    /**
     * @param domain
     * @param eventId
     * @return newly added {@link iCalendar.Event}
     */
    public Event addEvent(String domain, String eventId)
    {
        Event event = new Event(domain, eventId);
        addEvent(event);
        return event;
    }

    /**
     * @param domain
     * @param eventId
     * @param summary
     * @return newly added {@link iCalendar.Event}
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
        if (timezone == null) {
            throw new IllegalArgumentException("Unknown timezone " + dateTimeZone);
        }
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
        try {
            if (!calendar.getComponents().isEmpty()) {
                calendar.validate();
            }
        }
        catch (ValidationException exception) {
            System.err.println(calendar.toString());
            throw new RuntimeException(exception);
        }
        return calendar.toString();
    }

    public class Event
    {
        private VEvent event;

        public Event(String domain, String eventId)
        {
            this.event = new VEvent();
            this.event.getProperties().add(new Uid(eventId + "@" + domain));
            this.event.getProperties().add(new LastModified(getDateTime(org.joda.time.DateTime.now())));
        }

        public void setSequence(int sequenceNo)
        {
            PropertyList properties = event.getProperties();
            removeProperty(properties, Property.SEQUENCE);
            properties.add(new Sequence(sequenceNo));
        }

        public void setInterval(org.joda.time.Interval interval, org.joda.time.DateTimeZone dateTimeZone)
        {
            VTimeZone timeZone = getVTimeZone(dateTimeZone);
            DateTime start = getDateTime(interval.getStart().withZone(dateTimeZone));
            DateTime end = getDateTime(interval.getEnd().withZone(dateTimeZone));

            DtStart dtStart = new DtStart(start);
            dtStart.setTimeZone(new TimeZone(timeZone));
            DtEnd dtEnd = new DtEnd(end);
            dtEnd.setTimeZone(new TimeZone(timeZone));

            PropertyList properties = event.getProperties();
            removeProperty(properties, Property.TZID);
            removeProperty(properties, Property.DTSTART);
            removeProperty(properties, Property.DTEND);
            properties.add(dtStart);
            properties.add(dtEnd);
        }

        public void setSummary(String summary)
        {
            PropertyList properties = event.getProperties();
            removeProperty(properties, Property.SUMMARY);
            properties.add(new Summary(summary));
        }

        public void setDescription(String description)
        {
            PropertyList properties = event.getProperties();
            removeProperty(properties, Property.DESCRIPTION);
            properties.add(new Description(description));
        }

        public void addAttendee(String name, String email)
        {
            Attendee attendee;
            try {
                attendee = new Attendee("mailto:" + email.trim());
                attendee.getParameters().add(Role.REQ_PARTICIPANT);
                attendee.getParameters().add(new Cn(name));
            }
            catch (URISyntaxException exception) {
                throw new IllegalArgumentException(exception);
            }
            event.getProperties().add(attendee);
        }

        private void updateStatus()
        {
            PropertyList properties = event.getProperties();
            removeProperty(properties, Property.STATUS);
            net.fortuna.ical4j.model.property.Method method = (net.fortuna.ical4j.model.property.Method)
                    calendar.getProperties().getProperty(Property.METHOD);
            if (net.fortuna.ical4j.model.property.Method.PUBLISH.equals(method)) {
                properties.add(Status.VEVENT_CONFIRMED);
            }
            else if (net.fortuna.ical4j.model.property.Method.CANCEL.equals(method)) {
                properties.add(Status.VEVENT_CANCELLED);
            }
        }

        public void setMethod(Method method)
        {
            PropertyList properties = event.getProperties();
            removeProperty(properties, Property.STATUS);
//            net.fortuna.ical4j.model.property.Method method = (net.fortuna.ical4j.model.property.Method)
//                    calendar.getProperties().getProperty(Property.METHOD);
            switch (method) {
                case CREATE:
                case UPDATE:
                    properties.add(Status.VEVENT_CONFIRMED);
                    break;
                case CANCEL:
                    properties.add(Status.VEVENT_CANCELLED);
                    break;
            }
            /*if (net.fortuna.ical4j.model.property.Method.PUBLISH.equals(method)) {
                properties.add(Status.VEVENT_CONFIRMED);
            }
            else if (net.fortuna.ical4j.model.property.Method.CANCEL.equals(method)) {
                properties.add(Status.VEVENT_CANCELLED);
            }*/
        }

        public void setOrganizer(String organizer)
        {
            try {
                PropertyList properties = event.getProperties();
                removeProperty(properties, Property.ORGANIZER);
                properties.add(new Organizer(organizer));
            }
            catch (URISyntaxException exception) {
                throw new IllegalArgumentException(exception);
            }
        }

        public void setOrganizerName (String name) {
            ParameterList params = event.getProperty(Property.ORGANIZER).getParameters();
            removeParameter(params, Parameter.CN);
            params.add(new Cn(name));
        }

        public void setLocation(String location)
        {
            PropertyList properties = event.getProperties();
            removeProperty(properties, Property.LOCATION);
            properties.add(new Location(location));
        }
    }

    private static void removeProperty(PropertyList properties, String propertyName)
    {
        Property property = properties.getProperty(propertyName);
        if (property != null) {
            properties.remove(property);
        }
    }

    private static void removeParameter(ParameterList parameters, String parameterName) {
        Parameter parameter =  parameters.getParameter(parameterName);
        if (parameter != null) {
            parameters.remove(parameter);
        }
    }

    public static enum Method
    {
        CREATE,
        UPDATE,
        CANCEL
    }
}
