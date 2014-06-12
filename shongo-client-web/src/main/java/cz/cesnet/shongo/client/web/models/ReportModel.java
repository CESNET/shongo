package cz.cesnet.shongo.client.web.models;

import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.client.web.auth.OpenIDConnectAuthenticationToken;
import cz.cesnet.shongo.controller.ControllerConnectException;
import cz.cesnet.shongo.controller.api.rpc.CommonService;
import net.tanesha.recaptcha.ReCaptcha;
import net.tanesha.recaptcha.ReCaptchaImpl;
import net.tanesha.recaptcha.ReCaptchaResponse;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ValidationUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents a report problem model.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReportModel
{
    private static Logger logger = LoggerFactory.getLogger(ReportModel.class);

    private ReCaptcha reCaptcha;

    private String email;

    private boolean emailReadOnly;

    private String message;

    private final Context context;

    public ReportModel(String requestUri, ReCaptcha reCaptcha, CommonService commonService)
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
        this.context = new Context(requestUri, commonService);
    }

    public ReportModel(ErrorModel errorModel, ReCaptcha reCaptcha, CommonService commonService)
    {
        this(errorModel.getRequestUri(), reCaptcha, commonService);
        setError(errorModel);
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

    public Context getContext()
    {
        return context;
    }

    public void setError(ErrorModel errorModel)
    {
        context.errorModel = errorModel;
    }

    public ErrorModel getError()
    {
        return context.errorModel;
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

    public String getEmailSubject()
    {
        String subject = "Problem report";
        if (context.errorModel != null) {
            subject = context.errorModel.getEmailSubject() + " - User report";
        }
        return subject;
    }

    public String getEmailContent(HttpServletRequest request)
    {
        StringBuilder contentBuilder = new StringBuilder();
        contentBuilder.append("From: ");
        contentBuilder.append(email);
        contentBuilder.append("\n\n");
        contentBuilder.append(message);
        contentBuilder.append("\n\n");
        contentBuilder.append("--------------------------------------------------------------------------------\n\n");
        contentBuilder.append(context.toString(request));
        return contentBuilder.toString();
    }

    /**
     * Object which implements this interface can customize how it will be printed in {@link Context}.
     */
    public interface ContextSerializable
    {
        /**
         * @return string which should be used in {@link Context} for this object
         */
        public String toContextString();
    }

    /**
     * Represents a printable context for {@link ReportModel}.
     */
    public class Context
    {
        private String url;

        private boolean controllerAvailable;

        private ErrorModel errorModel;

        public Context(String url, CommonService commonService)
        {
            this.url = url;
            try {
                commonService.getController();
                controllerAvailable = true;
            }
            catch (ControllerConnectException exception) {
                controllerAvailable = false;
            }
            catch (Exception exception) {
                logger.warn("Checking controller state failed.", exception);
            }
        }

        public String getUrl()
        {
            return url;
        }

        public String toString(HttpServletRequest request)
        {
            StringBuilder contextBuilder = new StringBuilder();

            // Common attributes
            Map<String, Object> attributes = new LinkedHashMap<String, Object>();
            attributes.put("Controller", (controllerAvailable ? "available" : "not-available"));
            if (url != null) {
                attributes.put("URL", url);
            }
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication instanceof OpenIDConnectAuthenticationToken) {
                UserInformation userInformation = ((OpenIDConnectAuthenticationToken) authentication).getPrincipal();
                attributes.put("User", userInformation.getFullName() + " (" + userInformation.getPrimaryEmail() + ")");
            }
            UserSession userSession = UserSession.getInstance(request);
            if (userSession != null) {
                attributes.put("Language", userSession.getLocale());
                attributes.put("Timezone", userSession.getTimeZone());
                attributes.put("Administrator", userSession.isAdministrationMode());
                attributes.put("Advanced UI", userSession.isAdvancedUserInterface());
            }
            contextBuilder.append(formatAttributes(attributes));

            // Session attributes
            HttpSession httpSession = request.getSession();
            Enumeration<String> attributeNames = httpSession.getAttributeNames();
            while (attributeNames.hasMoreElements()) {
                String attributeName = attributeNames.nextElement();
                Object attributeValue = httpSession.getAttribute(attributeName);
                if (attributeValue == null
                        || attributeValue instanceof ReportModel
                        || attributeValue instanceof ErrorModel
                        || attributeValue instanceof UserSession
                        || attributeValue instanceof SecurityContext) {
                    continue;
                }

                String attributeContent;
                if (attributeValue instanceof ContextSerializable) {
                    attributeContent = ((ContextSerializable) attributeValue).toContextString();
                }
                else {
                    attributeContent = attributeValue.toString();
                }
                if (attributeContent == null || attributeContent.isEmpty()) {
                    continue;
                }

                attributeContent = attributeContent.replaceAll("\n", "\n  ");
                if (contextBuilder.length() > 0) {
                    contextBuilder.append("\n\n");
                }
                contextBuilder.append(attributeName);
                contextBuilder.append(":\n  ");
                contextBuilder.append(attributeContent);
            }

            if (errorModel != null) {
                contextBuilder.append("\n\n");
                contextBuilder.append(errorModel.getContent());
            }

            return contextBuilder.toString();
        }
    }

    public static String formatAttributes(Map<String, Object> attributes)
    {
        int maxAttributeKeyWidth = 0;
        for (Map.Entry<String, Object> attribute : attributes.entrySet()) {
            int keyLength = attribute.getKey().length();
            if (keyLength > maxAttributeKeyWidth) {
                maxAttributeKeyWidth = keyLength;
            }
        }

        StringBuilder stringBuilder = new StringBuilder();
        for (Map.Entry<String, Object> attribute : attributes.entrySet()) {
            String key = attribute.getKey();
            Object value = attribute.getValue();
            if (value == null) {
                continue;
            }
            String stringValue;
            if (value instanceof Boolean) {
                stringValue = (Boolean.TRUE.equals(value) ? "yes" : "no");
            }
            else if (value instanceof DateTimeZone) {
                stringValue = TimeZoneModel.formatTimeZone((DateTimeZone) value);
            }
            else {
                stringValue = value.toString();
            }
            if (stringValue.isEmpty()) {
                continue;
            }
            if (stringBuilder.length() > 0) {
                stringBuilder.append("\n");
            }
            for (int indent = 0; indent < (maxAttributeKeyWidth - key.length()); indent++) {
                stringBuilder.append(" ");
            }
            stringBuilder.append(key);
            stringBuilder.append(": ");
            stringBuilder.append(stringValue);
        }
        return stringBuilder.toString();
    }
}
