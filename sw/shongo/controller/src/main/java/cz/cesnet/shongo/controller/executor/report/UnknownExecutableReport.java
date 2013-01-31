package cz.cesnet.shongo.controller.executor.report;

import cz.cesnet.shongo.controller.report.Report;
import org.joda.time.DateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Transient;

/**
 * Represents a {@link cz.cesnet.shongo.controller.report.Report} for {@link cz.cesnet.shongo.controller.executor.Executable}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class UnknownExecutableReport extends ExecutableReport
{
    /**
     * Message describing the unknown report.
     */
    private String message;

    /**
     * Constructor.
     */
    public UnknownExecutableReport()
    {
    }

    /**
     * Constructor.
     *
     * @param dateTime sets the {@link #dateTime}
     */
    public UnknownExecutableReport(DateTime dateTime, String message)
    {
        setDateTime(dateTime);
        setMessage(message);
    }

    /**
     * @return {@link #message}
     */
    @Column
    public String getMessage()
    {
        return message;
    }

    /**
     * @param message sets the {@link #message}
     */
    public void setMessage(String message)
    {
        this.message = message;
    }

    @Override
    @Transient
    public String getText()
    {
        return String.format("Unknown: %s", message);
    }
}
