package cz.cesnet.shongo.report;

import java.util.Map;

/**
 * Object of class which implements {@link ReportableComplex} can customize it's description in
 * {@link cz.cesnet.shongo.report.AbstractReport#getMessage}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public interface ReportableComplex
{
    /**
     * @return string description of this object for {@link cz.cesnet.shongo.report.AbstractReport}
     */
    public Map<String, Object> getReportDescription();
}
