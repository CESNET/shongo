package cz.cesnet.shongo.client.web.models;

import cz.cesnet.shongo.controller.api.AclRecord;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

/**
 * {@link Validator} for {@link AclRecord}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AclRecordValidator implements Validator
{
    @Override
    public boolean supports(Class<?> type)
    {
        return AclRecord.class.equals(type);
    }

    @Override
    public void validate(Object object, Errors errors)
    {
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "userId", "validation.field.required");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "entityId", "validation.field.required");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "role", "validation.field.required");
    }
}
