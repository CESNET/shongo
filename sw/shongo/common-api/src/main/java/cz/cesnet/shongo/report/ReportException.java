package cz.cesnet.shongo.report;

/**
 * Represents a {@link Exception} for {@link AbstractReport}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class ReportException extends Exception
{
    /**
     * {@link AbstractReport}.
     */
    protected AbstractReport report;

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
     * @return {@link AbstractReport}
     */
    public AbstractReport getReport()
    {
        return report;
    }

    /**
     * @param report sets the {@link #report}
     */
    public void setReport(AbstractReport report)
    {
        this.report = report;
    }

    @Override
    public String getMessage()
    {
        return getReport().getMessage();
    }
}
