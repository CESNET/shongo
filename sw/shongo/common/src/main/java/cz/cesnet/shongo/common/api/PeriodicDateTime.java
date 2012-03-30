package cz.cesnet.shongo.common.api;

/**
 * Represents a Date/Time that takes places periodically
 *
 * @author Martin Srom
 */
public class PeriodicDateTime extends DateTime
{
    /**
     * First periodic Date/Time
     */
    private AbsoluteDateTime start;

    /**
     * Specifies period
     */
    private Period period;

    /**
     * Specifies ending Date/Time
     */
    private AbsoluteDateTime end;

    /**
     * Extra events and Enable/Disable rules
     */
    private Rule[] rules;

    /**
     * Represents a rule in Periodic Date/Time
     *
     * @author Martin Srom
     */
    public static class Rule
    {
        /**
         * Type of rule
         */
        private Type type;

        /**
         * Specifies one date
         */
        private AbsoluteDateTime dateTime;

        /**
         * Specifies interval start
         */
        private AbsoluteDateTime from;

        /**
         * Specifies interval end
         */
        private AbsoluteDateTime to;

        /**
         * Type of rule definition
         */
        public enum Type
        {
            Enable,
            Disable,
            Extra
        }

        public Type getType()
        {
            return type;
        }

        public void setType(Type type)
        {
            this.type = type;
        }

        public AbsoluteDateTime getDateTime()
        {
            return dateTime;
        }

        public void setDateTime(AbsoluteDateTime dateTime)
        {
            this.dateTime = dateTime;
        }

        public AbsoluteDateTime getFrom()
        {
            return from;
        }

        public void setFrom(AbsoluteDateTime from)
        {
            this.from = from;
        }

        public AbsoluteDateTime getTo()
        {
            return to;
        }

        public void setTo(AbsoluteDateTime to)
        {
            this.to = to;
        }
    }

    public AbsoluteDateTime getStart()
    {
        return start;
    }

    public void setStart(AbsoluteDateTime start)
    {
        this.start = start;
    }

    public Period getPeriod()
    {
        return period;
    }

    public void setPeriod(Period period)
    {
        this.period = period;
    }

    public AbsoluteDateTime getEnd()
    {
        return end;
    }

    public void setEnd(AbsoluteDateTime end)
    {
        this.end = end;
    }

    public Rule[] getRules()
    {
        return rules;
    }

    public void setRules(Rule[] rules)
    {
        this.rules = rules;
    }
}
