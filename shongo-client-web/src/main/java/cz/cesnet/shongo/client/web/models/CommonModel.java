package cz.cesnet.shongo.client.web.models;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.ReadablePartial;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
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
     * Prefix for new unique identifiers.
     */
    private static final String NEW_ID_PREFIX = "new-";

    /**
     * Email pattern.
     */
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$");

    /**
     * Last auto-generated identifier index.
     */
    private static int lastGeneratedId = 0;

    /**
     * @param id to be checked
     * @return true whether given {@code id} is auto-generated, false otherwise
     */
    public synchronized static boolean isNewId(String id)
    {
        return id.startsWith(NEW_ID_PREFIX);
    }

    /**
     * @return new auto-generated identifier
     */
    public synchronized static String getNewId()
    {
        return NEW_ID_PREFIX + String.valueOf(++lastGeneratedId);
    }

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

    /**
     * Log validation errors.
     *
     * @param logger
     * @param errors
     */
    public static void logValidationErrors(Logger logger, Errors errors)
    {
        logger.debug("Validation errors {}", errors);
    }
}
