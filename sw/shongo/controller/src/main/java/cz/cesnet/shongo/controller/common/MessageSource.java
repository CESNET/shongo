package cz.cesnet.shongo.controller.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a source of translatable messages.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class MessageSource
{
    private String baseFileName;

    private Locale locale;

    private Map<Locale, ResourceBundle> resourceBundleByLocale = new HashMap<Locale, ResourceBundle>();

    public MessageSource(String baseFileName)
    {
        this.baseFileName = baseFileName;
    }

    public MessageSource(String baseFileName, Locale locale)
    {
        this.baseFileName = baseFileName;
        this.locale = locale;
    }

    public void setLocale(Locale locale)
    {
        this.locale = locale;
    }

    public String getMessage(String code, Locale locale)
    {
        ResourceBundle resourceBundle = getResourceBundle(locale);
        return resourceBundle.getString(code);
    }

    public String getMessage(String code)
    {
        if (locale == null) {
            throw new IllegalStateException("Default locale is not set.");
        }
        ResourceBundle resourceBundle = getResourceBundle(locale);
        return resourceBundle.getString(code);
    }

    public String getMessage(String code, Object... arguments)
    {
        if (locale == null) {
            throw new IllegalStateException("Default locale is not set.");
        }
        ResourceBundle resourceBundle = getResourceBundle(locale);
        return formatMessage(resourceBundle.getString(code), arguments);
    }

    public ResourceBundle getResourceBundle(Locale locale)
    {
        ResourceBundle resourceBundle = resourceBundleByLocale.get(locale);
        if (resourceBundle == null) {
            resourceBundle = new ReflectiveResourceBundle(ResourceBundle.getBundle(baseFileName, locale));
            resourceBundleByLocale.put(locale, resourceBundle);
        }
        return resourceBundle;
    }

    public static String formatMessage(String message, Object... arguments)
    {
        int argumentIndex = 0;
        for (Object argument : arguments) {
            message = message.replaceAll("\\{" + argumentIndex +"\\}", Matcher.quoteReplacement(argument.toString()));
            argumentIndex++;
        }
        return message;
    }

    private static class ReflectiveResourceBundle extends ResourceBundle
    {
        private static Logger logger = LoggerFactory.getLogger(ReflectiveResourceBundle.class);

        private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([\\w\\.\\-]+)\\}");

        private Map<String, String> cache = new HashMap<String, String>();

        private ResourceBundle resourceBundle;

        private ReflectiveResourceBundle(ResourceBundle resourceBundle)
        {
            this.resourceBundle = resourceBundle;
        }

        @Override
        protected Object handleGetObject(String code)
        {
            String message = cache.get(code);
            if (message != null) {
                return message;
            }
            message = resourceBundle.getString(code);
            if (message == null) {
                return null;
            }
            try {
                message = new String(message.getBytes("ISO-8859-1"), "UTF-8");
            }
            catch (UnsupportedEncodingException exception) {
                throw new RuntimeException("Encoding not supported", exception);
            }
            message = evaluateMessage(message);
            cache.put(code, message);
            return message;
        }

        @Override
        public Enumeration<String> getKeys()
        {
            return resourceBundle.getKeys();
        }

        /**
         * @param message to be evaluated
         * @return given {@code message} with evaluated self-referencing variables
         */
        private String evaluateMessage(String message)
        {
            if (message == null) {
                return null;
            }
            StringBuffer stringBuffer = new StringBuffer();
            Matcher matcher = VARIABLE_PATTERN.matcher(message);
            while (matcher.find()) {
                String subCode = matcher.group(1);
                String subMessage = getString(subCode);
                matcher.appendReplacement(stringBuffer, (subMessage != null ? subMessage : ("\\${" + subCode + "}")));
            }
            matcher.appendTail(stringBuffer);
            String evaluatedMessage = stringBuffer.toString();
            //logger.debug("Evaluated message from '{}' to '{}'.", message, evaluatedMessage);
            return evaluatedMessage;
        }
    }
}
