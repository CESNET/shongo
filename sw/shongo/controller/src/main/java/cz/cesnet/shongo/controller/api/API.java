package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.common.api.ComplexType;
import org.joda.time.DateTime;
import org.joda.time.Period;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller API data types.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class API extends cz.cesnet.shongo.common.api.API
{
    public static class PeriodicDateTime extends ComplexType
    {
        public DateTime start;

        public Period period;

        public static PeriodicDateTime create(DateTime start, Period period)
        {
            PeriodicDateTime periodicDateTime = new PeriodicDateTime();
            periodicDateTime.start = start;
            periodicDateTime.period = period;
            return periodicDateTime;
        }
    }

    public static class DateTimeSlot extends ComplexType
    {
        @AllowedTypes({DateTime.class, PeriodicDateTime.class})
        public Object dateTime;

        public Period duration;

        public static DateTimeSlot create(Object dateTime, Period duration)
        {
            DateTimeSlot dateTimeSlot = new DateTimeSlot();
            dateTimeSlot.dateTime = dateTime;
            dateTimeSlot.duration = duration;
            return dateTimeSlot;
        }
    }

    public static class Person extends ComplexType
    {
        public String name;
    }

    public static class Compartment extends ComplexType
    {
        public List<Person> persons = new ArrayList<Person>();
    }

    public static class ReservationRequest extends ComplexType
    {
        public static enum Type
        {
            NORMAL,
            PERNAMENT
        }

        public static enum Purpose
        {
            SCIENCE,
            EDUCATION
        }

        public Type type;

        public Purpose purpose;

        public List<DateTimeSlot> slots = new ArrayList<DateTimeSlot>();

        public List<Compartment> compartments = new ArrayList<Compartment>();


        public void addSlot(Object dateTime, Period duration)
        {
            slots.add(DateTimeSlot.create(dateTime, duration));
        }

        public Compartment addCompartment()
        {
            Compartment compartment = new Compartment();
            compartments.add(compartment);
            return compartment;
        }
    }
}
