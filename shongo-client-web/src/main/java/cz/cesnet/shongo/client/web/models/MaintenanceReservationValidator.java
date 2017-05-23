package cz.cesnet.shongo.client.web.models;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;

/**
 * @author Marek Perichta <mperichta@cesnet.cz>
 */
public class MaintenanceReservationValidator extends GeneralValidator{

    private static Logger logger = LoggerFactory.getLogger(MaintenanceReservationValidator.class);


    @Override
    public boolean supports(Class<?> type)
    {
        return MaintenanceReservationValidator.class.equals(type);
    }

    @Override
    public void validate(Object object, Errors errors) {
        MaintenanceReservationModel reservationRequest = (MaintenanceReservationModel) object;
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "start", "validation.field.required");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "startDate", "validation.field.required");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "end", "validation.field.required");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "endDate", "validation.field.required");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "priority", "validation.field.required");

        validatePositiveNum("priority", errors);

        try {

            if (reservationRequest.getStartDateTime().isAfter(reservationRequest.getEndDateTime())) {
                errors.rejectValue("endDate", "validation.field.invalidInterval");
            }
            if (reservationRequest.getEndDateTime().isBefore(DateTime.now())) {
                errors.rejectValue("endDate", "validation.field.invalidFutureSlot");
            }
        }
        catch (Exception exception) {
            if (!errors.hasErrors()) {
                throw new IllegalStateException("Time slot cannot be checked.", exception);
            }
        }
    }
}
