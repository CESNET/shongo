package cz.cesnet.shongo.client.web.models;

import com.google.common.base.Strings;
import cz.cesnet.shongo.client.web.ClientWebConfiguration;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Marek Perichta <mperichta@cesnet.cz>
 */
public abstract class GeneralValidator implements Validator {

    private static final Pattern PATTERN_IDENTIFIER = Pattern.compile("^[a-zA-Z0-9_-]*$");
    private static final Pattern PATTERN_ALPHA_NUM = Pattern.compile("^[a-zA-Z0-9]*$");
    private static final Pattern PATTERN_NUM = Pattern.compile("^[0-9]*$");
    private static final Pattern PATTERN_H323_E164_NUMBER = Pattern.compile(ClientWebConfiguration.getInstance().getE164Pattern());

    /**
     * @param field
     * @param errors
     * @return true whether validation succeeds, otherwise false
     */
    public static void validateIdentifier(String field, Errors errors)
    {
        String value = (String) errors.getFieldValue(field);
        Matcher matcher = PATTERN_IDENTIFIER.matcher(value);
        if (!matcher.matches()) {
            errors.rejectValue(field, "validation.field.invalidIdentifier");
        }
    }

    /**
     * @param field
     * @param errors
     * @return true whether validation succeeds, otherwise false
     */
    public static void validateAlphaNum(String field, Errors errors)
    {
        String value = (String) errors.getFieldValue(field);
        Matcher matcher = PATTERN_ALPHA_NUM.matcher(value);
        if (!matcher.matches()) {
            errors.rejectValue(field, "validation.field.invalidAlphaNum");
        }
    }

    /**
     * @param field
     * @param errors
     * @return true whether validation succeeds, otherwise false
     */
    public static void validateNum(String field, Errors errors)
    {
        String value = (String) errors.getFieldValue(field);
        Matcher matcher = PATTERN_NUM.matcher(value);
        if (!matcher.matches()) {
            errors.rejectValue(field, "validation.field.invalidNum");
        }
    }

    public static void validatePositiveNum(String field, Errors errors)
    {
        String value = (String) errors.getFieldValue(field);
        Matcher matcher = PATTERN_NUM.matcher(value);
        if (!matcher.matches()) {
            errors.rejectValue(field, "validation.field.invalidNum");
        } else {
            Integer number = Integer.parseInt(value);
            if (number < 1) {
                errors.rejectValue(field, "validation.field.invalidPositiveNum");
            }
        }
    }

    public static void validateE164Number(String field, Errors errors)
    {
        String value = (String) errors.getFieldValue(field);
        if (!Strings.isNullOrEmpty(value)) {
            Matcher matcher = PATTERN_H323_E164_NUMBER.matcher(value);
            if (!matcher.matches()) {
                errors.rejectValue(field, "validation.field.invalidNum.E164");
            }
        }
    }
}
