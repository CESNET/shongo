package cz.cesnet.shongo.client.web.models;

import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.rpc.ReservationService;
import org.joda.time.DateTime;
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

        ReservationRequestModel.SpecificationType specificationType = reservationRequestModel.getSpecificationType();
        if (specificationType != null) {
            switch (specificationType) {
                case ADHOC_ROOM:
                case PERMANENT_ROOM_CAPACITY:
                    ValidationUtils.rejectIfEmptyOrWhitespace(errors, "durationCount", "validation.field.required");
                    ValidationUtils.rejectIfEmptyOrWhitespace(
                            errors, "roomParticipantCount", "validation.field.required");
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
                            errors, "permanentRoomCapacityReservationRequestId", "validation.field.required");
                    break;
            }
        }

        if (errors.hasErrors()) {
            return;
        }

        if (specificationType != null) {
            switch (specificationType) {
                case PERMANENT_ROOM:
                    Object isSpecificationAvailable = reservationService.checkAvailableSpecification(securityToken,
                            reservationRequestModel.getSlot(), reservationRequestModel.toSpecificationApi());
                    if (!Boolean.TRUE.equals(isSpecificationAvailable)) {
                        errors.rejectValue("permanentRoomName", "validation.field.permanentRoomNameNotAvailable");
                    }
                    break;
                case PERMANENT_ROOM_CAPACITY:
                    Object isProvidedReservationAvailableAvailable =
                            reservationService.checkAvailableProvidedReservationRequest(securityToken,
                                    reservationRequestModel.getSlot(),
                                    reservationRequestModel.getPermanentRoomCapacityReservationRequestId());
                    if (!isProvidedReservationAvailableAvailable.equals(Boolean.TRUE)) {
                        errors.rejectValue("permanentRoomCapacityReservationRequestId",
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
