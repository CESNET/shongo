package cz.cesnet.shongo.report;

/**
 * Object of class which implements {@link ReportableSimple} can customize it's description in
 * {@link cz.cesnet.shongo.report.AbstractReport#getMessage}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public interface ReportableSimple
{
    /**
     * @return string description of this object for {@link cz.cesnet.shongo.report.AbstractReport}
     */
    public String getReportDescription();
}
