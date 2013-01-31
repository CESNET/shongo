package cz.cesnet.shongo.controller.executor.report;

import cz.cesnet.shongo.controller.executor.Endpoint;
import cz.cesnet.shongo.controller.executor.Executable;
import cz.cesnet.shongo.controller.report.Report;
import org.hibernate.annotations.Type;
import org.joda.time.DateTime;

import javax.persistence.*;

/**
 * Represents a {@link Report} for {@link Executable}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public abstract class ExecutableReport extends Report
{
    /**
     * Identification of source endpoint.
     */
    private DateTime dateTime;

    /**
     * Constructor.
     */
    public ExecutableReport()
    {
    }

    /**
     * Constructor.
     *
     * @param dateTime sets the {@link #dateTime}
     */
    public ExecutableReport(DateTime dateTime)
    {
        setDateTime(dateTime);
    }

    /**
     * @return {@link #dateTime}
     */
    @Column
    @Type(type = "DateTime")
    @Access(AccessType.PROPERTY)
    public DateTime getDateTime()
    {
        return dateTime;
    }

    /**
     * @param dateTime sets the {@link #dateTime}
     */
    public void setDateTime(DateTime dateTime)
    {
        this.dateTime = dateTime;
    }
}
