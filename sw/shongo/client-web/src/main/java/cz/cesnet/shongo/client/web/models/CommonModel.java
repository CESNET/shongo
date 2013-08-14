package cz.cesnet.shongo.client.web.models;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ValidationUtils;

import java.util.regex.Pattern;

/**
 * Common validation methods.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class CommonModel
{
    /**
     * Date/time formatters.
     */
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormat.forStyle("M-");
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forStyle("MS");

    /**
     * Email pattern.
     */
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$");

    /**
     * Validate email address.
     *
     * @param bindingResult
     * @param field
     * @param errorCode
     */
    public static void validateEmail(BindingResult bindingResult, String field, String errorCode)
    {
        String value = (String) bindingResult.getFieldValue(field);
        if (value != null) {
            value = value.trim();
        }
        if (StringUtils.isEmpty(value)) {
            bindingResult.rejectValue(field, "validation.field.required");
        }
        else if (!EMAIL_PATTERN.matcher(value).matches()) {
            bindingResult.rejectValue(field, errorCode);
        }
    }
}
