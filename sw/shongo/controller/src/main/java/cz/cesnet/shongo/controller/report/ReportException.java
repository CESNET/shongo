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
     * @param report sets the {@link #report}
     */
    public void setReport(Report report)
    {
        if (report.getParentReport() != null) {
            throw new IllegalArgumentException("Only top parents reports should be thrown.");
        }
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
