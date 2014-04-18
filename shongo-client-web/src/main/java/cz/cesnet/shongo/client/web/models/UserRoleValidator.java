package cz.cesnet.shongo.client.web.models;

import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

/**
 * {@link Validator} for {@link UserRoleModel}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class UserRoleValidator implements Validator
{
    @Override
    public boolean supports(Class<?> type)
    {
        return UserRoleModel.class.equals(type);
    }

    @Override
    public void validate(Object object, Errors errors)
    {
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "identityPrincipalId", "validation.field.required");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "role", "validation.field.required");
    }
}
