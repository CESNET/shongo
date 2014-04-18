package cz.cesnet.shongo.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
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
    private static Logger logger = LoggerFactory.getLogger(ReflectiveResourceBundle.class);

    private static Map<String, ReflectiveResourceBundle> resourceBundles =
            new HashMap<String, ReflectiveResourceBundle>();

    private String baseFileName;

    private Locale locale;

    private Map<Locale, ReflectiveResourceBundle> resourceBundleByLocale =
            new HashMap<Locale, ReflectiveResourceBundle>();

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

    public String getMessage(String code, Locale locale, Object... arguments)
    {
        ReflectiveResourceBundle resourceBundle = getResourceBundle(locale);
        MessageFormat messageFormat = resourceBundle.getMessageFormat(code);
        return messageFormat.format(arguments);
    }

    public String getMessage(String code, Object... arguments)
    {
        if (locale == null) {
            throw new IllegalStateException("Default locale is not set.");
        }
        ReflectiveResourceBundle resourceBundle = getResourceBundle(locale);
        MessageFormat messageFormat = resourceBundle.getMessageFormat(code);
        return messageFormat.format(arguments);
    }

    public ReflectiveResourceBundle getResourceBundle(Locale locale)
    {
        ReflectiveResourceBundle resourceBundle = resourceBundleByLocale.get(locale);
        if (resourceBundle == null) {
            synchronized (MessageSource.class) {
                String key = baseFileName + "_" + locale;
                resourceBundle = resourceBundles.get(key);
                if (resourceBundle == null) {
                    logger.debug("Loading resource bundle '{}' for locale '{}'...", baseFileName, locale);
                    resourceBundle = new ReflectiveResourceBundle(ResourceBundle.getBundle(baseFileName, locale));
                    resourceBundles.put(key, resourceBundle);
                }
                else {
                    logger.debug("Using cached resource bundle '{}' for locale '{}'...", baseFileName, locale);
                }
            }
            resourceBundleByLocale.put(locale, resourceBundle);
        }
        return resourceBundle;
    }

    private static class ReflectiveResourceBundle extends ResourceBundle
    {
        private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([\\w\\.\\-]+)\\}");

        private final Map<String, String> stringCache = new HashMap<String, String>();

        private final Map<String, MessageFormat> messageFormatCache = new HashMap<String, MessageFormat>();

        private final ResourceBundle resourceBundle;

        private ReflectiveResourceBundle(ResourceBundle resourceBundle)
        {
            this.resourceBundle = resourceBundle;
        }

        public synchronized MessageFormat getMessageFormat(String code)
        {
            MessageFormat messageFormat = messageFormatCache.get(code);
            if (messageFormat == null) {
                String message = getString(code);
                messageFormat = new MessageFormat((message != null ? message : ""), getLocale());
                messageFormatCache.put(code, messageFormat);
            }
            return messageFormat;
        }

        @Override
        public Locale getLocale()
        {
            return resourceBundle.getLocale();
        }

        @Override
        protected synchronized Object handleGetObject(String code)
        {
            String message = stringCache.get(code);
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
            synchronized (stringCache) {
                stringCache.put(code, message);
            }
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
