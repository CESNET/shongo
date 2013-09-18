package cz.cesnet.shongo.client.web.models;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.ReadablePartial;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ValidationUtils;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Common validation methods.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class CommonModel
{
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

    /**
     * @param string
     * @return given {@code string} which can be used in double quoted string (e.g., "<string>")
     */
    public static String escapeDoubleQuotedString(String string)
    {
        if (string == null) {
            return null;
        }
        string = string.replaceAll("\n", "\\\\n");
        string = string.replaceAll("\"", "\\\\\"");
        return string;
    }
}
