package cz.cesnet.shongo.client.web.models;

import com.google.common.base.Strings;
import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.Temporal;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.AdobeConnectRoomSetting;
import cz.cesnet.shongo.api.H323RoomSetting;
import cz.cesnet.shongo.client.web.Cache;
import cz.cesnet.shongo.client.web.CacheProvider;
import cz.cesnet.shongo.client.web.ClientWebConfiguration;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.request.AvailabilityCheckRequest;
import cz.cesnet.shongo.controller.api.rpc.ReservationService;
import cz.cesnet.shongo.util.DateTimeFormatter;
import cz.cesnet.shongo.util.SlotHelper;
import org.apache.commons.lang.StringUtils;
import org.joda.time.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validator for {@link ReservationRequestModel}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationRequestValidator implements Validator
{
    private static Logger logger = LoggerFactory.getLogger(ReservationRequestValidator.class);

    private static final Pattern PATTERN_IDENTIFIER = Pattern.compile("^[a-zA-Z0-9_-]*$");
    private static final Pattern PATTERN_ALPHA_NUM = Pattern.compile("^[a-zA-Z0-9]*$");
    private static final Pattern PATTERN_NUM = Pattern.compile("^[0-9]*$");
    private static final Pattern PATTERN_H323_E164_NUMBER = Pattern.compile(ClientWebConfiguration.getInstance().getE164Pattern());

    protected static DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.getInstance(DateTimeFormatter.Type.SHORT);

    private SecurityToken securityToken;

    private ReservationService reservationService;

    private Cache cache;

    private Locale locale;

    private DateTimeZone timeZone;

    public ReservationRequestValidator(SecurityToken securityToken, ReservationService reservationService, Cache cache,
            Locale locale, DateTimeZone timeZone)
    {
        this.securityToken = securityToken;
        this.reservationService = reservationService;
        this.cache = cache;
        this.locale = locale;
        this.timeZone = timeZone;
    }

    @Override
    public boolean supports(Class<?> type)
    {
        return ReservationRequestValidator.class.equals(type);
    }

    @Override
    public void validate(Object object, Errors errors)
    {
        ReservationRequestModel reservationRequestModel = (ReservationRequestModel) object;
        SpecificationType specificationType = reservationRequestModel.getSpecificationType();

        // Remove deleted (null) slots and duplicate from except slots
        // Sort - for validation and user GUI
        if (reservationRequestModel.getExcludeDates() != null) {
            List<LocalDate> excludeDates = new ArrayList<>(new HashSet<>(reservationRequestModel.getExcludeDates()));
            Iterator<LocalDate> i = excludeDates.iterator();
            while (i.hasNext()) {
                LocalDate excludeDate = i.next();
                if (excludeDate == null) {
                    i.remove();
                }
            }
            Collections.sort(excludeDates);
            reservationRequestModel.setExcludeDates(excludeDates);
        }

        if (specificationType != SpecificationType.MEETING_ROOM) {
            ValidationUtils.rejectIfEmptyOrWhitespace(errors, "technology", "validation.field.required");
        }
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "start", "validation.field.required");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "specificationType", "validation.field.required");

        if (!Strings.isNullOrEmpty(reservationRequestModel.getRoomPin())) {
            if (TechnologyModel.H323_SIP.equals(reservationRequestModel.getTechnology())) {
                validateNum("roomPin", errors);
            }
            else {
                validateAlphaNum("roomPin", errors);
            }
        }

        if (!Strings.isNullOrEmpty(reservationRequestModel.getAdminPin())) {
            if (TechnologyModel.FREEPBX.equals(reservationRequestModel.getTechnology())) {
                validateNum("adminPin", errors);
            }
            else {
                validateAlphaNum("adminPin", errors);
            }
        }

        if (!Strings.isNullOrEmpty(reservationRequestModel.getRoomPin())) {
            if (TechnologyModel.FREEPBX.equals(reservationRequestModel.getTechnology())) {
                validateNum("roomPin", errors);
            }
            else {
                validateAlphaNum("roomPin", errors);
            }
        }
        if (TechnologyModel.PEXIP.equals(reservationRequestModel.getTechnology())) {
            if (!Strings.isNullOrEmpty(reservationRequestModel.getAdminPin())) {
                validateNum("adminPin", errors);
                validatePinLength("adminPin", errors);
            }
            if (reservationRequestModel.getAllowGuests()) {
                ValidationUtils.rejectIfEmptyOrWhitespace(errors, "adminPin", "validation.field.requiredAdminPin");
            }
            if (!Strings.isNullOrEmpty(reservationRequestModel.getGuestPin())) {
                ValidationUtils.rejectIfEmptyOrWhitespace(errors, "adminPin", "validation.field.requiredAdminPin");
                validateNum("guestPin", errors);
                validatePinLength("guestPin", errors);
            }
        }

        if (!Strings.isNullOrEmpty(reservationRequestModel.getGuestPin()) && !Strings.isNullOrEmpty(reservationRequestModel.getAdminPin()) ) {
            if (TechnologyModel.PEXIP.equals(reservationRequestModel.getTechnology())) {
                if (reservationRequestModel.getAdminPin().equals(reservationRequestModel.getGuestPin())) {
                    errors.rejectValue("adminPin", "validation.field.equalPins");
                }
                validateSameLength("adminPin", "guestPin", errors);
            }
        }

        if (!Strings.isNullOrEmpty(reservationRequestModel.getRoomPin()) && !Strings.isNullOrEmpty(reservationRequestModel.getAdminPin()) ) {
            if (TechnologyModel.FREEPBX.equals(reservationRequestModel.getTechnology())) {
                if (reservationRequestModel.getAdminPin().equals(reservationRequestModel.getRoomPin())) {
                    errors.rejectValue("adminPin", "validation.field.equalPins");
                }
                validateSameLength("adminPin", "roomPin", errors);
            }
        }

        if (Strings.isNullOrEmpty(reservationRequestModel.getDescription())) {
            errors.rejectValue("description", "validation.field.description");
        } else {
            validateNotNum("description", errors);
        }

        if (specificationType != null) {
            switch (specificationType) {
                case PERMANENT_ROOM_CAPACITY:
                    ValidationUtils.rejectIfEmptyOrWhitespace(
                            errors, "permanentRoomReservationRequestId", "validation.field.required");
                case ADHOC_ROOM:
                    ValidationUtils.rejectIfEmptyOrWhitespace(errors, "slotBeforeMinutes", "validation.field.required");
                    ValidationUtils.rejectIfEmptyOrWhitespace(errors, "slotAfterMinutes", "validation.field.required");
                    if (reservationRequestModel.getDurationType() != null) {
                        validateDurationCount(reservationRequestModel, errors);
                    }
                    else {
                        validateInterval(reservationRequestModel, errors);
                    }
                    ValidationUtils.rejectIfEmptyOrWhitespace(errors, "roomParticipantCount", "validation.field.required");
                    Integer roomParticipantCount = reservationRequestModel.getRoomParticipantCount();
                    if (roomParticipantCount != null && roomParticipantCount <= 0) {
                        errors.rejectValue("roomParticipantCount", "validation.field.invalidCount");
                    }
                    validatePeriodicity(reservationRequestModel, errors);
                    validatePeriodicSlotsStart(reservationRequestModel, errors);
                    validatePeriodicExclusions(reservationRequestModel, timeZone, errors);
                    validateParticipants(reservationRequestModel, errors, true);
                    break;
                case MEETING_ROOM:
                    validatePeriodicity(reservationRequestModel, errors);
                    validatePeriodicSlotsStart(reservationRequestModel, errors);
                    validatePeriodicExclusions(reservationRequestModel, timeZone, errors);
                    if (reservationRequestModel.getDurationType() != null) {
                        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "meetingRoomResourceId", "validation.field.required");
                        validateDurationCount(reservationRequestModel, errors);
                    }
                    else {
                        validateInterval(reservationRequestModel, errors);
                    }
                    break;
                case PERMANENT_ROOM:
                    validateInterval(reservationRequestModel, errors);
                    ValidationUtils.rejectIfEmptyOrWhitespace(errors, "roomName", "validation.field.required");
                    validateIdentifier("roomName", errors);
                    if (TechnologyModel.H323_SIP.equals(reservationRequestModel.getTechnology())) {
                        validateE164Number("e164Number", errors);
                    }
                    break;
            }
        }

        String slotField = (reservationRequestModel.getEnd() != null ? "end" : "start");
        String slotFieldDuration = (reservationRequestModel.getEnd() != null ? "end" : "durationCount");
        try {
            Interval interval = reservationRequestModel.getFirstSlot();
            if (interval.getEnd().isBefore(DateTime.now())) {
                errors.rejectValue(slotField, "validation.field.invalidFutureSlot");
            }
        }
        catch (Exception exception) {
            // We ignore errors in computing time slot when other error is present
            if (!errors.hasErrors()) {
                throw new IllegalStateException("Time slot cannot be checked whether it is in future.", exception);
            }
        }

        // Check permanent room capacity periodicity
        if (SpecificationType.PERMANENT_ROOM_CAPACITY.equals(specificationType)) {
            PeriodicDateTimeSlot.PeriodicityType periodicityType = reservationRequestModel.getPeriodicityType();
            if (periodicityType != null && !PeriodicDateTimeSlot.PeriodicityType.NONE.equals(periodicityType)) {
                if (StringUtils.isNotEmpty(reservationRequestModel.getPermanentRoomReservationRequestId())) {
                    ReservationRequestSummary permanentRoom =
                            reservationRequestModel.loadPermanentRoom(new CacheProvider(cache, securityToken));
                    LocalDate permanentRoomEnd = permanentRoom.getEarliestSlot().getEnd().toLocalDate();
                    LocalDate periodicityEnd = reservationRequestModel.getPeriodicityEnd();
                    if (periodicityEnd == null) {
                        reservationRequestModel.setPeriodicityEnd(permanentRoomEnd);
                    }
                    else if (periodicityEnd.isAfter(permanentRoomEnd)) {
                        errors.rejectValue("periodicityEnd", "validation.field.permanentRoomNotAvailable");
                    }
                }
            }
        }

        if (errors.hasErrors()) {
            return;
        }

        if (specificationType != null) {
            AvailabilityCheckRequest availabilityCheckRequest = new AvailabilityCheckRequest(securityToken);
            availabilityCheckRequest.setPurpose(reservationRequestModel.getPurpose());
            SortedSet<PeriodicDateTimeSlot> slots = reservationRequestModel.getSlots(timeZone);
            if (reservationRequestModel.getExcludeDates() != null && !reservationRequestModel.getExcludeDates().isEmpty()) {
                for (LocalDate excludeDate : reservationRequestModel.getExcludeDates()) {
                    for (PeriodicDateTimeSlot slot : slots) {
                        if (Temporal.dateFitsInterval(slot.getStart(), slot.getEnd(), excludeDate)) {
                            slot.addExcludeDate(excludeDate);
                        }
                    }
                }
            }
            availabilityCheckRequest.addAllSlots(new LinkedList<>(slots));
            if (!Strings.isNullOrEmpty(reservationRequestModel.getId())) {
                availabilityCheckRequest.setIgnoredReservationRequestId(reservationRequestModel.getId());
            }
            if (!PeriodicDateTimeSlot.PeriodicityType.NONE.equals(reservationRequestModel.getPeriodicityType())) {
                availabilityCheckRequest.setPeriod(reservationRequestModel.getPeriodicityType().toPeriod());
                availabilityCheckRequest.setPeriodEnd(reservationRequestModel.getPeriodicityEnd());
                if (PeriodicDateTimeSlot.PeriodicityType.MonthPeriodicityType.SPECIFIC_DAY.equals(reservationRequestModel.getMonthPeriodicityType())) {
                    availabilityCheckRequest.setPeriodicityDayOrder(reservationRequestModel.periodicityDayOrder);
                    availabilityCheckRequest.setPeriodicityDayInMonth(reservationRequestModel.periodicityDayInMonth);
                }
            }

            availabilityCheckRequest.setSpecification(reservationRequestModel.toSpecificationApi());
            switch (specificationType) {
                case PERMANENT_ROOM_CAPACITY:
                    String permanentRoomId = reservationRequestModel.getPermanentRoomReservationRequestId();
                    availabilityCheckRequest.setReservationRequestId(permanentRoomId);
                    break;
            }
            // Check availability
            try {
                Object availabilityCheckResult = reservationService.checkPeriodicAvailability(availabilityCheckRequest);
                if (!Boolean.TRUE.equals(availabilityCheckResult)) {
                    AllocationStateReport allocationStateReport = (AllocationStateReport) availabilityCheckResult;
                    AllocationStateReport.UserError userError = allocationStateReport.toUserError();
                    if (userError instanceof AllocationStateReport.AliasAlreadyAllocated) {
                        AllocationStateReport.AliasAlreadyAllocated aliasAlreadyAllocated;
                        aliasAlreadyAllocated = (AllocationStateReport.AliasAlreadyAllocated) userError;
                        DateTimeFormatter dateTimeFormatter = DATE_TIME_FORMATTER.with(locale, timeZone);
                        switch (aliasAlreadyAllocated.getAliasType()) {
                            case ROOM_NAME:
                                errors.rejectValue(
                                    "roomName", "validation.field.roomNameNotAvailable", new Object[]{dateTimeFormatter.formatInterval(aliasAlreadyAllocated.getInterval())}, null);
                                break;
                            case H323_E164:
                                errors.rejectValue("e164Number", "validation.field.E164NumberNotAvailable", new Object[]{dateTimeFormatter.formatInterval(aliasAlreadyAllocated.getInterval())},null);
                                break;
                            default:
                                throw new TodoImplementException("Unsupported report: " + userError.getClass().getSimpleName());
                        }
                    }
                    else if (userError instanceof AllocationStateReport.ReusementAlreadyUsed) {
                        AllocationStateReport.ReusementAlreadyUsed reusementAlreadyUsed;
                        reusementAlreadyUsed = (AllocationStateReport.ReusementAlreadyUsed) userError;
                        Interval slot = reusementAlreadyUsed.getUsageReservationRequestSlot();
                        DateTimeFormatter dateTimeFormatter = DATE_TIME_FORMATTER.with(locale, timeZone);

                        errors.rejectValue(
                                slotField, "validation.field.permanentRoomAlreadyUsed", new Object[]{dateTimeFormatter.formatInterval(slot)},null);

                    }
                    else if (userError instanceof AllocationStateReport.ReusementInvalidSlot) {
                        errors.rejectValue(
                                slotField, "validation.field.permanentRoomNotAvailable");
                    }
                    else if (userError instanceof AllocationStateReport.RecordingCapacityExceeded
                            || userError instanceof AllocationStateReport.RecordingRoomCapacityExceed
                            || (userError instanceof AllocationStateReport.ResourceNotFound &&
                                        AllocationStateReport.ResourceNotFound.Type.RECORDING.equals(
                                                ((AllocationStateReport.ResourceNotFound) userError).getType()))) {
                        errors.rejectValue("roomRecorded", null, userError.getMessage(locale, timeZone));
                    }
                    else if (userError instanceof AllocationStateReport.RoomCapacityExceeded) {
                        errors.rejectValue("roomParticipantCount", null, userError.getMessage(locale, timeZone));
                    }
                    else if (userError instanceof AllocationStateReport.SingleRoomLimitExceeded) {
                        errors.rejectValue("roomParticipantCount", null, userError.getMessage(locale, timeZone));
                    }
                    else if (userError instanceof AllocationStateReport.MaximumFutureExceeded) {
                        errors.rejectValue(slotField, null, userError.getMessage(locale, timeZone));
                    }
                    else if (userError instanceof AllocationStateReport.MaximumDurationExceeded) {
                        errors.rejectValue(slotFieldDuration, null, userError.getMessage(locale, timeZone));
                    }
                    else if (userError instanceof AllocationStateReport.ResourceAlreadyAllocated) {
                        AllocationStateReport.ResourceAlreadyAllocated error = (AllocationStateReport.ResourceAlreadyAllocated) userError;
                        if (userError instanceof AllocationStateReport.ResourceUnderMaintenance) {
                            // If resource is allocated, but virtual room is requested
                            errors.rejectValue("roomParticipantCount", null, error.getMessage(locale, timeZone));
                        } else if (reservationRequestModel.getMeetingRoomResourceId() != null) {
                            // Meeting room already allocated
                            errors.rejectValue("meetingRoomResourceId", null, userError.getMessage(locale, timeZone));
                            // check if colliding interval is not the first one for periodic reservation request
                            if (!SlotHelper.areIntervalsColliding(error.getInterval(), reservationRequestModel.getFirstSlot())) {
                                errors.rejectValue("collidingInterval", null, error.getInterval().toString());
                            }
                        } else {
                            throw new TodoImplementException("Unsupported error: " + error.getClass().getSimpleName());
                        }
                    }
                    else {
                        logger.warn("Validation of availability failed: {}\n{}", userError, allocationStateReport);
                    }
                }
            }
            catch (CommonReportSet.ClassAttributeValueMaximumLengthExceededException exception) {
                String className = exception.getClassName();
                String attribute = exception.getAttribute();
                int maximumLength = exception.getMaximumLength();
                if (className.equals(AliasSpecification.CLASS) && attribute.equals(AliasSpecification.VALUE)) {
                    errors.rejectValue("roomName", "validation.field.maximumLengthExceeded",
                            new Object[]{maximumLength}, null);
                }
                else if (attribute.equals(H323RoomSetting.PIN) || attribute.equals(AdobeConnectRoomSetting.PIN)) {
                    errors.rejectValue("roomPin", "validation.field.maximumLengthExceeded",
                            new Object[]{maximumLength}, null);
                }
            }

        }
    }

    /**
     * @param reservationRequestModel to get validated participants
     * @param errors
     * @param autoFixError specifies whether error should be automatically fixed instead of appending them to
     *                     given {@code errors}. It is useful for validating on different page without the proper form
     * @return true whether validation succeeds, otherwise false
     */
    public static boolean validateParticipants(ReservationRequestModel reservationRequestModel, Errors errors,
            boolean autoFixError)
    {
        if (reservationRequestModel.isRoomParticipantNotificationEnabled()) {
            if (autoFixError) {
                String roomMeetingName = reservationRequestModel.getRoomMeetingName();
                if (roomMeetingName == null ||!org.springframework.util.StringUtils.hasText(roomMeetingName)) {
                    reservationRequestModel.setRoomParticipantNotificationEnabled(false);
                }
            }
            else {
                ValidationUtils.rejectIfEmptyOrWhitespace(errors, "roomMeetingName", "validation.field.required");
            }
        }
        return !errors.hasErrors();
    }

    /**
     * @param reservationRequestModel to get validated interval
     * @param errors
     * @return true whether validation succeeds, otherwise false
     */
    public static void validateInterval(ReservationRequestModel reservationRequestModel, Errors errors)
    {
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "end", "validation.field.required");
        DateTime start = reservationRequestModel.getRequestStart();
        DateTime end = reservationRequestModel.getEnd();
        if (end != null && end.getMillisOfDay() == 0) {
            end = end.withHourOfDay(23).withMinuteOfHour(59).withSecondOfMinute(59);
        }
        if (start != null && end != null && !start.isBefore(end)) {
            errors.rejectValue("end", "validation.field.invalidIntervalEnd");
        }
    }

    public static void validatePeriodicity(ReservationRequestModel reservationRequestModel, Errors errors)
    {
        validatePeriodicityEnd(reservationRequestModel, errors);
        switch (reservationRequestModel.getPeriodicityType()) {
            case WEEKLY:
                if (reservationRequestModel.getPeriodicDaysInWeek().length == 0) {
                    errors.rejectValue("periodicDaysInWeek","validation.field.noCheckboxChecked");
                }
            case MONTHLY:
                validateNum("periodicityCycle", errors);
                break;
        }
    }

    public static void validatePeriodicityEnd(ReservationRequestModel reservationRequestModel, Errors errors)
    {
        if (!reservationRequestModel.getPeriodicityType().equals(PeriodicDateTimeSlot.PeriodicityType.NONE)) {
            ValidationUtils.rejectIfEmptyOrWhitespace(errors, "periodicityEnd", "validation.field.required");
            DateTime periodicityStart = reservationRequestModel.getRequestStart();
            LocalDate periodicityEnd = reservationRequestModel.getPeriodicityEnd();
            if ((periodicityStart != null && periodicityEnd != null && periodicityEnd.isBefore(periodicityStart.toLocalDate()))) {
                errors.rejectValue("periodicityEnd", "validation.field.invalidIntervalEnd");
            }
        }
    }

    public static void validatePeriodicSlotsStart(ReservationRequestModel reservationRequestModel, Errors errors)
    {
        try {
            LocalDate end = reservationRequestModel.getPeriodicityEnd();
            if (end == null) {
                return;
            }
            SortedSet<PeriodicDateTimeSlot> slots = reservationRequestModel.getSlots(null);
            int invalidSlots = 0;
            for (PeriodicDateTimeSlot slot : slots) {
                if (!PeriodicDateTimeSlot.PeriodicityType.NONE.equals(reservationRequestModel.getPeriodicityType())
                && (end != null && slot.getStart().toLocalDate().isAfter(end))) {
                    invalidSlots++;
                }
            }
            if (slots.size() - invalidSlots == 0) {
                errors.rejectValue("periodicityEnd", "validation.field.invalidIntervalEnd");
            }
        }
        catch (IllegalStateException ex) {
            //Skip this test
        }
    }

    public static void validatePeriodicExclusions(ReservationRequestModel reservationRequestModel, DateTimeZone timeZone, Errors errors)
    {
        DateTime start = reservationRequestModel.getRequestStart();
        Integer duration = reservationRequestModel.getDurationCount();
        LocalDate end = reservationRequestModel.getPeriodicityEnd();
        if (start == null || duration == null || end == null) {
            return;
        }
        if (reservationRequestModel.getExcludeDates() != null && !reservationRequestModel.getExcludeDates().isEmpty()) {
            int index = 0;
            for (LocalDate excludeDate : reservationRequestModel.getExcludeDates()) {
                boolean invalidExcludeDate = true;
                for (PeriodicDateTimeSlot slot : reservationRequestModel.getSlots(timeZone)) {
                    if (Temporal.dateFitsInterval(slot.getStart(), slot.getEnd(), excludeDate)) {
                        invalidExcludeDate = false;
                    }
                }
                if (invalidExcludeDate) {
                    // For specific field by index validation, the exclude dates collection must be sorted
                    errors.rejectValue("excludeDates", "validation.field.invalidExcludeDates");
                    errors.rejectValue("excludeDates[" + index + "]", "validation.field.invalidExcludeDates");
                }
                index++;
            }
        }
    }

    /**
     * Validate duration count if filled properly.
     *
     * @param reservationRequestModel
     * @param errors
     */
    public static void validateDurationCount(ReservationRequestModel reservationRequestModel, Errors errors)
    {
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "durationCount", "validation.field.required");
        if (reservationRequestModel.getDurationCount() != null) {
            validatePositiveNum("durationCount", errors);
        }
    }

    /**
     * @param field
     * @param errors
     * @return true whether validation succeeds, otherwise false
     */
    public static void validateIdentifier(String field, Errors errors)
    {
        String value = (String) errors.getFieldValue(field);
        Matcher matcher = PATTERN_IDENTIFIER.matcher(value);
        if (!matcher.matches()) {
            errors.rejectValue(field, "validation.field.invalidIdentifier");
        }
    }

    /**
     * @param field
     * @param errors
     * @return true whether validation succeeds, otherwise false
     */
    public static void validateAlphaNum(String field, Errors errors)
    {
        String value = (String) errors.getFieldValue(field);
        Matcher matcher = PATTERN_ALPHA_NUM.matcher(value);
        if (!matcher.matches()) {
            errors.rejectValue(field, "validation.field.invalidAlphaNum");
        }
    }

    /**
     * @param field
     * @param errors
     * @return true whether validation succeeds, otherwise false
     */
    public static void validateNum(String field, Errors errors)
    {
        String value = (String) errors.getFieldValue(field);
        Matcher matcher = PATTERN_NUM.matcher(value);
        if (!matcher.matches()) {
            errors.rejectValue(field, "validation.field.invalidNum");
        }
    }

    /**
     * @param field
     * @param errors
     * @return true whether validation succeeds, otherwise false
     */
    public static void validateNotNum(String field, Errors errors)
    {
        String value = (String) errors.getFieldValue(field);
        Matcher matcher = PATTERN_NUM.matcher(value);
        if (matcher.matches()) {
            errors.rejectValue(field, "validation.field.invalidNotNum");
        }
    }

    public static void validatePositiveNum(String field, Errors errors)
    {
        String value = (String) errors.getFieldValue(field);
        Matcher matcher = PATTERN_NUM.matcher(value);
        if (!matcher.matches()) {
            errors.rejectValue(field, "validation.field.invalidNum");
        } else {
            Integer number = Integer.parseInt(value);
            if (number < 1) {
                errors.rejectValue(field, "validation.field.invalidPositiveNum");
            }
        }
    }

    public static void validateE164Number(String field, Errors errors)
    {
        String value = (String) errors.getFieldValue(field);
        if (!Strings.isNullOrEmpty(value)) {
            Matcher matcher = PATTERN_H323_E164_NUMBER.matcher(value);
            if (!matcher.matches()) {
                errors.rejectValue(field, "validation.field.invalidNum.E164");
            }
        }
    }

    public static void validateSameLength(String field1, String field2, Errors errors) {
        String value1 = (String) errors.getFieldValue(field1);
        String value2 = (String) errors.getFieldValue(field2);
        int lengthDifference = value1.length() - value2.length();
        if (lengthDifference != 0) {
            errors.rejectValue(field1, "validation.field.pinsLengthDiffer");
            errors.rejectValue(field2, "validation.field.pinsLengthDiffer");
        }
    }

    public static void validatePinLength(String field, Errors errors) {
        String value = (String) errors.getFieldValue(field);
        if (value.length() < 4) {
            errors.rejectValue(field, "validation.field.lengthTooShort");
        }
        if (value.length() > 20) {
            errors.rejectValue(field, "validation.field.lengthTooLong");
        }
    }
}
