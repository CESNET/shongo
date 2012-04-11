package cz.cesnet.shongo.common;

import java.util.*;

/**
 * Represents a Date/Time of events that takes place periodically.
 *
 * @author Martin Srom
 */
public class PeriodicDateTime extends DateTime
{
    private AbsoluteDateTime start;

    private Period period;

    private AbsoluteDateTime end;

    private ArrayList<Rule> rules = new ArrayList<Rule>();

    /**
     * Constructs empty periodical date/time events.
     */
    public PeriodicDateTime()
    {
        this(null, null, null);
    }

    /**
     * Constructs periodical date/time events. The first event takes place at start
     * and each other take places in given period.
     *
     * @param start
     * @param period
     */
    public PeriodicDateTime(AbsoluteDateTime start, Period period)
    {
        this(start, period, null);
    }

    /**
     * Constructs periodical date/time events. The first event takes place at start
     * and each other take places in given period. The last event takes place before
     * or equals to end.
     *
     * @param start
     * @param period
     */
    public PeriodicDateTime(AbsoluteDateTime start, Period period, AbsoluteDateTime end)
    {
        setStart(start);
        setPeriod(period);
        setEnd(end);
    }

    /**
     * Get date/time of the first periodic event.
     *
     * @return absolute data/time
     */
    public AbsoluteDateTime getStart()
    {
        return start;
    }

    /**
     * Set date/time of the first periodic event.
     *
     * @param start
     */
    public void setStart(AbsoluteDateTime start)
    {
        this.start = start;
    }

    /**
     * Get period of periodic events.
     *
     * @return period
     */
    public Period getPeriod()
    {
        return period;
    }

    /**
     * Set events period.
     *
     * @param period
     */
    public void setPeriod(Period period)
    {
        this.period = period;
    }

    /**
     * Get ending date/time after which the periodic events are not considered.
     *
     * @return absolute date/time
     */
    public AbsoluteDateTime getEnd()
    {
        return end;
    }

    /**
     * Set ending date/time after which the periodic events are not considered.
     * The ending date/time can be nice e.g., 31.12.2012, and for periodic events
     * on every Thursday the last will take place on 29.12.2012.
     *
     * @param end
     */
    public void setEnd(AbsoluteDateTime end)
    {
        this.end = end;
    }

    /**
     * Add a new rule for periodic date, that can add extra date/time
     * outside the periodicity or enable/disable periodic events for
     * specified interval.
     *
     * @param rule
     */
    public void addRule(Rule rule)
    {
        rules.add(rule);
    }

    /**
     * Add a new rule for periodic date/time.
     *
     * @param type     Type of rule
     * @param dateTime Concrete date/time
     */
    public void addRule(RuleType type, AbsoluteDateTime dateTime)
    {
        addRule(new Rule(type, dateTime));
    }

    /**
     * Add a new rule for periodic date/time.
     *
     * @param type         Type of rule
     * @param dateTimeFrom Start of interval
     * @param dateTimeTo   End of interval
     */
    public void addRule(RuleType type, AbsoluteDateTime dateTimeFrom, AbsoluteDateTime dateTimeTo)
    {
        addRule(new Rule(type, dateTimeFrom, dateTimeTo));
    }

    /**
     * Remove all rules.
     */
    public void clearRules()
    {
        rules.clear();
    }

    /**
     * Get all rules.
     *
     * @return rules
     */
    public Rule[] getRules()
    {
        return rules.toArray(new Rule[rules.size()]);
    }

    /**
     * Enumerate all periodic Date/Time events to array of absolute Date/Times.
     *
     * @return array of absolute Date/Times
     */
    public AbsoluteDateTime[] enumerate()
    {
        return enumerate(null, null);
    }

    /**
     * Enumerate all periodic Date/Time events to array of absolute Date/Times.
     * Return only events that take place inside interval defined by from - to.
     *
     * @param from
     * @param to
     * @return array of absolute Date/Times
     */
    public AbsoluteDateTime[] enumerate(AbsoluteDateTime from, AbsoluteDateTime to)
    {
        // Find all events in range from-to
        List<AbsoluteDateTime> dateTimeList = new ArrayList<AbsoluteDateTime>();
        if (this.start != null) {
            AbsoluteDateTime start = this.start.clone();
            while (start.after(this.end) == false) {
                if (to != null && start.after(to)) {
                    break;
                }
                if (from == null || start.before(from) == false) {
                    dateTimeList.add(start);
                }
                start = start.add(period);
            }
        }

        // Build set of indexes for disabled date/times from the list
        Set<Integer> disabledSet = new HashSet<Integer>();
        for (Rule rule : rules) {
            // Extra rule
            if (rule.getType() == RuleType.Extra) {
                AbsoluteDateTime ruleDateTime = rule.getDateTime();
                if ((from != null && ruleDateTime.before(from)) || (to != null && ruleDateTime.after(to))) {
                    continue;
                }
                if (this.start != null) {
                    dateTimeList.add(this.start.merge(ruleDateTime));
                }
                else {
                    dateTimeList.add(ruleDateTime);
                }
                continue;
            }

            // Enable/disable rule
            RuleType type = rule.getType();
            assert (type == RuleType.Enable || type == RuleType.Disable) : "Rule type should be enable or disable.";
            // Interval
            if (rule.isInterval()) {
                AbsoluteDateTime ruleFrom = rule.getDateTimeFrom();
                AbsoluteDateTime ruleTo = rule.getDateTimeTo();
                for (int index = 0; index < dateTimeList.size(); index++) {
                    AbsoluteDateTime dateTime = dateTimeList.get(index);
                    if (dateTime.before(ruleFrom) == false && dateTime.after(ruleTo) == false) {
                        if (type == RuleType.Enable) {
                            disabledSet.remove(index);
                        }
                        else {
                            disabledSet.add(index);
                        }
                    }
                }
            }
            // Single date/time
            else {
                AbsoluteDateTime ruleDateTime = rule.getDateTime();
                for (int index = 0; index < dateTimeList.size(); index++) {
                    AbsoluteDateTime dateTime = dateTimeList.get(index);
                    if (dateTime.match(ruleDateTime)) {
                        if (type == RuleType.Enable) {
                            disabledSet.remove(index);
                        }
                        else {
                            disabledSet.add(index);
                        }
                    }
                }
            }
        }

        // Order disabled indexes
        Integer[] disabled = disabledSet.toArray(new Integer[disabledSet.size()]);
        Arrays.sort(disabled, Collections.reverseOrder());
        // Remove disabled date/times
        for (Integer index : disabled) {
            dateTimeList.remove(index.intValue());
        }

        return dateTimeList.toArray(new AbsoluteDateTime[dateTimeList.size()]);
    }

