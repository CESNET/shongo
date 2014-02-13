package cz.cesnet.shongo.client.web.models;

import com.google.common.base.Strings;
import cz.cesnet.shongo.controller.api.AllocationStateReport;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.request.AvailabilityCheckRequest;
import cz.cesnet.shongo.controller.api.rpc.ReservationService;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import javax.servlet.http.HttpServletRequest;
import java.util.Locale;

/**
 * Validator for {@link ReservationRequestModel}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationRequestValidator implements Validator
{
    private static Logger logger = LoggerFactory.getLogger(ReservationRequestValidator.class);

    private SecurityToken securityToken;

    private ReservationService reservationService;

    private Locale locale;

    private DateTimeZone timeZone;

    public ReservationRequestValidator(SecurityToken securityToken, ReservationService reservationService,
            Locale locale, DateTimeZone timeZone)
    {
        this.securityToken = securityToken;
        this.reservationService = reservationService;
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

        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "technology", "validation.field.required");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "start", "validation.field.required");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "specificationType", "validation.field.required");

        SpecificationType specificationType = reservationRequestModel.getSpecificationType();
        if (specificationType != null) {
            switch (specificationType) {
                case ADHOC_ROOM:
                case PERMANENT_ROOM_CAPACITY:
                    ValidationUtils.rejectIfEmptyOrWhitespace(errors, "slotBeforeMinutes", "validation.field.required");
                    ValidationUtils.rejectIfEmptyOrWhitespace(errors, "slotAfterMinutes", "validation.field.required");
                    ValidationUtils.rejectIfEmptyOrWhitespace(errors, "durationCount", "validation.field.required");
                    ValidationUtils.rejectIfEmptyOrWhitespace(
                            errors, "roomParticipantCount", "validation.field.required");
                    Integer roomParticipantCount = reservationRequestModel.getRoomParticipantCount();
                    if (roomParticipantCount != null && roomParticipantCount <= 0) {
                        errors.rejectValue("roomParticipantCount", "validation.field.invalidCount");
                    }
                    validateParticipants(reservationRequestModel, errors);
                    break;
            }
            switch (specificationType) {
                case PERMANENT_ROOM:
                    ValidationUtils.rejectIfEmptyOrWhitespace(errors, "end", "validation.field.required");
                    DateTime start = reservationRequestModel.getStart();
                    DateTime end = reservationRequestModel.getEnd();
                    if (end != null && end.getMillisOfDay() == 0) {
                        end = end.withHourOfDay(23).withMinuteOfHour(59).withSecondOfMinute(59);
                    }
                    if (start != null && end != null && !start.isBefore(end)) {
                        errors.rejectValue("end", "validation.field.invalidIntervalEnd");
                    }
                    ValidationUtils.rejectIfEmptyOrWhitespace(errors, "roomName", "validation.field.required");
                    break;
                case PERMANENT_ROOM_CAPACITY:
                    ValidationUtils.rejectIfEmptyOrWhitespace(
                            errors, "permanentRoomReservationRequestId", "validation.field.required");
                    break;
            }
        }

        String slotField = (SpecificationType.PERMANENT_ROOM.equals(specificationType) ? "end" : "start");
        try {
            Interval interval = reservationRequestModel.getSlot();
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

        if (errors.hasErrors()) {
            return;
        }

        if (specificationType != null) {
            AvailabilityCheckRequest availabilityCheckRequest = new AvailabilityCheckRequest(securityToken);
            availabilityCheckRequest.setPurpose(ReservationRequestModel.PURPOSE);
            availabilityCheckRequest.setSlot(reservationRequestModel.getSlot());
            if (!Strings.isNullOrEmpty(reservationRequestModel.getId())) {
                availabilityCheckRequest.setIgnoredReservationRequestId(reservationRequestModel.getId());
            }
            availabilityCheckRequest.setSpecification(reservationRequestModel.toSpecificationApi());
            switch (specificationType) {
                case PERMANENT_ROOM_CAPACITY:
                    String permanentRoomId = reservationRequestModel.getPermanentRoomReservationRequestId();
                    availabilityCheckRequest.setReservationRequestId(permanentRoomId);
                    break;
            }
            // Check availability
            Object availabilityCheckResult = reservationService.checkAvailability(availabilityCheckRequest);
            if (!Boolean.TRUE.equals(availabilityCheckResult)) {
                AllocationStateReport allocationStateReport = (AllocationStateReport) availabilityCheckResult;
                AllocationStateReport.UserError userError = allocationStateReport.toUserError();
                if (userError instanceof AllocationStateReport.AliasAlreadyAllocated) {
                    errors.rejectValue(
                            "roomName", "validation.field.roomNameNotAvailable");
                }
                else if (userError instanceof AllocationStateReport.ReusementAlreadyUsed) {
                    errors.rejectValue(
                            "start", "validation.field.permanentRoomAlreadyUsed");
                }
                else if (userError instanceof AllocationStateReport.ReusementInvalidSlot) {
                    errors.rejectValue(
                            "start", "validation.field.permanentRoomNotAvailable");
                }
                else if (userError instanceof AllocationStateReport.RecordingRoomCapacityExceed) {
                    errors.rejectValue("roomRecorded", null, userError.getMessage(locale, timeZone));
                }
                else if (userError instanceof AllocationStateReport.RoomCapacityExceeded) {
                    errors.rejectValue("roomParticipantCount", null, userError.getMessage(locale, timeZone));
                }
                else if (userError instanceof AllocationStateReport.MaximumFutureExceeded) {
                    errors.rejectValue(slotField, null, userError.getMessage(locale, timeZone));
                }
                else {
                    logger.warn("Validation of availability failed: {}\n{}", userError, allocationStateReport);
                }
            }
        }
    }

    /**
     *
     * @param reservationRequestModel to be validated
     * @param errors                  to be filled with errors
     * @param securityToken           to be used for validation
     * @param reservationService      to be used for validation
     * @param request
     * @return true whether validation succeeds, otherwise false
     */
    public static boolean validate(ReservationRequestModel reservationRequestModel, Errors errors,
            SecurityToken securityToken, ReservationService reservationService, HttpServletRequest request)
    {
        UserSession userSession = UserSession.getInstance(request);
        ReservationRequestValidator validator = new ReservationRequestValidator(securityToken, reservationService,
                userSession.getLocale(), userSession.getTimeZone());
        validator.validate(reservationRequestModel, errors);
        return !errors.hasErrors();
    }

    /**
     * @param reservationRequestModel to get validated participants
     * @param errors
     * @return true whether validation succeeds, otherwise false
     */
    public static boolean validateParticipants(ReservationRequestModel reservationRequestModel, Errors errors)
    {
        if (reservationRequestModel.isRoomParticipantNotificationEnabled()) {
            ValidationUtils.rejectIfEmptyOrWhitespace(errors, "roomMeetingName", "validation.field.required");
        }
        return !errors.hasErrors();
    }
}
