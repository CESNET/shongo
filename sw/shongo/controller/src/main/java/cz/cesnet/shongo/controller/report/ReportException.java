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
        setReport(report);
    }

    /**
     * @return {@link #report}
     */
    public Report getReport()
    {
        return report;
    }

    /**
     * @return {@link #report}
     */
    public Report getTopReport()
    {
        Report report = this.report;
        while (report.hasParentReport()) {
            report = report.getParentReport();
        }
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
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(report.getReport());
        stringBuilder.append("\n");
        return stringBuilder.toString();
    }

    @Override
    public String toString()
    {
        return getMessage();
    }
}
