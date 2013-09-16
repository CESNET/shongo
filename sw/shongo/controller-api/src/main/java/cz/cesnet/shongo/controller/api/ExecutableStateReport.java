package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.AbstractEntityReport;
import cz.cesnet.shongo.controller.ExecutableStateReportMessages;
import cz.cesnet.shongo.report.Report;
import cz.cesnet.shongo.util.DateTimeFormatter;
import org.joda.time.DateTime;

import java.util.Locale;
import java.util.Map;

/**
 * {@link cz.cesnet.shongo.api.AbstractEntityReport} for {@link ExecutableState}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ExecutableStateReport extends AbstractEntityReport
{
    public static final String DATE_TIME = "dateTime";

    /**
     * Constructor.
     */
    public ExecutableStateReport()
    {
        super(null);
    }

    /**
     * Constructor.
     *
     * @param userType sets the {@link #userType}
     */
    public ExecutableStateReport(Report.UserType userType)
    {
        super(userType);
    }

    @Override
    public String toString(Locale locale)
    {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.getInstance(DateTimeFormatter.Type.LONG).with(locale);
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
            String message = ExecutableStateReportMessages.getMessage(
                    reportId, getUserType(), Report.Language.fromLocale(locale), report);
            stringBuilder.append("[");
            stringBuilder.append(dateTimeFormatter.formatDateTime(new DateTime(report.get(DATE_TIME))));
            stringBuilder.append("] ");
            stringBuilder.append(message);
        }
        return (stringBuilder.length() > 0 ? stringBuilder.toString() : null);
    }
}
