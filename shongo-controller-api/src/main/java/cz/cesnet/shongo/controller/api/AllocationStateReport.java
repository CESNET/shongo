package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.api.AbstractObjectReport;
import cz.cesnet.shongo.api.Converter;
import cz.cesnet.shongo.controller.AllocationStateReportMessages;
import cz.cesnet.shongo.controller.api.rpc.AuthorizationService;
import cz.cesnet.shongo.report.Report;
import cz.cesnet.shongo.util.DateTimeFormatter;
import cz.cesnet.shongo.util.MessageSource;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.joda.time.Period;

import java.util.*;

/**
 * {@link cz.cesnet.shongo.api.AbstractObjectReport} for {@link AllocationState}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AllocationStateReport extends AbstractObjectReport
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
    public String toString(Locale locale, DateTimeZone timeZone)
    {
        StringBuilder stringBuilder = new StringBuilder();
        for (Map<String, Object> report : reports) {
            if (stringBuilder.length() > 0) {
                stringBuilder.append("\n\n");
            }
            stringBuilder.append(getReportMessage(report, locale, timeZone));
        }
        return (stringBuilder.length() > 0 ? stringBuilder.toString() : null);
    }

    /**
     * @return {@link UserError} detected from this {@link AllocationStateReport}
     */
    public UserError toUserError()
    {
        UserError userError = findUserError(reports, new Stack<Map<String, Object>>());
        if (userError == null) {
            userError = new UserError();
        }
        return userError;
    }

    /**
     * @param report
     * @param locale
     * @param timeZone
     * @return string message for given {@code report} and {@code locale}
     */
    private String getReportMessage(Map<String, Object> report, Locale locale, DateTimeZone timeZone)
    {
        StringBuilder messageBuilder = new StringBuilder();

        // Append prefix
        String reportId = (String) report.get(ID);
        String message = AllocationStateReportMessages.getMessage(
                reportId, getUserType(), Report.Language.fromLocale(locale), timeZone, report);
        messageBuilder.append("-");
        Report.Type reportType = getReportType(report);
        if (reportType != null) {
            switch (reportType) {
                case ERROR:
                    messageBuilder.append("[ERROR] ");
                    break;
                default:
                    break;
            }
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
            String childReportString = getReportMessage(childReport, locale, timeZone);
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
     * @return {@link AllocationStateReport.UserError} for given {@code reports} and {@code parentReports}
     */
    private UserError findUserError(Collection<Map<String, Object>> reports, Stack<Map<String, Object>> parentReports)
    {
        UserError userError = null;
        for (Map<String, Object> report : reports) {
            String identifier = (String) report.get(ID);
            if (identifier.equals(AllocationStateReportMessages.RESERVATION_REQUEST_DENIED_ALREADY_ALLOCATED)) {
                String resourceName = (String) ((HashMap<String,Object>) report.get("resource")).get("name");
                Interval interval = Converter.convertToInterval(report.get("interval"));
                return new ReservationRequestDeniedAlreadyAllocated(resourceName, interval);
            }
            else if (identifier.equals(AllocationStateReportMessages.RESERVATION_REQUEST_DENIED)) {
                String reason = (String) report.get("reason");
                String deniedBy = (String) report.get("deniedBy");

                return new ReservationRequestDenied(reason, deniedBy);
            }
            else if (identifier.equals(AllocationStateReportMessages.RESOURCE_NOT_FOUND)) {
                if (ResourceNotFound.hasHigherPriorityThan(userError)) {
                    String parentId = (String) parentReports.lastElement().get(ID);
                    if (parentId.equals(AllocationStateReportMessages.ALLOCATING_RECORDING_SERVICE)) {
                        return new ResourceNotFound(ResourceNotFound.Type.RECORDING);
                    }
                    else {
                        return new ResourceNotFound();
                    }
                }
            }
            else if (identifier.equals(AllocationStateReportMessages.RESOURCE_NOT_ALLOCATABLE)) {
                if (ResourceNotFound.hasHigherPriorityThan(userError)) {
                    String parentId = (String) parentReports.lastElement().get(ID);
                    if (parentId.equals(AllocationStateReportMessages.ALLOCATING_RECORDING_SERVICE)) {
                        userError = new ResourceNotFound(ResourceNotFound.Type.RECORDING);
                    }
                    else {
                        userError = new ResourceNotFound();
                    }
                }
            }
            else if (identifier.equals(AllocationStateReportMessages.RESOURCE_ALREADY_ALLOCATED)) {
                Map<String, Object> parentReport = findParentReport(
                        parentReports, AllocationStateReportMessages.ALLOCATING_ALIAS);
                String resourceName = (String) ((HashMap<String,Object>) report.get("resource")).get("name");
                Interval interval = Converter.convertToInterval(report.get("interval"));
                return new ResourceAlreadyAllocated(resourceName, interval);
            }
            else if (identifier.equals(AllocationStateReportMessages.RESOURCE_UNDER_MAINTENANCE)) {
                String resourceName = (String) ((HashMap<String,Object>) report.get("resource")).get("name");
                Interval interval = Converter.convertToInterval(report.get("interval"));
                return new ResourceUnderMaintenance(resourceName, interval);
            }
            else if (identifier.equals(AllocationStateReportMessages.MAXIMUM_DURATION_EXCEEDED)) {
                return new MaximumDurationExceeded(Converter.convertToPeriod(report.get("maxDuration")));
            }
            else if (identifier.equals(AllocationStateReportMessages.RESOURCE_NOT_AVAILABLE)) {
                DateTime dateTime = Converter.convertToDateTime(report.get("maxDateTime"));
                if (userError instanceof MaximumFutureExceeded) {
                    MaximumFutureExceeded maximumFutureExceeded = (MaximumFutureExceeded) userError;
                    if (dateTime.isAfter(maximumFutureExceeded.getMaxDateTime())) {
                        maximumFutureExceeded.setMaxDateTime(dateTime);
                    }
                }
                else if (MaximumFutureExceeded.hasHigherPriorityThan(userError)) {
                    userError = new MaximumFutureExceeded(dateTime);
                }
            }
            else if (parentReports.size() == 0) {
                if (identifier.equals(AllocationStateReportMessages.RESERVATION_REQUEST_INVALID_SLOT)) {
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
            }
            else if (identifier.equals(AllocationStateReportMessages.EXECUTABLE_INVALID_SLOT)) {
                return new ReusementInvalidSlot(
                        Converter.convertToString(report.get("executable")),
                        Converter.convertToInterval(report.get("interval")));
            }
            else if (identifier.equals(AllocationStateReportMessages.EXECUTABLE_ALREADY_USED)) {
                return new ReusementAlreadyUsed(
                        Converter.convertToString(report.get("executable")),
                        Converter.convertToString(report.get("usageReservationRequest")),
                        Converter.convertToInterval(report.get("usageInterval")));
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
            else if (identifier.equals(AllocationStateReportMessages.RESOURCE_ROOM_CAPACITY_EXCEEDED)) {
                Integer availableLicenseCount = Converter.convertToInteger(report.get("availableLicenseCount"));
                Integer maxLicenseCount = Converter.convertToInteger(report.get("maxLicenseCount"));
                if (userError instanceof RoomCapacityExceeded) {
                    RoomCapacityExceeded roomCapacityExceeded = (RoomCapacityExceeded) userError;
                    if (availableLicenseCount > maxLicenseCount) {
                        roomCapacityExceeded.setAvailableLicenseCount(availableLicenseCount);
                        roomCapacityExceeded.setMaxLicenseCount(maxLicenseCount);
                    }
                }
                else if (RoomCapacityExceeded.hasHigherPriorityThan(userError)) {
                    userError = new RoomCapacityExceeded(availableLicenseCount, maxLicenseCount);
                }
            }
            else if (identifier.equals(AllocationStateReportMessages.RESOURCE_SINGLE_ROOM_LIMIT_EXCEEDED)) {
                Integer maxLicencesPerRoom = Converter.convertToInteger(report.get("maxLicencesPerRoom"));
                userError = new SingleRoomLimitExceeded(maxLicencesPerRoom);
            }
            else if (identifier.equals(AllocationStateReportMessages.RESOURCE_RECORDING_CAPACITY_EXCEEDED)) {
                if (!(userError instanceof RecordingCapacityExceeded)) {
                    userError = new RecordingCapacityExceeded();
                }
            }

            parentReports.push(report);
            UserError childUserError = findUserError(getReportChildren(report), parentReports);
            if (childUserError != null) {
                if (identifier.equals(AllocationStateReportMessages.ALLOCATING_RECORDING_SERVICE) && childUserError instanceof RoomCapacityExceeded) {
                    RoomCapacityExceeded roomCapacityExceeded = (RoomCapacityExceeded) childUserError;
                    return new RecordingRoomCapacityExceed(roomCapacityExceeded.getAvailableLicenseCount(), roomCapacityExceeded.getMaxLicenseCount());
                }
                return childUserError;
            }
            parentReports.pop();
        }
        if (userError != null) {
            return userError;
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
     * Represents an allocation error which can be detected from {@link AllocationStateReport} and which can be
     * formatted to user in a user-friendly way.
     */
    public static class UserError
    {
        /**
         * To be used for {@link AllocationStateReport.UserError} messages.
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
         * @return message for this {@link AllocationStateReport.UserError} and for given {@code locale} and {@code timeZone}
         */
        public String getMessage(Locale locale, DateTimeZone timeZone)
        {
            return MESSAGE_SOURCE.getMessage("unknown", locale);
        }

        /**
         * @param locale
         * @return message for this {@link AllocationStateReport.UserError} and for given {@code locale}
         */
        public final String getMessage(Locale locale)
        {
            return getMessage(locale, DateTimeZone.getDefault());
        }

        /**
         * @return message for this {@link AllocationStateReport.UserError}
         */
        public final String getMessage()
        {
            return getMessage(Locale.getDefault(), DateTimeZone.getDefault());
        }

        /**
         * @return true whether this {@link UserError} doesn't represent a specific error,
         *         false otherwise
         */
        public boolean isUnknown()
        {
            return getClass().equals(UserError.class);
        }

        @Override
        public String toString()
        {
            return getMessage();
        }

        /**
         * @param userErrorType
         * @return whether this {@link UserError} has lower priority than given {@code userErrorType},
         */
        public static <T extends UserError> boolean hasHigherPriorityThan(T userErrorType)
        {
            // Resource not found has the lowest possible priority (everything else should be reported instead of it)
            if (userErrorType == null || userErrorType instanceof ResourceNotFound) {
                return true;
            }
            return false;
        }
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

        public DateTime getMaxDateTime()
        {
            return maxDateTime;
        }

        public void setMaxDateTime(DateTime maxDateTime)
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
     * Requested time slot is too long.
     */
    public static class MaximumDurationExceeded extends UserError
    {
        private Period maxDuration;

        public MaximumDurationExceeded(Period maxDuration)
        {
            this.maxDuration = maxDuration;
        }

        @Override
        public String getMessage(Locale locale, DateTimeZone timeZone)
        {
            DateTimeFormatter dateTimeFormatter = DATE_TIME_FORMATTER.with(locale, timeZone);
            String maxDuration = dateTimeFormatter.formatRoundedDuration(this.maxDuration);
            return MESSAGE_SOURCE.getMessage("maximumDurationExceeded", locale, maxDuration);
        }
    }

    /**
     * Time slot from reused reservation request doesn't contain the whole requested time slot
     * (usage time slot must be fully contained).
     */
    public static class ReusementInvalidSlot extends UserError
    {
        private String reusedObjectId;

        private Interval reusedReservationRequestSlot;

        public ReusementInvalidSlot(String reusedObjectId, Interval reusedReservationRequestSlot)
        {
            this.reusedObjectId = reusedObjectId;
            this.reusedReservationRequestSlot = reusedReservationRequestSlot;
        }

        @Override
        public String getMessage(Locale locale, DateTimeZone timeZone)
        {
            DateTimeFormatter dateTimeFormatter = DATE_TIME_FORMATTER.with(locale, timeZone);
            String reusedEntity;
            if (reusedObjectId.contains(":exe:")) {
                reusedEntity = MESSAGE_SOURCE.getMessage("reusementInvalidSlot.reusedRoom", locale, reusedObjectId);
            }
            else {
                reusedEntity = MESSAGE_SOURCE.getMessage(
                        "reusementInvalidSlot.reusedReservationRequest", locale, reusedObjectId);
            }
            String reusedReservationRequestSlot = dateTimeFormatter.formatInterval(this.reusedReservationRequestSlot);
            return MESSAGE_SOURCE.getMessage("reusementInvalidSlot", locale,
                    reusedEntity, reusedReservationRequestSlot);
        }
    }

    /**
     * Reused reservation request is already used in requested time slot (usages must not intersect in time).
     */
    public static class ReusementAlreadyUsed extends UserError
    {
        private String reusedObjectId;

        private String usageReservationRequestId;

        private Interval usageReservationRequestSlot;

        public ReusementAlreadyUsed(String reusedObjectId, String usageReservationRequestId,
                Interval usageReservationRequestSlot)
        {
            this.reusedObjectId = reusedObjectId;
            this.usageReservationRequestId = usageReservationRequestId;
            this.usageReservationRequestSlot = usageReservationRequestSlot;
        }

        public Interval getUsageReservationRequestSlot()
        {
            return usageReservationRequestSlot;
        }

        @Override
        public String getMessage(Locale locale, DateTimeZone timeZone)
        {
            DateTimeFormatter dateTimeFormatter = DATE_TIME_FORMATTER.with(locale, timeZone);
            String reusedEntity;
            if (reusedObjectId.contains(":exe:")) {
                reusedEntity = MESSAGE_SOURCE.getMessage("reusementAlreadyUsed.reusedRoom", locale, reusedObjectId);
            }
            else {
                reusedEntity = MESSAGE_SOURCE.getMessage(
                        "reusementAlreadyUsed.reusedReservationRequest", locale, reusedObjectId);
            }
            String usageReservationRequest = MESSAGE_SOURCE.getMessage(
                    "reusementAlreadyUsed.usageReservationRequest", locale, usageReservationRequestId);
            String usageReservationRequestSlot = dateTimeFormatter.formatInterval(this.usageReservationRequestSlot);
            return MESSAGE_SOURCE.getMessage("reusementAlreadyUsed", locale,
                    reusedEntity, usageReservationRequest, usageReservationRequestSlot);
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

        public AliasType getAliasType()
        {
            return aliasType;
        }

        public String getValue()
        {
            return value;
        }

        public Interval getInterval()
        {
            return interval;
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
     * Resource with name {@link #resourceName}  is already allocated in specified {@link #interval}.
     */
    public static class ResourceAlreadyAllocated extends UserError
    {
        private String resourceName;

        private Interval interval;

        public ResourceAlreadyAllocated(String resourceName, Interval interval)
        {
            this.resourceName = resourceName;
            this.interval = interval;
        }

        public String getResourceName()
        {
            return resourceName;
        }

        public Interval getInterval() {
            return interval;
        }

        @Override
        public String getMessage(Locale locale, DateTimeZone timeZone)
        {
            DateTimeFormatter dateTimeFormatter = DATE_TIME_FORMATTER.with(locale, timeZone);
            return MESSAGE_SOURCE.getMessage("resourceAlreadyAllocated", locale, this.resourceName,
                    dateTimeFormatter.formatInterval(interval));
        }
    }

    /**
     * Resource with name {@link #resourceName} is already allocated in specified {@link #interval} due to maintenance.
     */
    public static class ResourceUnderMaintenance extends ResourceAlreadyAllocated
    {
        public ResourceUnderMaintenance(String resourceName, Interval interval)
        {
            super(resourceName, interval);
        }

        @Override
        public String getMessage(Locale locale, DateTimeZone timeZone)
        {
            DateTimeFormatter dateTimeFormatter = DATE_TIME_FORMATTER.with(locale, timeZone);
            return MESSAGE_SOURCE.getMessage("resourceUnderMaintenance", locale,
                    dateTimeFormatter.formatInterval(getInterval()));
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

    /**
     * Room capacity exceeded.
     */
    public static class RoomCapacityExceeded extends UserError
    {
        protected Integer availableLicenseCount;

        protected Integer maxLicenseCount;

        public RoomCapacityExceeded(Integer availableLicenseCount, Integer maxLicenseCount)
        {
            this.availableLicenseCount = availableLicenseCount;
            this.maxLicenseCount = maxLicenseCount;
        }

        public Integer getAvailableLicenseCount()
        {
            return availableLicenseCount;
        }

        public void setAvailableLicenseCount(Integer availableLicenseCount)
        {
            this.availableLicenseCount = availableLicenseCount;
        }

        public Integer getMaxLicenseCount()
        {
            return maxLicenseCount;
        }

        public void setMaxLicenseCount(Integer maxLicenseCount)
        {
            this.maxLicenseCount = maxLicenseCount;
        }

        @Override
        public String getMessage(Locale locale, DateTimeZone timeZone)
        {
            return MESSAGE_SOURCE.getMessage("roomCapacityExceeded", locale, availableLicenseCount, maxLicenseCount);
        }
    }

    /**
     * Room capacity per single room exceeded.
     */
    public static class SingleRoomLimitExceeded extends UserError
    {

        protected Integer maxLicencesPerRoom;

        public SingleRoomLimitExceeded(Integer maxLicencesPerRoom)
        {
            this.maxLicencesPerRoom = maxLicencesPerRoom;
        }

        public Integer getMaxLicencesPerRoom()
        {
            return maxLicencesPerRoom;
        }

        public void setMaxLicencesPerRoom(Integer maxLicencesPerRoom)
        {
            this.maxLicencesPerRoom = maxLicencesPerRoom;
        }

        @Override
        public String getMessage(Locale locale, DateTimeZone timeZone)
        {
            return MESSAGE_SOURCE.getMessage("singleRoomLimitExceeded", locale, maxLicencesPerRoom);
        }
    }

    /**
     * Resource was not found.
     */
    public static class ResourceNotFound extends UserError
    {
        private Type type;

        public ResourceNotFound()
        {
        }

        public ResourceNotFound(Type type)
        {
            this.type = type;
        }

        public Type getType()
        {
            return type;
        }

        public void setType(Type type)
        {
            this.type = type;
        }

        @Override
        public String getMessage(Locale locale, DateTimeZone timeZone)
        {
            if (type != null) {
                return MESSAGE_SOURCE.getMessage("resourceNotFound." + type, locale);
            }
            else {
                return MESSAGE_SOURCE.getMessage("resourceNotFound", locale);
            }
        }

        public static enum Type
        {
            RECORDING
        }
    }

    /**
     * Recording capacity exceeded.
     */
    public static class RecordingCapacityExceeded extends UserError
    {
        public RecordingCapacityExceeded()
        {
        }

        @Override
        public String getMessage(Locale locale, DateTimeZone timeZone)
        {
            return MESSAGE_SOURCE.getMessage("recordingCapacityExceeded", locale);
        }
    }

    /**
     * Recording is unavailable because {@link RoomCapacityExceeded}.
     */
    public static class RecordingRoomCapacityExceed extends RoomCapacityExceeded
    {
        public RecordingRoomCapacityExceed(Integer availableLicenseCount, Integer maxLicenseCount)
        {
            super(availableLicenseCount, maxLicenseCount);
        }

        @Override
        public String getMessage(Locale locale, DateTimeZone timeZone)
        {
            return MESSAGE_SOURCE.getMessage("recordingRoomCapacityExceeded", locale, availableLicenseCount, maxLicenseCount);
        }
    }

    /**
     * Reservation request denied because resource with name {@link #resourceName} is already allocated in specified {@link #interval}.
     */
    public static class ReservationRequestDeniedAlreadyAllocated extends UserError
    {
        private String resourceName;

        private Interval interval;

        public ReservationRequestDeniedAlreadyAllocated(String resourceName, Interval interval)
        {
            this.resourceName = resourceName;
            this.interval = interval;
        }

        @Override
        public String getMessage(Locale locale, DateTimeZone timeZone)
        {
            DateTimeFormatter dateTimeFormatter = DATE_TIME_FORMATTER.with(locale, timeZone);
            return MESSAGE_SOURCE.getMessage("resourceAlreadyAllocated", locale, this.resourceName,
                    dateTimeFormatter.formatInterval(interval));
        }
    }

    /**
     * Reservation request has been denied by resource owner {@link #userId}.
     *
     * {@link #userName} must be set before {@link #getMessage()} is called;
     */
    public static class ReservationRequestDenied extends UserError implements UserIdentityRequired
    {
        private String reason;

        private String userId;

        private String userName;

        public ReservationRequestDenied(String reason, String userId)
        {
            this.reason = reason;
            this.userId = userId;
        }

        public String getUserId()
        {
            return userId;
        }

        public void setUserName(String userName)
        {
            this.userName = userName;
        }

        @Override
        public String getMessage(Locale locale, DateTimeZone timeZone)
        {
            return getMessage(locale, timeZone, null);
        }

        public String getMessage(Locale locale, DateTimeZone timeZone, String userName)
        {
            if (userName != null && this.userName == null) {
                this.userName = userName;
            }
            if (this.userName == null) {
                throw new IllegalStateException("Username must be set.");
            }
//            if (reason == null || "".equals(reason)) {
//                reason = MESSAGE_SOURCE.getMessage("reservationRequestDenied.reason.none", locale);
//            }
//            return MESSAGE_SOURCE.getMessage("reservationRequestDenied", locale, this.userName, reason);
            return MESSAGE_SOURCE.getMessage("reservationRequestDenied", locale, this.userName);
        }
    }

    /**
     * Specifies {@link cz.cesnet.shongo.controller.api.AllocationStateReport.UserError} requiring user lookup after initialization.
     */
    public interface UserIdentityRequired
    {
        String getUserId();

        void setUserName(String userName);
    }
}
