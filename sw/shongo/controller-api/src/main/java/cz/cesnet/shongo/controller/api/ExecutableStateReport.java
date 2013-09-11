package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.Temporal;
import cz.cesnet.shongo.TodoImplementException;

import java.util.Locale;

/**
 * {@link AbstractStateReport} for {@link ExecutableState}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ExecutableStateReport extends AbstractStateReport
{
    @Override
    public String toString(Locale locale)
    {
        /*int count = 0;
        StringBuilder stringBuilder = new StringBuilder();
        for (ExecutableReport report : getCachedSortedReports()) {
            if (stringBuilder.length() > 0) {
                stringBuilder.append("\n");
                stringBuilder.append("\n");
            }
            if (++count > 5) {
                stringBuilder.append("... ");
                stringBuilder.append(reports.size() - count + 1);
                stringBuilder.append(" more");
                break;
            }
            String dateTime = Temporal.formatDateTime(report.getDateTime());
            stringBuilder.append("[");
            stringBuilder.append(dateTime);
            stringBuilder.append("] ");
            stringBuilder.append(report.getMessage(messageType));
        }
        return (stringBuilder.length() > 0 ? stringBuilder.toString() : null);*/
        throw new TodoImplementException();
    }
}
