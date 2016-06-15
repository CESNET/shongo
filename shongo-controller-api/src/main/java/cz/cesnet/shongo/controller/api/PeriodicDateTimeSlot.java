package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.Converter;
import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.api.IdentifiedComplexType;
import org.joda.time.*;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

/**
 * Represents a definition for periodic date/time slots.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class PeriodicDateTimeSlot extends IdentifiedComplexType implements Comparable<PeriodicDateTimeSlot>
{
    /**
     * Starting date/time for the first date/time slot.
     */
    private DateTime start;

    /**
     * Timezone in which the periodicity should be computed (to proper handling of daylight saving time).
     */
    private DateTimeZone timeZone;

    /**
     * Duration of each date/time slots.
     */
    private Period duration;

    /**
     * Period for multiple date/time slots.
     */
    private Period period;

    /**
     * Type of periodicity with {@link cz.cesnet.shongo.controller.api.PeriodicDateTimeSlot.PeriodicityType#MONTHLY} period
     */
    private PeriodicityType.MonthPeriodicityType monthPeriodicityType;

    protected Integer periodicityDayOrder;

    protected PeriodicDateTimeSlot.DayOfWeek periodicityDayInMonth;

    /**
     * Ending date and/or time after which the periodic events are not considered.
     */
    private ReadablePartial end;

    /**
     * Collection of dates excluded from given time slots
     */
    protected List<LocalDate> excludeDates = new LinkedList<>();

    /**
     * Constructor.
     */
    public PeriodicDateTimeSlot()
    {
    }

    /**
     * Constructor.
     *
     * @param start    sets the {@link #start}
     * @param duration sets the {@link #duration}
     * @param period   sets the {@link #period}
     */
    public PeriodicDateTimeSlot(DateTime start, Period duration, Period period)
    {
        setStart(start);
        setTimeZone(start.getZone());
        setPeriod(period);
        setDuration(duration);
    }

    /**
     * Constructor.
     *
     * @param start  sets the {@link #start}
     * @param period sets the {@link #period}
     * @param end    sets the {@link #end}
     */
    public PeriodicDateTimeSlot(DateTime start, Period duration, Period period, ReadablePartial end)
    {
        setStart(start);
        setTimeZone(start.getZone());
        setDuration(duration);
        setPeriod(period);
        setEnd(end);
    }

    /**
     * Constructor.
     *
     * @param start    sets the {@link #start}
     * @param duration sets the {@link #duration}
     * @param period   sets the {@link #period}
     */
    public PeriodicDateTimeSlot(String start, String duration, String period)
    {
        setStart(DateTime.parse(start));
        setDuration(Period.parse(duration));
        setPeriod(Period.parse(period));
    }

    /**
     * Constructor.
     *
     * @param start    sets the {@link #start}
     * @param duration sets the {@link #duration}
     * @param period   sets the {@link #period}
     * @param end      sets the {@link #end}
     */
    public PeriodicDateTimeSlot(String start, String duration, String period, String end)
    {
        setStart(DateTime.parse(start));
        setDuration(Period.parse(duration));
        setPeriod(Period.parse(period));
        setEnd(Converter.convertStringToReadablePartial(end));
    }

    /**
     * @return {@link #start}
     */
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
    public DateTimeZone getTimeZone()
    {
        return timeZone;
    }

    /**
     * @param timeZone sets the {@link #timeZone}
     */
    public void setTimeZone(DateTimeZone timeZone)
    {
        this.timeZone = timeZone;
    }

    /**
     * @return {@link #duration}
     */
    public Period getDuration()
    {
        return duration;
    }

    /**
     * @param duration sets the {@link #duration}
     */
    public void setDuration(Period duration)
    {
        this.duration = duration;
    }

    public PeriodicityType.MonthPeriodicityType getMonthPeriodicityType() {
        return monthPeriodicityType;
    }

    public void setMonthPeriodicityType(PeriodicityType.MonthPeriodicityType monthPeriodicityType) {
        this.monthPeriodicityType = monthPeriodicityType;
    }

    public Integer getPeriodicityDayOrder() {
        return periodicityDayOrder;
    }

    public void setPeriodicityDayOrder(Integer periodicityDayOrder) {
        this.periodicityDayOrder = periodicityDayOrder;
    }

    public DayOfWeek getPeriodicityDayInMonth() {
        return periodicityDayInMonth;
    }

    public void setPeriodicityDayInMonth(DayOfWeek periodicityDayInMonth) {
        this.periodicityDayInMonth = periodicityDayInMonth;
    }

    /**
     * @return {@link #period}
     */
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
     * @return {@link #EXCLUDE_DATES}
     */
    public List<LocalDate> getExcludeDates()
    {
        return excludeDates;
    }

    /**
     * @param excludeDates sets the {@link #EXCLUDE_DATES}
     */
    public void setExcludeDates(List<LocalDate> excludeDates)
    {
        this.excludeDates = excludeDates;
    }

    public void addAllExcludeDates(List<LocalDate> excludeDates)
    {
        this.excludeDates.addAll(excludeDates);
    }

    /**
     * Add new date to the {@link #EXCLUDE_DATES}.
     *
     * @param date
     */
    public void addExcludeDate(LocalDate date)
    {
        this.excludeDates.add(date);
    }

    /**
     * Add new date to the {@link #EXCLUDE_DATES}.
     *
     * @param date
     */
    public void addExcludeDate(ReadablePartial date)
    {
        this.excludeDates.add(new LocalDate(date));
    }

    public static final String START = "start";
    public static final String TIME_ZONE = "timeZone";
    public static final String DURATION = "duration";
    public static final String PERIOD = "period";
    public static final String END = "end";
    public static final String MONTH_PERIODICITY_TYPE = "monthPeriodicityType";
    public static final String PERIODICITY_DAY_ORDER = "periodicityDayOrder";
    public static final String PERIODICITY_DAY_IN_MONTH = "periodicityDayInMonth";
    public static final String EXCLUDE_DATES = "excludeDates";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(START, start);
        dataMap.set(TIME_ZONE, timeZone);
        dataMap.set(DURATION, duration);
        dataMap.set(PERIOD, period);
        dataMap.set(END, end);
        dataMap.set(MONTH_PERIODICITY_TYPE, monthPeriodicityType);
        dataMap.set(PERIODICITY_DAY_ORDER, periodicityDayOrder);
        dataMap.set(PERIODICITY_DAY_IN_MONTH, periodicityDayInMonth);
        dataMap.set(EXCLUDE_DATES, excludeDates);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        start = dataMap.getDateTimeRequired(START);
        timeZone = dataMap.getDateTimeZone(TIME_ZONE);
        duration = dataMap.getPeriod(DURATION);
        period = dataMap.getPeriod(PERIOD);
        end = dataMap.getReadablePartial(END);
        monthPeriodicityType = dataMap.getEnum(MONTH_PERIODICITY_TYPE, PeriodicityType.MonthPeriodicityType.class);
        periodicityDayOrder = dataMap.getInteger(PERIODICITY_DAY_ORDER);
        periodicityDayInMonth = dataMap.getEnum(PERIODICITY_DAY_IN_MONTH, DayOfWeek.class);
        excludeDates = dataMap.getList(EXCLUDE_DATES, LocalDate.class);
    }

    @Override
    public int compareTo(PeriodicDateTimeSlot o) {
        return getStart().compareTo(o.getStart());
    }

    /**
     * Type of periodicity of the reservation request.
     */
    public static enum PeriodicityType
    {
        NONE,
        DAILY,
        WEEKLY,
        MONTHLY;

        public Period toPeriod()
        {
            return toPeriod(1);
        }

        public Period toPeriod(int cycle)
        {
            if (!DAILY.equals(this) && cycle < 1) {
                throw new IllegalArgumentException("Cycle of the period must be positive.");
            }

            switch (this) {
                case NONE:
                    return Period.ZERO;
                case DAILY:
                    return Period.days(1);
                case WEEKLY:
                    return Period.weeks(cycle);
                case MONTHLY:
                    return Period.months(cycle);
                default:
                    throw new TodoImplementException(this);
            }
        }

        public static PeriodicityType fromPeriod(Period period)
        {
            if (period == null) {
                throw new IllegalArgumentException("Period cannot be null");
            }
            if (period.getMillis() != 0  || period.getSeconds() != 0 || period.getMinutes() != 0
                    || period.getHours() != 0 || period.getYears() != 0) {
                throw new IllegalArgumentException("Period does not support millis, seconds, minutes, hours or years.");
            }
            int days = period.getDays();
            int weeks = period.getWeeks();
            int months = period.getMonths();
            if (days != 0 && weeks == 0 && months == 0) {
                return DAILY;
            }
            else if (days == 0 && weeks != 0 && months == 0) {
                return WEEKLY;
            }
            else if (days == 0 && weeks == 0 && months != 0) {
                return MONTHLY;
            }
            else if (days == 0 && weeks == 0 && months == 0) {
                return NONE;
            }
            else {
                throw new IllegalArgumentException("Period must have set just one type of periodicity.");
            }
        }

        public static int getPeriodCycle(Period period)
        {
            PeriodicityType type = fromPeriod(period);
            switch (type) {
                case DAILY:
                    return period.getDays();
                case WEEKLY:
                    return period.getWeeks();
                case MONTHLY:
                    return period.getMonths();
                case NONE:
                    return 0;
                default:
                    throw new TodoImplementException(type);
            }
        }
        public static enum MonthPeriodicityType
        {
            STANDARD, SPECIFIC_DAY;
        }
    }

    public enum DayOfWeek
    {
        MONDAY(Calendar.MONDAY),
        TUESDAY(Calendar.TUESDAY),
        WEDNESDAY(Calendar.WEDNESDAY),
        THURSDAY(Calendar.THURSDAY),
        FRIDAY(Calendar.FRIDAY),
        SATURDAY(Calendar.SATURDAY),
        SUNDAY(Calendar.SUNDAY);

        private int dayIndex;
        DayOfWeek(int dayIndex) {
            this.dayIndex = dayIndex;
        }

        public int getDayIndex() {
            return dayIndex;
        }

        public void setDayIndex(int dayIndex) {
            this.dayIndex = dayIndex;
        }

        public static DayOfWeek fromDayIndex(int dayIndex)
        {
            switch (dayIndex) {
                case Calendar.SUNDAY:
                    return SUNDAY;
                case Calendar.MONDAY:
                    return MONDAY;
                case Calendar.TUESDAY:
                    return TUESDAY;
                case Calendar.WEDNESDAY:
                    return WEDNESDAY;
                case Calendar.THURSDAY:
                    return THURSDAY;
                case Calendar.FRIDAY:
                    return FRIDAY;
                case Calendar.SATURDAY:
                    return SATURDAY;
                default:
                    throw new IllegalArgumentException("Illegal day index.");
            }
        }
    }
}
