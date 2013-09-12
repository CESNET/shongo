package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.AbstractEntityReport;
import cz.cesnet.shongo.controller.AllocationStateReportMessages;
import cz.cesnet.shongo.report.Report;

import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

/**
 * {@link cz.cesnet.shongo.api.AbstractEntityReport} for {@link AllocationState}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AllocationStateReport extends AbstractEntityReport
{
    /**
     * Constructor.
     */
    public AllocationStateReport()
    {
        super(null);
    }

    /**
     * Constructor.
     *
     * @param userType sets the {@link #userType}
     */
    public AllocationStateReport(Report.UserType userType)
    {
        super(userType);
    }

    @Override
    public String toString(Locale locale)
    {
        StringBuilder stringBuilder = new StringBuilder();
        for (Map<String, Object> report : reports) {
            if (stringBuilder.length() > 0) {
                stringBuilder.append("\n\n");
            }
            stringBuilder.append(getReportMessage(report, locale));
        }
        return (stringBuilder.length() > 0 ? stringBuilder.toString() : null);
    }

    public String getReportMessage(Map<String, Object> report, Locale locale)
    {
        StringBuilder messageBuilder = new StringBuilder();

        // Append prefix
        String reportId = (String) report.get(ID);
        String message = AllocationStateReportMessages.getMessage(
                reportId, getUserType(), Report.Language.fromLocale(locale), report);
        messageBuilder.append("-");
        Report.Type reportType = getReportType(report);
        switch (reportType) {
            case ERROR:
                messageBuilder.append("[ERROR] ");
                break;
            default:
                break;
        }

        Collection<Map<String, Object>> childReports = getReportChildren(report);

        // Append message
        if (childReports.size() > 0) {
            message = message.replace("\n", String.format("\n  |%" + (messageBuilder.length() - 3) + "s", ""));
        }
        else {
            message = message.replace("\n", String.format("\n%" + messageBuilder.length() + "s", ""));
        }
        messageBuilder.append(message);

        // Append child reports
        for (Iterator<Map<String, Object>> iterator = childReports.iterator(); iterator.hasNext(); ) {
            Map<String, Object> childReport = iterator.next();
            String childReportString = getReportMessage(childReport, locale);
            if (childReportString != null) {
                messageBuilder.append("\n  |");
                messageBuilder.append("\n  +-");
                childReportString = childReportString.replace("\n", (iterator.hasNext() ? "\n  | " : "\n    "));
                messageBuilder.append(childReportString);
            }
        }

        return messageBuilder.toString();
    }
}
