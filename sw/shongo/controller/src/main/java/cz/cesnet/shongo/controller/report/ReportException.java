package cz.cesnet.shongo.controller.report;

/**
 * Exception described by the {@link Report}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReportException extends Exception
{
    /**
     * {@link Report} describing exception.
     */
    private Report report;

    /**
     * Constructor.
     *
     * @param report sets the {@link #report}
     */
    public ReportException(Report report)
    {
        this.report = report;
    }

    /**
     * @return {@link #report}
     */
    public Report getReport()
    {
        return report;
    }

    @Override
    public String getMessage()
    {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(report.toString());
        stringBuilder.append("\n");
        return stringBuilder.toString();
    }

    @Override
    public String toString()
    {
        return getMessage();
    }
}
