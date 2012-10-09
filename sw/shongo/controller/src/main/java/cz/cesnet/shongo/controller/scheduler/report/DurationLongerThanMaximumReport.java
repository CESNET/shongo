package cz.cesnet.shongo.controller.scheduler.report;

import cz.cesnet.shongo.controller.report.Report;
import org.hibernate.annotations.Type;
import org.joda.time.Period;

import javax.persistence.*;

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

    /**
     * Maximum duration
     */
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
        this.setDuration(duration.normalizedStandard());
        this.setMaximumDuration(maximumDuration.normalizedStandard());
    }

    /**
     * @return {@link #duration}
     */
    @Column
    @Type(type = "Period")
    @Access(AccessType.FIELD)
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
     * @return {@link #maximumDuration}
     */
    @Column
    @Type(type = "Period")
    @Access(AccessType.FIELD)
    public Period getMaximumDuration()
    {
        return maximumDuration;
    }

    /**
     * @param maximumDuration sets the {@link #maximumDuration}
     */
    public void setMaximumDuration(Period maximumDuration)
    {
        this.maximumDuration = maximumDuration;
    }

    @Override
    @Transient
    public String getText()
    {
        return String.format("Duration '%s' is longer than maximum '%s'!", getDuration(), getMaximumDuration());
    }
}
