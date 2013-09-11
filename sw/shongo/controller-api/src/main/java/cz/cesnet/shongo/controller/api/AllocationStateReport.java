package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.report.Report;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

/**
 * {@link AbstractStateReport} for {@link AllocationState}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AllocationStateReport extends AbstractStateReport
{
    @Override
    public String toString(Locale locale)
    {
        /*StringBuilder stringBuilder = new StringBuilder();
        for (SchedulerReport report : reports) {
            if (stringBuilder.length() > 0) {
                stringBuilder.append("\n");
                stringBuilder.append("\n");
            }
            stringBuilder.append(report.getMessageRecursive(messageType));
        }
        return (stringBuilder.length() > 0 ? stringBuilder.toString() : null);*/
        throw new TodoImplementException();
    }

    /*public String getMessageRecursive(Report.MessageType messageType)
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

    public void getMessageRecursiveChildren(Report.MessageType messageType, Collection<SchedulerReport> childReports)
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
