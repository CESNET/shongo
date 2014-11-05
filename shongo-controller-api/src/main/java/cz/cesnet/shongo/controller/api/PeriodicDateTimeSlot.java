package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.Converter;
import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.api.IdentifiedComplexType;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Period;
import org.joda.time.ReadablePartial;

/**
 * Represents a definition for periodic date/time slots.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class PeriodicDateTimeSlot extends IdentifiedComplexType
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
     * Ending date and/or time after which the periodic events are not considered.
     */
    private ReadablePartial end;

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

    public static final String START = "start";
    public static final String TIME_ZONE = "timeZone";
    public static final String DURATION = "duration";
    public static final String PERIOD = "period";
    public static final String END = "end";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(START, start);
        dataMap.set(TIME_ZONE, timeZone);
        dataMap.set(DURATION, duration);
        dataMap.set(PERIOD, period);
        dataMap.set(END, end);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        start = dataMap.getDateTimeRequired(START);
        timeZone = dataMap.getDateTimeZone(TIME_ZONE);
        duration = dataMap.getPeriod(DURATION);
        period = dataMap.getPeriodRequired(PERIOD);
        end = dataMap.getReadablePartial(END);
    }

    /**
     * Type of periodicity of the reservation request.
     */
    public static enum PeriodicityType
    {
        NONE,
        DAILY,
        WEEKLY;

        public Period toPeriod()
        {
            switch (this) {
                case DAILY:
                    return Period.days(1);
                case WEEKLY:
                    return Period.weeks(1);
                default:
                    throw new TodoImplementException(this);
            }
        }
    }
}
