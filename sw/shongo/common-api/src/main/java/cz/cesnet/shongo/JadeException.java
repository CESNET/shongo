package cz.cesnet.shongo;

import cz.cesnet.shongo.report.AbstractReportException;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class JadeException extends AbstractReportException
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
     * @return {@link cz.cesnet.shongo.report.Report}
     */
    public abstract JadeReport getReport();
}
