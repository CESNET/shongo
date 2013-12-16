package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.AbstractObjectReport;
import cz.cesnet.shongo.controller.ExecutionReportMessages;
import cz.cesnet.shongo.report.Report;
import cz.cesnet.shongo.util.DateTimeFormatter;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.Locale;
import java.util.Map;

/**
 * {@link cz.cesnet.shongo.api.AbstractObjectReport} for {@link ExecutableState}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ExecutionReport extends AbstractObjectReport
{
    public static final String DATE_TIME = "dateTime";

    /**
     * Constructor.
     */
    public ExecutionReport()
    {
        super(null);
    }

    /**
     * Constructor.
     *
     * @param userType sets the {@link #userType}
     */
    public ExecutionReport(Report.UserType userType)
    {
        super(userType);
    }

    @Override
    public String toString(Locale locale, DateTimeZone timeZone)
    {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.getInstance(DateTimeFormatter.Type.LONG).with(locale, timeZone);
        StringBuilder stringBuilder = new StringBuilder();
        int count = 0;
        for (Map<String, Object> report : reports) {
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
            String reportId = (String) report.get(ID);
            String message = ExecutionReportMessages.getMessage(
                    reportId, getUserType(), Report.Language.fromLocale(locale), timeZone, report);
            stringBuilder.append("[");
            stringBuilder.append(dateTimeFormatter.formatDateTime(new DateTime(report.get(DATE_TIME))));
            stringBuilder.append("] ");
            stringBuilder.append(message);
        }
        return (stringBuilder.length() > 0 ? stringBuilder.toString() : null);
    }
}
