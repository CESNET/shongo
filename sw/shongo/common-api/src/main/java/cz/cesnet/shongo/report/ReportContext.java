package cz.cesnet.shongo.report;

/**
 * Represents an context in which a {@link Report} was created.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public interface ReportContext
{
    /**
     * @return name of the {@link ReportContext}
     */
    public String getReportName();

    /**
     * @return detailed description of the {@link ReportContext}
     */
    public String getReportDetail();
}
