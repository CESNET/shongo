package cz.cesnet.shongo.report;

/**
 * Object of class which implements {@link Reportable} can customize it's description in
 * {@link cz.cesnet.shongo.report.Report#getMessage}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public interface Reportable
{
    /**
     * @return string description of this object for {@link Report}
     */
    public String getReportDescription(Report.MessageType messageType);
}
