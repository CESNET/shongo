package cz.cesnet.shongo.client.web.models;

import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.client.web.auth.OpenIDConnectAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ValidationUtils;

/**
 * Represents a report problem model.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReportModel
{
    private String email;

    private String message;

    public ReportModel()
    {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof OpenIDConnectAuthenticationToken) {
            UserInformation userInformation = (UserInformation) authentication.getPrincipal();
            email = userInformation.getPrimaryEmail();
        }
    }

    public String getEmail()
    {
        return email;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }

    public void validate(BindingResult bindingResult)
    {
        CommonModel.validateEmail(bindingResult, "email", "validation.field.invalidEmail");
        ValidationUtils.rejectIfEmptyOrWhitespace(bindingResult, "message", "validation.field.required");
    }
}
