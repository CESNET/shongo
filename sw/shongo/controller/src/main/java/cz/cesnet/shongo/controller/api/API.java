package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.common.api.ComplexType;

/**
 * Controller API data types.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class API extends cz.cesnet.shongo.common.api.API
{
    public static class PeriodicDateTime extends ComplexType
    {
        private String start;

        private String period;

        public String getStart()
        {
            return start;
        }

        @Required
        public void setStart(String start)
        {
            this.start = start;
        }

        public String getPeriod()
        {
            return period;
        }

        @Required
        public void setPeriod(String period)
        {
            this.period = period;
        }
    }

    public static class DateTimeSlot extends ComplexType
    {
        private Object dateTime;

        private String duration;

        public Object getDateTime()
        {
            return dateTime;
        }

        @AllowedTypes({String.class, PeriodicDateTime.class})
        public void setDateTime(Object dateTime)
        {
            this.dateTime = dateTime;
        }

        public String getDuration()
        {
            return duration;
        }

        @Required
        public void setDuration(String duration)
        {
            this.duration = duration;
        }
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

        private Type type;

        private Purpose purpose;

        private DateTimeSlot[] slots;

        public Type getType()
        {
            return type;
        }

        @Required
        public void setType(Type type)
        {
            this.type = type;
        }

        public Purpose getPurpose()
        {
            return purpose;
        }

        @Required
        public void setPurpose(Purpose purpose)
        {
            this.purpose = purpose;
        }

        public DateTimeSlot[] getSlots()
        {
            return slots;
        }

        @Required
        public void setSlots(DateTimeSlot[] slots)
        {
            this.slots = slots;
        }
    }
}
