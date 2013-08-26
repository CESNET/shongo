package cz.cesnet.shongo.client.web.models;

import com.google.common.base.Strings;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.request.AvailabilityCheckRequest;
import cz.cesnet.shongo.controller.api.rpc.ReservationService;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

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

    public ReservationRequestValidator(SecurityToken securityToken, ReservationService reservationService)
    {
        this.securityToken = securityToken;
        this.reservationService = reservationService;
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

        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "purpose", "validation.field.required");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "description", "validation.field.required");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "technology", "validation.field.required");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "start", "validation.field.required");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "specificationType", "validation.field.required");

        SpecificationType specificationType = reservationRequestModel.getSpecificationType();
        if (specificationType != null) {
            switch (specificationType) {
                case ADHOC_ROOM:
                case PERMANENT_ROOM_CAPACITY:
                    ValidationUtils.rejectIfEmptyOrWhitespace(errors, "durationCount", "validation.field.required");
                    ValidationUtils.rejectIfEmptyOrWhitespace(
                            errors, "roomParticipantCount", "validation.field.required");
                    Integer roomParticipantCount = reservationRequestModel.getRoomParticipantCount();
                    if (roomParticipantCount != null && roomParticipantCount <= 0) {
                        errors.rejectValue("roomParticipantCount", "validation.field.invalidCount");
                    }
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
                    ValidationUtils.rejectIfEmptyOrWhitespace(errors, "permanentRoomName", "validation.field.required");
                    break;
                case PERMANENT_ROOM_CAPACITY:
                    ValidationUtils.rejectIfEmptyOrWhitespace(
                            errors, "permanentRoomReservationRequestId", "validation.field.required");
                    break;
            }
        }

        if (errors.hasErrors()) {
            return;
        }

        if (specificationType != null) {
            AvailabilityCheckRequest availabilityCheckRequest = new AvailabilityCheckRequest(securityToken);
            availabilityCheckRequest.setSlot(reservationRequestModel.getSlot());
            if (!Strings.isNullOrEmpty(reservationRequestModel.getId())) {
                availabilityCheckRequest.setProvidedReservationRequestId(reservationRequestModel.getId());
            }
            switch (specificationType) {
                case PERMANENT_ROOM:
                    // Check if room name is available
                    availabilityCheckRequest.setSpecification(reservationRequestModel.toSpecificationApi());
                    Object isSpecificationAvailable = reservationService.checkAvailability(availabilityCheckRequest);
                    if (!Boolean.TRUE.equals(isSpecificationAvailable)) {
                        logger.warn("Validation of room availability failed, may be another problem: {}", isSpecificationAvailable);
                        errors.rejectValue("permanentRoomName", "validation.field.permanentRoomNameNotAvailable");
                    }
                    break;
                case PERMANENT_ROOM_CAPACITY:
                    // Check if permanent room is available
                    availabilityCheckRequest.setReservationRequestId(
                            reservationRequestModel.getPermanentRoomReservationRequestId());
                    Object isProvidedReservationAvailableAvailable =
                            reservationService.checkAvailability(availabilityCheckRequest);
                    if (!isProvidedReservationAvailableAvailable.equals(Boolean.TRUE)) {
                        logger.warn("Validation of reservation availability failed, may be another problem: {}", isProvidedReservationAvailableAvailable);
                        errors.rejectValue("permanentRoomReservationRequestId",
                                "validation.field.permanentRoomNotAvailable");
                    }
                    break;
            }
        }
    }

    /**
     * @param reservationRequestModel to be validated
     * @param errors                  to be filled with errors
     * @param securityToken           to be used for validation
     * @param reservationService      to be used for validation
     * @return true whether validation succeeds, otherwise false
     */
    public static boolean validate(ReservationRequestModel reservationRequestModel, Errors errors,
            SecurityToken securityToken, ReservationService reservationService)
    {
        ReservationRequestValidator validator = new ReservationRequestValidator(securityToken, reservationService);
        validator.validate(reservationRequestModel, errors);
        return !errors.hasErrors();
    }
}
