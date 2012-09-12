package cz.cesnet.shongo.controller.scheduler.report;

import cz.cesnet.shongo.controller.report.Report;
import org.joda.time.Period;

import javax.persistence.Entity;
import javax.persistence.Transient;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link #getText()}
 */
@Entity
public class DurationLongerThanMaximumReport extends Report
{
    /**
     * Duration
     */
    private Period duration;

    private Period maximumDuration;

    /**
     * Constructor.
     */
    public DurationLongerThanMaximumReport()
    {
    }

    /**
     * Constructor.
     *
     * @param duration
     * @param maximumDuration
     */
    public DurationLongerThanMaximumReport(Period duration, Period maximumDuration)
    {
        this.duration = duration;
        this.maximumDuration = maximumDuration;
    }

    @Override
    @Transient
    public String getText()
    {
        return String.format("Duration '%s' is longer than maximum '%s'!",
                duration.toString(), maximumDuration.toString());
    }
}
