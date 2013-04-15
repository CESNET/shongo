package cz.cesnet.shongo.report;

/**
 * Represents a {@link RuntimeException} for {@link Report}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class ReportRuntimeException extends RuntimeException
{
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
     * @return {@link cz.cesnet.shongo.report.Report}
     */
    public abstract Report getReport();

    @Override
    public String getMessage()
    {
        return getReport().getMessage();
    }
}
