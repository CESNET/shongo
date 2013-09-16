package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.api.AbstractEntityReport;
import cz.cesnet.shongo.api.Converter;
import cz.cesnet.shongo.controller.AllocationStateReportMessages;
import cz.cesnet.shongo.report.Report;
import cz.cesnet.shongo.util.DateTimeFormatter;
import cz.cesnet.shongo.util.MessageSource;
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
     * @return {@link AllocationError} detected from this {@link AllocationStateReport}
     */
    public AllocationError toAllocationError()
    {
        AllocationError allocationError = findAllocationError(reports, new Stack<Map<String, Object>>());
        if (allocationError == null) {
            allocationError = new AllocationError();
        }
        return allocationError;
    }

    /**
     * Represents an allocation error which can be detected from {@link AllocationStateReport} and which can be
     * formatted to user in a user-friendly way.
     */
    public static class AllocationError
    {
        /**
         * To be used for {@link AllocationError} messages.
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
         * @return message for this {@link AllocationError} and for given {@code locale} and {@code timeZone}
         */
        public String getMessage(Locale locale, DateTimeZone timeZone)
        {
            return MESSAGE_SOURCE.getMessage("unknown", locale);
        }

        /**
         * @param locale
         * @return message for this {@link AllocationError} and for given {@code locale}
         */
        public final String getMessage(Locale locale)
        {
            return getMessage(locale, DateTimeZone.getDefault());
        }

        /**
         * @return message for this {@link AllocationError}
         */
        public final String getMessage()
        {
            return getMessage(Locale.getDefault(), DateTimeZone.getDefault());
        }

        public boolean isUnknown()
        {
            return getClass().equals(AllocationError.class);
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
     * @return {@link AllocationError} for given {@code reports} and {@code parentReports}
     */
    private AllocationError findAllocationError(Collection<Map<String, Object>> reports, Stack<Map<String, Object>> parentReports)
    {
        for (Map<String, Object> report : reports) {
            String identifier = (String) report.get(ID);
            if (identifier.equals(AllocationStateReportMessages.VALUE_ALREADY_ALLOCATED)) {
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
            AllocationError allocationError = findAllocationError(getReportChildren(report), parentReports);
            if (allocationError != null) {
                return allocationError;
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

    public static class AliasAlreadyAllocated extends AllocationError
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
            DateTimeFormatter dateTimeFormatter = DATE_TIME_FORMATTER.with(locale);
            return MESSAGE_SOURCE.getMessage("aliasAlreadyAllocated." + this.aliasType, locale, value,
                    dateTimeFormatter.formatInterval(interval));
        }
    }

    public static class AliasNotAvailable extends AllocationError
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
            DateTimeFormatter dateTimeFormatter = DATE_TIME_FORMATTER.with(locale);
            return MESSAGE_SOURCE.getMessage("aliasNotAvailable." + this.aliasType, locale,
                    dateTimeFormatter.formatInterval(interval));
        }
    }
}
