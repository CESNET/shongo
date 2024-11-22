package cz.cesnet.shongo.controller.booking.datetime;

import cz.cesnet.shongo.*;
import cz.cesnet.shongo.controller.api.PeriodicDateTimeSlot;
import cz.cesnet.shongo.hibernate.PersistentDateTime;
import cz.cesnet.shongo.hibernate.PersistentDateTimeZone;
import cz.cesnet.shongo.hibernate.PersistentPeriod;
import cz.cesnet.shongo.hibernate.PersistentReadablePartial;
import org.hibernate.annotations.*;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.Parameter;
import org.hibernate.usertype.UserTypeLegacyBridge;
import org.joda.time.*;

import jakarta.persistence.*;
import jakarta.persistence.AccessType;
import jakarta.persistence.Entity;
import java.util.*;

/**
 * Represents a date/time of events that takes place periodically.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class PeriodicDateTime extends SimplePersistentObject  implements Cloneable
{
    /**
     * Maximum number of enumerated date/times. If {@link #enumerate} exceeds that number
     * an exception is thrown.
     */
    public static final int MAX_ENUMERATED_COUNT = 1000;

    /**
     * Maximum number of enumerated date/times to display by {@link #toString()}.
     */
    public static final int MAX_PRINT_COUNT = 10;

    /**
     * Date and time of the first periodic event.
     */
    private DateTime start;

    /**
     * Timezone in which the periodicity should be computed (to proper handling of daylight saving time).
     */
    private DateTimeZone timeZone;

    /**
     * Period of periodic events.
     */
    private Period period;

    /**
     * Order of periodicity day when {@link cz.cesnet.shongo.controller.api.PeriodicDateTimeSlot.PeriodicityType.MonthPeriodicityType#SPECIFIC_DAY} period is set
     */
    protected Integer periodicityDayOrder;

    /**
     * Day of periodicity day when {@link cz.cesnet.shongo.controller.api.PeriodicDateTimeSlot.PeriodicityType.MonthPeriodicityType#SPECIFIC_DAY} period is set
     */
    protected PeriodicDateTimeSlot.DayOfWeek periodicityDayInMonth;

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
    public PeriodicDateTime()
    {
        this(null, null, null, null, null);
    }

    /**
     * Constructs periodical date/time events. The first event takes place at start
     * and each other take places in given period.
     *
     * @param start
     * @param period
     */
    public PeriodicDateTime(DateTime start, Period period)
    {
        this(start, period, null, null, null);
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
    public PeriodicDateTime(DateTime start, Period period, ReadablePartial end)
    {
        this(start, period, end, null, null);
    }

    /**
     * Constructs periodical date/time events. The first event takes place at start
     * and each other take places in given period. The last event takes place before
     * or equals to end.
     *
     * @param start
     * @param period
     * @param end
     * @param periodicityDayOrder
     * @param periodicityDayInMonth
     */
    public PeriodicDateTime(DateTime start, Period period, ReadablePartial end, Integer periodicityDayOrder, PeriodicDateTimeSlot.DayOfWeek periodicityDayInMonth)
    {
        if (period != null && PeriodicDateTimeSlot.PeriodicityType.MONTHLY.equals(PeriodicDateTimeSlot.PeriodicityType.fromPeriod(period))) {
            setPeriodicityDayOrder(periodicityDayOrder);
            setPeriodicityDayInMonth(periodicityDayInMonth);
        }
        setStart(start);
        setPeriod(period);
        setEnd(end);
    }

    /**
     * @return {@link #start}
     */
    @Column
    @Type(PersistentDateTime.class)
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
     * @return {@link #timeZone}
     */
    @Column(length = PersistentDateTimeZone.LENGTH)
    @Type(PersistentDateTimeZone.class)
    @Access(AccessType.FIELD)
    public DateTimeZone getTimeZone()
    {
        return timeZone;
    }

    /**
     * @param timeZone sets the {@link #timeZone}
     */
    public void setTimeZone(DateTimeZone timeZone)
    {
        if (timeZone != null && timeZone.equals(DateTimeZone.getDefault())) {
            timeZone = null;
        }
        this.timeZone = timeZone;
    }

    /**
     * @return {@link #period}
     */
    @Column(length = PersistentPeriod.LENGTH)
    @Type(PersistentPeriod.class)
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
    @Column(name = "ending", length = PersistentReadablePartial.LENGTH)
    @Type(PersistentReadablePartial.class)
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

    @Column(nullable = true)
    public Integer getPeriodicityDayOrder() {
        return periodicityDayOrder;
    }

    public void setPeriodicityDayOrder(Integer periodicityDayOrder) {
        this.periodicityDayOrder = periodicityDayOrder;
    }

    @Column(nullable = true)
    public PeriodicDateTimeSlot.DayOfWeek getPeriodicityDayInMonth() {
        return periodicityDayInMonth;
    }

    public void setPeriodicityDayInMonth(PeriodicDateTimeSlot.DayOfWeek periodicityDayInMonth) {
        this.periodicityDayInMonth = periodicityDayInMonth;
    }

    /**
     * @return {@link #rules}
     */
    @OneToMany
    @Cascade(CascadeType.ALL)
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
     * Add list of new rules of the given type for periodic date. Adds only valid dates within the slot interval.
     *
     * @param type            Type of rule
     * @param dateTimeList    List of concrete date
     */
    public void addAllRules(RuleType type, List<LocalDate> dateTimeList)
    {
        if (dateTimeList != null) {
            for (LocalDate dateTime : dateTimeList) {
                if (cz.cesnet.shongo.Temporal.dateFitsInterval(getStart(), getEnd(), dateTime)) {
                    addRule(type, dateTime);
                }
            }
        }
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
        return enumerate(null);
    }

    /**
     * Enumerate all periodic Date/Time events to array of absolute Date/Times. Do not exceed maximum count.
     *
     * @param maxCount
     * @return array of absolute Date/Times
     */
    public final List<DateTime> enumerate(int maxCount)
    {
        return enumerate(null, null, maxCount);
    }

    /**
     * Enumerate all periodic Date/Time events to array of absolute Date/Times.
     * Return only events that take place inside interval.
     *
     * @param interval
     * @return array of absolute Date/Times
     */
    public List<DateTime> enumerate(Interval interval)
    {
        if (interval == null) {
            return enumerate(null, null);
        }
        else {
            return enumerate(interval.getStart(), interval.getEnd());
        }
    }

    /**
     * Enumerate all periodic Date/Time events to array of absolute Date/Times.
     * Return only events that take place inside interval.
     *
     * @param intervalStart
     * @param intervalEnd
     * @return array of absolute Date/Times
     */
    public final List<DateTime> enumerate(DateTime intervalStart, DateTime intervalEnd)
    {
        List<DateTime> dateTimeList = enumerate(intervalStart, intervalEnd, MAX_ENUMERATED_COUNT);
        if (dateTimeList.size() >= MAX_ENUMERATED_COUNT) {
            throw new IllegalArgumentException("Cannot enumerate periodic date/time for interval '"
                    + (intervalStart != null ? intervalStart
                    .toString() : "null") + "'-'" + (intervalEnd != null ? intervalEnd.toString() : "null")
                    + "' because maximum number of date/times " + MAX_ENUMERATED_COUNT + " was reached!");
        }
        return dateTimeList;
    }

    /**
     * Default interval 'from' and 'to'.
     */
    private final DateTime DEFAULT_INTERVAL_FROM = new DateTime(0, 1, 1, 0, 0, 0);
    private final DateTime DEFAULT_INTERVAL_TO = new DateTime(0, 12, 31, 23, 59, 59);

    /**
     * Enumerate all periodic Date/Time events to array of absolute Date/Times.
     * Return only events that take place inside interval and not exceed maximum count.
     *
     * @param intervalStart
     * @param intervalTo
     * @param maxCount
     * @return array of absolute Date/Times
     */
    public final List<DateTime> enumerate(DateTime intervalStart, DateTime intervalTo, int maxCount)
    {
        DateTime start = this.start;
        if (this.timeZone != null) {
            start = start.withZone(this.timeZone);
        }

        // Find all events in range from-to
        List<DateTime> dateTimeList = new ArrayList<DateTime>();
        if (start != null && Period.ZERO.equals(this.period)) {
            dateTimeList.add(start);
        }
        else if (start != null) {
            while (end == null || !start.isAfter(this.end.toDateTime(start))) {
                if (intervalTo != null && start.isAfter(intervalTo)) {
                    break;
                }
                if (intervalStart == null || !intervalStart.isAfter(start)) {
                    dateTimeList.add(start);
                }
                if (periodicityDayOrder != null && periodicityDayInMonth != null) {
                    DateTime newStart = start.plusMonths(1).minusDays(start.getDayOfMonth() - 1);
                    DateTime monthEnd = newStart.plusMonths(1).minusDays(1);;
                    if (0 < periodicityDayOrder && periodicityDayOrder < 5) {
                        while (newStart.getDayOfWeek() != (periodicityDayInMonth.getDayIndex() == 1 ? 7 : periodicityDayInMonth.getDayIndex() - 1)) {
                            newStart = newStart.plusDays(1);
                        }
                        for (int i = 1; i < periodicityDayOrder; i++) {
                            if (!newStart.plusDays(7).isAfter(monthEnd)) {
                                newStart = newStart.plusDays(7);
                            }
                        }
                    }
                    else if (periodicityDayOrder == -1) {
                        newStart = monthEnd;
                        while (newStart.getDayOfWeek() != (periodicityDayInMonth.getDayIndex() == 1 ? 7 : periodicityDayInMonth.getDayIndex() - 1)) {
                            newStart = newStart.minusDays(1);
                        }
                    }
                    else {
                        throw new TodoImplementException();
                    }

                    start = newStart;
                }
                else {
                    start = start.plus(period);
                }

                if (dateTimeList.size() >= maxCount || period == null) {
                    break;
                }
            }
        }

        // Build set of indexes for disabled date/times from the list
        Set<Integer> disabledSet = new HashSet<Integer>();
        for (Rule rule : rules) {
            // Extra rule
            if (rule.getType() == RuleType.EXTRA) {
                DateTime ruleDateTime = rule.getDateTime().toDateTime(start);
                if ((intervalStart == null || !intervalStart.isAfter(ruleDateTime))
                        && (intervalTo == null || !intervalTo.isBefore(ruleDateTime))) {
                    dateTimeList.add(ruleDateTime);
                }
                if (dateTimeList.size() >= maxCount) {
                    break;
                }
                continue;
            }

            // Enable/disable rule
            RuleType type = rule.getType();
            if (type != RuleType.ENABLE && type != RuleType.DISABLE) {
                throw new RuntimeException("Rule type should be enable or disable.");
            }
            // Interval
            if (rule.isInterval()) {
                DateTime ruleFrom = rule.getDateTimeFrom().toDateTime(DEFAULT_INTERVAL_FROM);
                DateTime ruleTo = rule.getDateTimeTo().toDateTime(DEFAULT_INTERVAL_TO);
                for (int index = 0; index < dateTimeList.size(); index++) {
                    DateTime dateTime = dateTimeList.get(index);
                    if (dateTime.isBefore(ruleFrom) == false && dateTime.isAfter(ruleTo) == false) {
                        if (type == RuleType.ENABLE) {
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
                        if (type == RuleType.ENABLE) {
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

    /**
     * Get the earliest Date/Time since a given datetime (strict inequality).
     *
     * @param referenceDateTime the datetime since which to find the earliest occurrence
     * @return absolute Date/Time, or <code>null</code> if the datetime won't take place since referenceDateTime
     */
    public DateTime getEarliest(DateTime referenceDateTime)
    {
        List<DateTime> dateTimes = enumerate(referenceDateTime, null, 2);
        for (DateTime dateTime : dateTimes) {
            if (referenceDateTime == null || dateTime.isBefore(referenceDateTime) == false) {
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
        if (getId() != null) {
            return getId().equals(periodicDateTime.getId());
        }

        // TODO: think up how to do infinite equals
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

    @Override
    protected PeriodicDateTime clone() throws CloneNotSupportedException
    {
        PeriodicDateTime periodicDateTime = (PeriodicDateTime) super.clone();
        periodicDateTime.setIdNull();
        periodicDateTime.setStart(start);
        periodicDateTime.setPeriod(period);
        periodicDateTime.setEnd(end);
        periodicDateTime.setTimeZone(timeZone);
        periodicDateTime.setPeriodicityDayInMonth(periodicityDayInMonth);
        periodicDateTime.setPeriodicityDayOrder(periodicityDayOrder);
        rules = new ArrayList<>();
        for (Rule rule : rules) {
            periodicDateTime.addRule(rule.clone());
        }
        return periodicDateTime;
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
        EXTRA,

        /**
         * Represents a rule for enabling events by concrete date/time or by interval from - to.
         */
        ENABLE,

        /**
         * Represents a rule for disabling events by concrete date/time or by interval from - to.
         */
        DISABLE
    }

    /**
     * Periodic date/time rule.
     * Rule conflicts are solved by last-match policy.
     *
     * @author Martin Srom <martin.srom@cesnet.cz>
     */
    @Entity
    public static class Rule extends SimplePersistentObject implements Cloneable
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
         * Constructor.
         */
        private Rule()
        {
        }

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
        @Column(length = PersistentReadablePartial.LENGTH)
        @Type(PersistentReadablePartial.class)
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
        @Column(length = PersistentReadablePartial.LENGTH)
        @Type(PersistentReadablePartial.class)
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
         * false otherwise
         */
        @Transient
        public boolean isInterval()
        {
            if (dateTimeFrom == null) {
                throw new IllegalStateException("Periodic date/time rule should have set at least one date/time.");
            }
            return dateTimeTo != null;
        }

        @Override
        protected Rule clone() throws CloneNotSupportedException
        {
            Rule rule = (Rule) super.clone();
            rule.id = null;
            rule.type = type;
            rule.dateTimeFrom = dateTimeFrom;
            rule.dateTimeTo = dateTimeTo;
            return rule;
        }
    }
}
