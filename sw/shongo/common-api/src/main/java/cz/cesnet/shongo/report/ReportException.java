package cz.cesnet.shongo.report;

/**
 * Represents a {@link Exception} for {@link Report}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class ReportException extends Exception
{
    /**
     * {@link Report}.
     */
    protected Report report;

    /**
     * Constructor.
     */
    public ReportException()
    {
    }

    /**
     * Constructor.
     *
     * @param throwable
     */
    public ReportException(Throwable throwable)
    {
        super(throwable);
    }

    /**
     * @return {@link Report}
     */
    public Report getReport()
    {
        return report;
    }

    /**
     * @param report sets the {@link #report}
     */
    public void setReport(Report report)
    {
        this.report = report;
    }

    @Override
    public String getMessage()
    {
        return getReport().getMessage();
    }
}