    @Override
    public AbsoluteDateTime getEarliest(AbsoluteDateTime referenceDateTime)
    {
        AbsoluteDateTime[] dateTimes = enumerate();
        for ( AbsoluteDateTime dateTime : dateTimes ) {
            if (dateTime.before(referenceDateTime) == false)
                return dateTime;
        }
        return null;
    }

    @Override
    public boolean equals(Object object)
    {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }

        PeriodicDateTime periodicDateTime = (PeriodicDateTime) object;
        AbsoluteDateTime[] dateTimes1 = enumerate();
        AbsoluteDateTime[] dateTimes2 = periodicDateTime.enumerate();
        if (dateTimes1.length != dateTimes2.length) {
            return false;
        }
        for (int index = 0; index < dateTimes1.length; index++) {
            if (dateTimes1[index].equals(dateTimes2[index]) == false) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString()
    {
        AbsoluteDateTime[] dateTimes = enumerate();
        StringBuilder result = new StringBuilder();
        for (AbsoluteDateTime dateTime : dateTimes) {
            if (result.length() > 0) {
                result.append(", ");
            }
            result.append(dateTime.toString());
        }
        return "PeriodicDateTime [" + result.toString() + "]";
    }

    /**
     * Periodic date/time rule type.
     *
     * @author Martin Srom
     */
    public enum RuleType
    {
        /**
         * Represents a rule that will add new event outside periodicity.
         */
        Extra,

        /**
         * Represents a rule for enabling events by concrete date/time or by interval from - to.
         */
        Enable,

        /**
         * Represents a rule for disabling events by concrete date/time or by interval from - to.
         */
        Disable
    }

    /**
     * Periodic date/time rule.
     * Rule conflicts are solved by last-match policy.
     *
     * @author Martin Srom
     */
    public static class Rule
    {
        private RuleType type;

        private AbsoluteDateTime dateTimeFrom;

        private AbsoluteDateTime dateTimeTo;

        /**
         * Construct rule that performs it's effect for concrete date/time.
         *
         * @param type     Type of rule
         * @param dateTime Concrete date/time
         */
        public Rule(RuleType type, AbsoluteDateTime dateTime)
        {
            this.type = type;
            this.dateTimeFrom = dateTime;
        }

        /**
         * Construct rule that performs it's effect for interval of date/times.
         *
         * @param type         Type of rule
         * @param dateTimeFrom Start of date/time interval
         * @param dateTimeTo   End of date/time interval
         */
        public Rule(RuleType type, AbsoluteDateTime dateTimeFrom, AbsoluteDateTime dateTimeTo)
        {
            this.type = type;
            this.dateTimeFrom = dateTimeFrom;
            this.dateTimeTo = dateTimeTo;
        }

        /**
         * Get type of rule.
         *
         * @return type
         */
        public RuleType getType()
        {
            return type;
        }

        /**
         * Get rule single date/time.
         *
         * @return date/time
         */
        public AbsoluteDateTime getDateTime()
        {
            assert (dateTimeFrom != null && dateTimeTo == null)
                    : "Periodic date/time rule should have only single date/time set.";
            return dateTimeFrom;
        }

        /**
         * Get rule interval "from" date/time.
         *
         * @return date/time
         */
        public AbsoluteDateTime getDateTimeFrom()
        {
            assert (dateTimeFrom != null) : "Periodic date/time rule should have interval from set.";
            return dateTimeFrom;
        }

        /**
         * Get rule interval "to" date/time.
         *
         * @return date/time
         */
        public AbsoluteDateTime getDateTimeTo()
        {
            assert (dateTimeTo != null) : "Periodic date/time rule should have interval to set.";
            return dateTimeTo;
        }

        /**
         * Checks whether rule has interval set or single date/time.
         *
         * @return true if rule has interval set (pair of date/times),
         *         false otherwise
         */
        public boolean isInterval()
        {
            assert (dateTimeFrom != null) : "Periodic date/time rule should have set at least one date/time.";
            return dateTimeTo != null;
        }
    }
}
