package cz.cesnet.shongo;

import cz.cesnet.shongo.report.ReportException;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class DefaultJadeException extends JadeException
{
    private JadeReport jadeReport;

    /**
     * Constructor.
     */
    public DefaultJadeException(JadeReport jadeReport)
    {
        this.jadeReport = jadeReport;
    }

    /**
     * Constructor.
     *
     * @param throwable
     */
    public DefaultJadeException(JadeReport jadeReport, Throwable throwable)
    {
        super(throwable);
        this.jadeReport = jadeReport;
    }

    /**
     * @return {@link JadeReport}
     */
    public JadeReport getReport()
    {
        return jadeReport;
    }
}
