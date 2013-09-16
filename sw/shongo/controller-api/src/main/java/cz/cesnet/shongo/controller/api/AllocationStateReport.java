package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.api.AbstractEntityReport;
import cz.cesnet.shongo.api.Converter;
import cz.cesnet.shongo.controller.AllocationStateReportMessages;
import cz.cesnet.shongo.report.Report;
import cz.cesnet.shongo.util.DateTimeFormatter;
import cz.cesnet.shongo.util.MessageSource;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;

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
                stringBuilder.append("\n\n");
            }
            stringBuilder.append(getReportMessage(report, locale));
        }
        return (stringBuilder.length() > 0 ? stringBuilder.toString() : null);
    }

    /**
     * @return {@link cz.cesnet.shongo.controller.api.AllocationStateReport.UserError} detected from this {@link AllocationStateReport}
     */
    public UserError toUserError()
    {
        UserError userError = findAllocationError(reports, new Stack<Map<String, Object>>());
        if (userError == null) {
            userError = new UserError();
        }
        return userError;
    }

    /**
     * Represents an allocation error which can be detected from {@link AllocationStateReport} and which can be
     * formatted to user in a user-friendly way.
     */
    public static class UserError
    {
        /**
         * To be used for {@link cz.cesnet.shongo.controller.api.AllocationStateReport.UserError} messages.
         */
        protected static final MessageSource MESSAGE_SOURCE = new MessageSource("allocation-error");

        /**
         * To be used for formatting date/times.
         */
        protected static DateTimeFormatter DATE_TIME_FORMATTER =
                DateTimeFormatter.getInstance(DateTimeFormatter.Type.SHORT);

        /**
         * @param locale
         * @param timeZone
         * @return message for this {@link cz.cesnet.shongo.controller.api.AllocationStateReport.UserError} and for given {@code locale} and {@code timeZone}
         */
        public String getMessage(Locale locale, DateTimeZone timeZone)
        {
            return MESSAGE_SOURCE.getMessage("unknown", locale);
        }

        /**
         * @param locale
         * @return message for this {@link cz.cesnet.shongo.controller.api.AllocationStateReport.UserError} and for given {@code locale}
         */
        public final String getMessage(Locale locale)
        {
            return getMessage(locale, DateTimeZone.getDefault());
        }

        /**
         * @return message for this {@link cz.cesnet.shongo.controller.api.AllocationStateReport.UserError}
         */
        public final String getMessage()
        {
            return getMessage(Locale.getDefault(), DateTimeZone.getDefault());
        }

        public boolean isUnknown()
        {
            return getClass().equals(UserError.class);
        }
    }

    /**
     * @param report
     * @param locale
     * @return string message for given {@code report} and {@code locale}
     */
    private String getReportMessage(Map<String, Object> report, Locale locale)
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

    /**
     * @param reports
     * @param parentReports
     * @return {@link cz.cesnet.shongo.controller.api.AllocationStateReport.UserError} for given {@code reports} and {@code parentReports}
     */
    private UserError findAllocationError(Collection<Map<String, Object>> reports, Stack<Map<String, Object>> parentReports)
    {
        for (Map<String, Object> report : reports) {
            String identifier = (String) report.get(ID);
            if (parentReports.size() == 0) {
                if (identifier.equals(AllocationStateReportMessages.RESERVATION_REQUEST_NOT_USABLE)) {
                    return new ReusementInvalidSlot(
                            Converter.convertToString(report.get("reservationRequest")),
                            Converter.convertToInterval(report.get("interval")));
                }
                else if (identifier.equals(AllocationStateReportMessages.RESERVATION_ALREADY_USED)) {
                    return new ReusementAlreadyUsed(
                            Converter.convertToString(report.get("reservationRequest")),
                            Converter.convertToString(report.get("usageReservationRequest")),
                            Converter.convertToInterval(report.get("usageInterval")));
                }
                else if (identifier.equals(AllocationStateReportMessages.RESOURCE_NOT_AVAILABLE)) {
                    return new MaximumFutureExceeded(
                            Converter.convertToDateTime(report.get("maxDateTime")));
                }
            }
            else if (identifier.equals(AllocationStateReportMessages.VALUE_ALREADY_ALLOCATED)) {
                Map<String, Object> parentReport = findParentReport(
                        parentReports, AllocationStateReportMessages.ALLOCATING_ALIAS);
                if (parentReport != null) {
                    Set<AliasType> aliasTypes = Converter.convertToSet(parentReport.get("aliasTypes"), AliasType.class);
                    if (aliasTypes.size() == 1) {
                        return new AliasAlreadyAllocated(aliasTypes.iterator().next(),
                                Converter.convertToString(report.get("value")),
                                Converter.convertToInterval(report.get("interval")));
                    }
                }
            }
            else if (identifier.equals(AllocationStateReportMessages.VALUE_NOT_AVAILABLE)) {
                Map<String, Object> parentReport = findParentReport(
                        parentReports, AllocationStateReportMessages.ALLOCATING_ALIAS);
                if (parentReport != null) {
                    Set<AliasType> aliasTypes = Converter.convertToSet(parentReport.get("aliasTypes"), AliasType.class);
                    if (aliasTypes.size() == 1) {
                        return new AliasNotAvailable(aliasTypes.iterator().next(),
                                Converter.convertToInterval(report.get("interval")));
                    }
                }
            }

            parentReports.push(report);
            UserError userError = findAllocationError(getReportChildren(report), parentReports);
            if (userError != null) {
                return userError;
            }
            parentReports.pop();
        }
        return null;
    }

    /**
     * @param parentReports
     * @param parentReportId
     * @return parent report with given {@code parentReportId} in given {@code parentReports}
     */
    private Map<String, Object> findParentReport(Collection<Map<String, Object>> parentReports, String parentReportId)
    {
        for (Map<String, Object> parentReport : parentReports) {
            if (parentReport.get(ID).equals(parentReportId)) {
                return parentReport;
            }
        }
        return null;
    }

    /**
     * Requested time slot is too far in future.
     */
    public static class MaximumFutureExceeded extends UserError
    {
        private DateTime maxDateTime;

        public MaximumFutureExceeded(DateTime maxDateTime)
        {
            this.maxDateTime = maxDateTime;
        }

        @Override
        public String getMessage(Locale locale, DateTimeZone timeZone)
        {
            DateTimeFormatter dateTimeFormatter = DATE_TIME_FORMATTER.with(locale, timeZone);
            String maxDateTime = dateTimeFormatter.formatDateTime(this.maxDateTime);
            return MESSAGE_SOURCE.getMessage("maximumFutureExceeded", locale, maxDateTime);
        }
    }

    /**
     * Time slot from reused reservation request doesn't contain the whole requested time slot
     * (usage time slot must be fully contained).
     */
    public static class ReusementInvalidSlot extends UserError
    {
        private String reusedReservationRequestId;

        private Interval reusedReservationRequestSlot;

        public ReusementInvalidSlot(String reusedReservationRequestId, Interval reusedReservationRequestSlot)
        {
            this.reusedReservationRequestId = reusedReservationRequestId;
            this.reusedReservationRequestSlot = reusedReservationRequestSlot;
        }

        @Override
        public String getMessage(Locale locale, DateTimeZone timeZone)
        {
            DateTimeFormatter dateTimeFormatter = DATE_TIME_FORMATTER.with(locale, timeZone);
            String reusedReservationRequest = MESSAGE_SOURCE.getMessage(
                    "reusementInvalidSlot.reusedReservationRequest", locale, reusedReservationRequestId);
            String reusedReservationRequestSlot = dateTimeFormatter.formatInterval(this.reusedReservationRequestSlot);
            return MESSAGE_SOURCE.getMessage("reusementInvalidSlot", locale,
                    reusedReservationRequest, reusedReservationRequestSlot);
        }
    }

    /**
     * Reused reservation request is already used in requested time slot (usages must not intersect in time).
     */
    public static class ReusementAlreadyUsed extends UserError
    {
        private String reusedReservationRequestId;

        private String usageReservationRequestId;

        private Interval usageReservationRequestSlot;

        public ReusementAlreadyUsed(String reusedReservationRequestId, String usageReservationRequestId,
                Interval usageReservationRequestSlot)
        {
            this.reusedReservationRequestId = reusedReservationRequestId;
            this.usageReservationRequestId = usageReservationRequestId;
            this.usageReservationRequestSlot = usageReservationRequestSlot;
        }

        @Override
        public String getMessage(Locale locale, DateTimeZone timeZone)
        {
            DateTimeFormatter dateTimeFormatter = DATE_TIME_FORMATTER.with(locale, timeZone);
            String reusedReservationRequest = MESSAGE_SOURCE.getMessage(
                    "reusementAlreadyUsed.reusedReservationRequest", locale, reusedReservationRequestId);
            String usageReservationRequest = MESSAGE_SOURCE.getMessage(
                    "reusementAlreadyUsed.usageReservationRequest", locale, usageReservationRequestId);
            String usageReservationRequestSlot = dateTimeFormatter.formatInterval(this.usageReservationRequestSlot);
            return MESSAGE_SOURCE.getMessage("reusementAlreadyUsed", locale,
                    reusedReservationRequest, usageReservationRequest, usageReservationRequestSlot);
        }
    }

    /**
     * Alias of specified {@link #aliasType} and {@link #value} is already allocated in specified {@link #interval}.
     */
    public static class AliasAlreadyAllocated extends UserError
    {
        private AliasType aliasType;

        private String value;

        private Interval interval;

        public AliasAlreadyAllocated(AliasType aliasType, String value, Interval interval)
        {
            this.aliasType = aliasType;
            this.value = value;
            this.interval = interval;
        }

        @Override
        public String getMessage(Locale locale, DateTimeZone timeZone)
        {
            DateTimeFormatter dateTimeFormatter = DATE_TIME_FORMATTER.with(locale, timeZone);
            return MESSAGE_SOURCE.getMessage("aliasAlreadyAllocated." + this.aliasType, locale, value,
                    dateTimeFormatter.formatInterval(interval));
        }
    }

    /**
     * Alias of specified {@link #aliasType} has no more available values in specified {@link #interval}.
     */
    public static class AliasNotAvailable extends UserError
    {
        private AliasType aliasType;

        private Interval interval;

        public AliasNotAvailable(AliasType aliasType, Interval interval)
        {
            this.aliasType = aliasType;
            this.interval = interval;
        }

        @Override
        public String getMessage(Locale locale, DateTimeZone timeZone)
        {
            DateTimeFormatter dateTimeFormatter = DATE_TIME_FORMATTER.with(locale, timeZone);
            return MESSAGE_SOURCE.getMessage("aliasNotAvailable." + this.aliasType, locale,
                    dateTimeFormatter.formatInterval(interval));
        }
    }
}
