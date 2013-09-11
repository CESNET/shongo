package cz.cesnet.shongo.report;

/**
 * Object of class which implements {@link Reportable} can customize it's description in
 * {@link AbstractReport#getMessage}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public interface Reportable
{
    /**
     * @return string description of this object for {@link AbstractReport}
     */
    public String getReportDescription(AbstractReport.MessageType messageType);
}
