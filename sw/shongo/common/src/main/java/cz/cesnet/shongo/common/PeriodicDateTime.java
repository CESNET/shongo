package cz.cesnet.shongo.common;

import javax.persistence.*;
import java.util.*;

/**
 * Represents a Date/Time of events that takes place periodically.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class PeriodicDateTime extends DateTime
{
    /**
     * Date/time of the first periodic event.
     */
    private AbsoluteDateTime start;

    /**
     * Period of periodic events.
     */
    private Period period;

    /**
     * Ending date/time after which the periodic events are not considered.
     * The ending date/time can be nice e.g., 31.12.2012, and for periodic events
     * on every Thursday the last will take place on 29.12.2012.
     */
    private AbsoluteDateTime end;

    /**
     * List of rules for periodic date/time.
     */
    private List<Rule> rules = new ArrayList<Rule>();

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
     * @return {@link #start}
     */
    @Embedded
    public AbsoluteDateTime getStart()
    {
        return start;
    }

    /**
     * @param start sets the {@link #start}
     */
    public void setStart(AbsoluteDateTime start)
    {
        this.start = start;
    }

    /**
     * @return {@link #period}
     */
    @Column
    @Access(AccessType.FIELD)
    public Period getPeriod()
    {
        return period;
    }

    /**
     * @param period sets the {@link #period}
     */
    public void setPeriod(Period period)
    {
        this.period = period;
    }

    /**
     * @return {@link #end}
     */
    @Embedded
    public AbsoluteDateTime getEnd()
    {
        return end;
    }

    /**
     * @param end sets the {@link #end}
     */
    public void setEnd(AbsoluteDateTime end)
    {
        this.end = end;
    }

    /**
     * @return {@link #rules}
     */
    @OneToMany
    public List<Rule> getRules()
    {
        return Collections.unmodifiableList(rules);
    }

    /**
     * @param rules sets the {@link #rules}
     */
    private void setRules(List<Rule> rules)
    {
        this.rules = rules;
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
     * Enumerate all periodic Date/Time events to array of absolute Date/Times.
     *
     * @return array of absolute Date/Times
     */
    public final List<AbsoluteDateTime> enumerate()
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
    public List<AbsoluteDateTime> enumerate(AbsoluteDateTime from, AbsoluteDateTime to)
    {
        // Find all events in range from-to
        List<AbsoluteDateTime> dateTimeList = new ArrayList<AbsoluteDateTime>();
        if (this.start != null) {
            AbsoluteDateTime start = (AbsoluteDateTime) this.start.clone();
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
            if (type != RuleType.Enable && type != RuleType.Disable) {
                throw new IllegalStateException("Rule type should be enable or disable.");
            }
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
                    if (dateTime.equals(ruleDateTime)) {
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

        return dateTimeList;
    }

    @Override
    public AbsoluteDateTime getEarliest(AbsoluteDateTime referenceDateTime)
    {
        List<AbsoluteDateTime> dateTimes = enumerate();
        for (AbsoluteDateTime dateTime : dateTimes) {
            if (dateTime.before(referenceDateTime) == false) {
                return dateTime;
            }
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
        List<AbsoluteDateTime> dateTimes1 = enumerate();
        List<AbsoluteDateTime> dateTimes2 = periodicDateTime.enumerate();
        if (dateTimes1.size() != dateTimes2.size()) {
            return false;
        }
        for (int index = 0; index < dateTimes1.size(); index++) {
            if (dateTimes1.get(index).equals(dateTimes2.get(index)) == false) {
                return false;
            }
        }
        return true;
    }

    /**
     * Periodic date/time rule type.
     *
     * @author Martin Srom <martin.srom@cesnet.cz>
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
     * @author Martin Srom <martin.srom@cesnet.cz>
     */
    @Entity
    public static class Rule extends PersistentObject
    {
        /**
         * Type of rule.
         */
        private RuleType type;

        /**
         * Rule interval "from" date/time.
         */
        private AbsoluteDateTime dateTimeFrom;

        /**
         * Rule interval "to" date/time.
         */
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
         * @return {@link #type}
         */
        @Column
        @Access(AccessType.FIELD)
        public RuleType getType()
        {
            return type;
        }

        /**
         * @return single date/time for rule
         */
        @Transient
        public AbsoluteDateTime getDateTime()
        {
            if (dateTimeFrom == null || dateTimeTo != null) {
                throw new IllegalStateException("Periodic date/time rule should have only single date/time set.");
            }
            return dateTimeFrom;
        }

        /**
         * @return {@link #dateTimeFrom}
         */
        @Embedded
        @Access(AccessType.FIELD)
        public AbsoluteDateTime getDateTimeFrom()
        {
            if (dateTimeFrom == null) {
                throw new IllegalStateException("Periodic date/time rule should have interval from set.");
            }
            return dateTimeFrom;
        }

        /**
         * @return {@link #dateTimeTo}
         */
        @Embedded
        @Access(AccessType.FIELD)
        public AbsoluteDateTime getDateTimeTo()
        {
            if (dateTimeTo == null) {
                throw new IllegalStateException("Periodic date/time rule should have interval to set.");
            }
            return dateTimeTo;
        }

        /**
         * Checks whether rule has interval set or single date/time.
         *
         * @return true if rule has interval set (pair of date/times),
         *         false otherwise
         */
        @Transient
        public boolean isInterval()
        {
            if (dateTimeTo == null) {
                throw new IllegalStateException("Periodic date/time rule should have set at least one date/time.");
            }
            return dateTimeTo != null;
        }
    }

    @Override
    protected void fillDescriptionMap(Map<String, String> map)
    {
        super.fillDescriptionMap(map);

        map.put("start", start.toString());
        map.put("period", period.toString());
        if (end != null) {
            map.put("end", end.toString());
        }

        List<String> dateTimes = new ArrayList<String>();
        for (AbsoluteDateTime dateTime : enumerate()) {
            dateTimes.add(dateTime.toString());
        }
        addCollectionToMap(map, "enumerated", dateTimes);
    }
}
