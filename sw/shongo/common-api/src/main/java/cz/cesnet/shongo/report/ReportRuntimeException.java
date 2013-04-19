package cz.cesnet.shongo.report;

/**
 * Represents a {@link RuntimeException} for {@link Report}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class ReportRuntimeException extends RuntimeException
{
    /**
     * {@link Report}.
     */
    protected Report report;

    /**
     * Constructor.
     */
    public ReportRuntimeException()
    {
    }

    /**
     * Constructor.
     *
     * @param throwable
     */
    public ReportRuntimeException(Throwable throwable)
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
