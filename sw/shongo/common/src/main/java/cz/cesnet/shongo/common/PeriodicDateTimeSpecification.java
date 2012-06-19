package cz.cesnet.shongo.common;

import org.hibernate.annotations.Type;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.ReadablePartial;

import javax.persistence.*;
import java.util.*;

/**
 * Represents a date/time of events that takes place periodically.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class PeriodicDateTimeSpecification extends DateTimeSpecification
{
    /**
     * Date and time of the first periodic event.
     */
    private DateTime start;

    /**
     * Period of periodic events.
     */
    private Period period;

    /**
     * Ending date and/or time after which the periodic events are not considered.
     */
    private ReadablePartial end;

    /**
     * List of rules for periodic date/time.
     */
    private List<Rule> rules = new ArrayList<Rule>();

    /**
     * Constructs empty periodical date/time events.
     */
    public PeriodicDateTimeSpecification()
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
    public PeriodicDateTimeSpecification(DateTime start, Period period)
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
     * @param end
     */
    public PeriodicDateTimeSpecification(DateTime start, Period period, ReadablePartial end)
    {
        setStart(start);
        setPeriod(period);
        setEnd(end);
    }

    /**
     * @return {@link #start}
     */
    @Column
    @Type(type = "DateTime")
    public DateTime getStart()
    {
        return start;
    }

    /**
     * @param start sets the {@link #start}
     */
    public void setStart(DateTime start)
    {
        this.start = start;
    }

    /**
     * @return {@link #period}
     */
    @Column
    @Type(type = "Period")
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
    @Column(name = "ending")
    @Type(type = "ReadablePartial")
    public ReadablePartial getEnd()
    {
        return end;
    }

    /**
     * @param end sets the {@link #end}
     */
    public void setEnd(ReadablePartial end)
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
    public void addRule(RuleType type, ReadablePartial dateTime)
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
    public void addRule(RuleType type, ReadablePartial dateTimeFrom, ReadablePartial dateTimeTo)
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
    public final List<DateTime> enumerate()
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
    public List<DateTime> enumerate(DateTime from, DateTime to)
    {
        DateTime start = this.start;
        DateTime end = (this.end != null ? this.end.toDateTime(start) : null);

        // Find all events in range from-to
        List<DateTime> dateTimeList = new ArrayList<DateTime>();
        while (start != null && start.isAfter(end) == false) {
            if (to != null && start.isAfter(to)) {
                break;
            }
            if (from == null || start.isBefore(from) == false) {
                dateTimeList.add(start);
            }
            start = start.plus(period);
        }

        DateTime defaultIntervalFrom = new DateTime(0, 1, 1, 0, 0, 0);
        DateTime defaultIntervalTo = new DateTime(0, 1, 31, 23, 59, 59);

        // Build set of indexes for disabled date/times from the list
        Set<Integer> disabledSet = new HashSet<Integer>();
        for (Rule rule : rules) {
            // Extra rule
            if (rule.getType() == RuleType.Extra) {
                DateTime ruleDateTime = rule.getDateTime().toDateTime(start);
                if ((from != null && ruleDateTime.isBefore(from)) || (to != null && ruleDateTime.isAfter(to))) {
                    continue;
                }
                dateTimeList.add(ruleDateTime);
                continue;
            }

            // Enable/disable rule
            RuleType type = rule.getType();
            if (type != RuleType.Enable && type != RuleType.Disable) {
                throw new IllegalStateException("Rule type should be enable or disable.");
            }
            // Interval
            if (rule.isInterval()) {
                DateTime ruleFrom = rule.getDateTimeFrom().toDateTime(defaultIntervalFrom);
                DateTime ruleTo = rule.getDateTimeTo().toDateTime(defaultIntervalTo);
                for (int index = 0; index < dateTimeList.size(); index++) {
                    DateTime dateTime = dateTimeList.get(index);
                    if (dateTime.isBefore(ruleFrom) == false && dateTime.isAfter(ruleTo) == false) {
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
                ReadablePartial ruleDateTime = rule.getDateTime();
                for (int index = 0; index < dateTimeList.size(); index++) {
                    DateTime dateTime = dateTimeList.get(index);
                    if (dateTime.equals(ruleDateTime.toDateTime(dateTime))) {
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
    public DateTime getEarliest(DateTime referenceDateTime)
    {
        List<DateTime> dateTimes = enumerate();
        for (DateTime dateTime : dateTimes) {
            if (dateTime.isBefore(referenceDateTime) == false) {
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

        PeriodicDateTimeSpecification periodicDateTime = (PeriodicDateTimeSpecification) object;
        List<DateTime> dateTimes1 = enumerate();
        List<DateTime> dateTimes2 = periodicDateTime.enumerate();
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
        private ReadablePartial dateTimeFrom;

        /**
         * Rule interval "to" date/time.
         */
        private ReadablePartial dateTimeTo;

        /**
         * Construct rule that performs it's effect for concrete date/time.
         *
         * @param type     Type of rule
         * @param dateTime Concrete date/time
         */
        public Rule(RuleType type, ReadablePartial dateTime)
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
        public Rule(RuleType type, ReadablePartial dateTimeFrom, ReadablePartial dateTimeTo)
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
        public ReadablePartial getDateTime()
        {
            if (dateTimeFrom == null || dateTimeTo != null) {
                throw new IllegalStateException("Periodic date/time rule should have only single date/time set.");
            }
            return dateTimeFrom;
        }

        /**
         * @return {@link #dateTimeFrom}
         */
        @Column
        @Type(type = "ReadablePartial")
        @Access(AccessType.FIELD)
        public ReadablePartial getDateTimeFrom()
        {
            if (dateTimeFrom == null) {
                throw new IllegalStateException("Periodic date/time rule should have interval from set.");
            }
            return dateTimeFrom;
        }

        /**
         * @return {@link #dateTimeTo}
         */
        @Column
        @Type(type = "ReadablePartial")
        @Access(AccessType.FIELD)
        public ReadablePartial getDateTimeTo()
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
        for (DateTime dateTime : enumerate()) {
            dateTimes.add(dateTime.toString());
        }
        addCollectionToMap(map, "enumerated", dateTimes);
    }
}
