package cz.cesnet.shongo.client.web.models;

import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.client.web.auth.OpenIDConnectAuthenticationToken;
import net.tanesha.recaptcha.ReCaptcha;
import net.tanesha.recaptcha.ReCaptchaResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ValidationUtils;

import javax.servlet.http.HttpServletRequest;

/**
 * Represents a report problem model.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReportModel
{
    private String email;

    private boolean emailReadOnly;

    private String message;

    private ReCaptcha reCaptcha;

    public ReportModel(ReCaptcha reCaptcha)
    {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof OpenIDConnectAuthenticationToken) {
            UserInformation userInformation = (UserInformation) authentication.getPrincipal();
            email = userInformation.getPrimaryEmail();
            emailReadOnly = true;
        }
        else {
            this.reCaptcha = reCaptcha;
        }
    }

    public boolean isEmailReadOnly()
    {
        return emailReadOnly;
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

    public ReCaptcha getReCaptcha()
    {
        return reCaptcha;
    }

    public void setReCaptcha(ReCaptcha reCaptcha)
    {
        this.reCaptcha = reCaptcha;
    }

    public void validate(BindingResult bindingResult, HttpServletRequest request)
    {
        if (reCaptcha != null) {
            String reCaptchaChallenge = request.getParameter("recaptcha_challenge_field");
            String reCaptchaResponse = request.getParameter("recaptcha_response_field");
            String remoteAddress = request.getRemoteAddr();
            ReCaptchaResponse response = reCaptcha.checkAnswer(remoteAddress, reCaptchaChallenge, reCaptchaResponse);
            if (!response.isValid()) {
                bindingResult.rejectValue("reCaptcha", "validation.field.invalidCaptcha");
            }
            else {
                // reCaptcha is already validated
                reCaptcha = null;
            }
        }

        CommonModel.validateEmail(bindingResult, "email", "validation.field.invalidEmail");
        ValidationUtils.rejectIfEmptyOrWhitespace(bindingResult, "message", "validation.field.required");
    }
}
