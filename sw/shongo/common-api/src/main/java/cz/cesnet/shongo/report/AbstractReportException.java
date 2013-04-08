package cz.cesnet.shongo.report;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class AbstractReportException extends Exception
{
    /**
     * Constructor.
     */
    public AbstractReportException()
    {
    }

    /**
     * Constructor.
     *
     * @param throwable
     */
    public AbstractReportException(Throwable throwable)
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
