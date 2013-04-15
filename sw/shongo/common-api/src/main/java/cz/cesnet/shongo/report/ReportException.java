package cz.cesnet.shongo.report;

/**
 * Represents a {@link Exception} for {@link Report}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class ReportException extends Exception
{
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
    public abstract Report getReport();

    @Override
    public String getMessage()
    {
        return getReport().getMessage();
    }
}
