package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.AbstractEntityReport;
import cz.cesnet.shongo.controller.AllocationStateReportMessages;
import cz.cesnet.shongo.report.Report;

import java.util.*;

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
                stringBuilder.append("\n");
                stringBuilder.append("\n");
            }
            String reportId = (String) report.get(ID);
            String message = AllocationStateReportMessages.getMessage(
                    reportId, getUserType(), Report.Language.fromLocale(locale), report);
            stringBuilder.append(message);
        }
        return (stringBuilder.length() > 0 ? stringBuilder.toString() : null);
    }

    /*public String getMessageRecursive(Report.UserType messageType)
    {
        // Get child reports
        List<SchedulerReport> childReports = new LinkedList<SchedulerReport>();
        getMessageRecursiveChildren(messageType, childReports);

        StringBuilder messageBuilder = new StringBuilder();
        String message = null;
        if (isVisible(messageType)) {
            // Append prefix
            message = getMessage(messageType, Report.Language.ENGLISH);
            messageBuilder.append("-");
            switch (getType()) {
                case ERROR:
                    messageBuilder.append("[ERROR] ");
                    break;
                default:
                    break;
            }

            // Append message
            if (childReports.size() > 0) {
                message = message.replace("\n", String.format("\n  |%" + (messageBuilder.length() - 3) + "s", ""));
            }
            else {
                message = message.replace("\n", String.format("\n%" + messageBuilder.length() + "s", ""));
            }
            messageBuilder.append(message);

            // Append child reports
            int childReportsCount = childReports.size();
            for (int index = 0; index < childReportsCount; index++) {
                String childReportString = childReports.get(index).getMessageRecursive(messageType);
                if (childReportString != null) {
                    messageBuilder.append("\n  |");
                    messageBuilder.append("\n  +-");
                    childReportString = childReportString.replace("\n",
                            (index < (childReportsCount - 1) ? "\n  | " : "\n    "));
                    messageBuilder.append(childReportString);
                }
            }
        }
        else {
            for (SchedulerReport childReport : childReports) {
                if (messageBuilder.length() > 0) {
                    messageBuilder.append("\n\n");
                }
                messageBuilder.append(childReport.getMessageRecursive(messageType));
            }

        }
        return (messageBuilder.length() > 0 ? messageBuilder.toString() : null);
    }

    public void getMessageRecursiveChildren(Report.UserType messageType, Collection<SchedulerReport> childReports)
    {
        for (SchedulerReport childReport : this.childReports) {
            if (childReport.isVisible(messageType)) {
                childReports.add(childReport);
            }
            else {
                childReport.getMessageRecursiveChildren(messageType, childReports);
            }
        }
    }*/
}
