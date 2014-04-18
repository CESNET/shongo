package cz.cesnet.shongo.report;

/**
 * Represents a {@link RuntimeException} for {@link AbstractReport}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class ReportRuntimeException extends RuntimeException
{
    /**
     * {@link AbstractReport}.
     */
    protected AbstractReport report;

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
