package cz.cesnet.shongo;

import cz.cesnet.shongo.report.ReportException;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class JadeException extends ReportException
{
    /**
     * Constructor.
     */
    public JadeException()
    {
    }

    /**
     * Constructor.
     *
     * @param throwable
     */
    public JadeException(Throwable throwable)
    {
        super(throwable);
    }

    /**
     * @return {@link cz.cesnet.shongo.report.AbstractReport}
     */
    public abstract JadeReport getReport();
}
