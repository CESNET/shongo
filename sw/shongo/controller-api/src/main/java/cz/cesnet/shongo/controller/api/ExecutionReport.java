package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.JadeReportSet;
import cz.cesnet.shongo.api.AbstractObjectReport;
import cz.cesnet.shongo.controller.ExecutionReportMessages;
import cz.cesnet.shongo.controller.RecordingUnavailableException;
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
        DateTimeFormatter dateTimeFormatter =
                DateTimeFormatter.getInstance(DateTimeFormatter.Type.LONG).with(locale, timeZone);
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

    /**
     * @return {@link UserError} detected from this {@link ExecutionReport}
     */
    public UserError toUserError()
    {
        Map<String, Object> report = getLastReport();
        if (report != null) {
            String identifier = (String) report.get(ID);
            if (identifier.equals(ExecutionReportMessages.RECORDING_UNAVAILABLE)) {
                return new RecordingUnavailable();
            }
            else if (identifier.equals(ExecutionReportMessages.ROOM_NOT_STARTED)) {
                String roomName = (String) report.get("roomName");
                return new RoomNotStarted(roomName);
            }
        }
        return null;
    }

    /**
     * Represents an execution error which can be detected from {@link ExecutionReport}.
     */
    public static class UserError
    {

    }

    /**
     * Command threw {@link RecordingUnavailableException}.
     *
     * @see RecordingUnavailableException
     */
    public static class RecordingUnavailable extends UserError
    {
        public RecordingUnavailable()
        {
        }
    }

    /**
     * Cannot modify room {@link #roomName}, because it has not been started yet..
     */
    public static class RoomNotStarted extends UserError
    {
        private final String roomName;

        public RoomNotStarted(String roomName)
        {
            this.roomName = roomName;
        }

        public String getRoomName()
        {
            return roomName;
        }
    }
}
