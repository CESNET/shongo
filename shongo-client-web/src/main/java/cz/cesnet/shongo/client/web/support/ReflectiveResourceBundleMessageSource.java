package cz.cesnet.shongo.client.web.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@link ReloadableResourceBundleMessageSource} which allows for self-referential resource bundles.
 *
 * Example:
 *   my.bundle.user.name = Bob Smith
 *   my.bundle.user.state = Maine
 *   my.bundle.greeting = Hello, ${my.bundle.user.name}, I see you're from ${my.bundle.user.state}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReflectiveResourceBundleMessageSource extends ReloadableResourceBundleMessageSource
{
    private static Logger logger = LoggerFactory.getLogger(ReflectiveResourceBundleMessageSource.class);

    /**
     * Regex pattern for variables in message bundles.
     */
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([\\w\\.\\-]+)\\}");

    /**
     * Cached merged properties for not reloadable message source.
     */
    private final Map<Locale, PropertiesHolder> cachedMergedProperties = new HashMap<Locale, PropertiesHolder>();

    @Override
    protected PropertiesHolder getProperties(String filename)
    {
        PropertiesHolder propertiesHolder = super.getProperties(filename);
        injectReflectiveProperties(propertiesHolder);
        return propertiesHolder;
    }

    @Override
    protected PropertiesHolder getMergedProperties(Locale locale)
    {
        PropertiesHolder propertiesHolder = cachedMergedProperties.get(locale);
        if (propertiesHolder != null) {
            return propertiesHolder;
        }
        // Inject merged properties holder by reflective properties
        propertiesHolder = super.getMergedProperties(locale);
        injectReflectiveProperties(propertiesHolder);
        cachedMergedProperties.put(locale, propertiesHolder);
        return propertiesHolder;
    }

    /**
     * @param propertiesHolder to be injected as reflective
     */
    private void injectReflectiveProperties(PropertiesHolder propertiesHolder)
    {
        Properties properties = propertiesHolder.getProperties();
        if (properties instanceof ReflectiveProperties) {
            return;
        }

        ReflectiveProperties reflectiveProperties = new ReflectiveProperties(properties);
        try {
            logger.debug("Injecting reflective properties to properties holder...");
            Field field = PropertiesHolder.class.getDeclaredField("properties");
            field.setAccessible(true);
            field.set(propertiesHolder, reflectiveProperties);
        }
        catch (Exception exception) {
            throw new RuntimeException("Can't hook properties filed of PropertiesHolder.", exception);
        }
    }

    private class ReflectiveProperties extends Properties
    {
        private Map<String, String> cache = new HashMap<String, String>();

        private ReflectiveProperties(Properties defaults)
        {
            if (defaults != null) {
                putAll(defaults);
            }
        }

        @Override
        public String getProperty(String code)
        {
            String message = cache.get(code);
            if (message != null) {
                return message;
            }
            message = evaluateMessage(super.getProperty(code));
            cache.put(code, message);
            return message;
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
                String subMessage = getProperty(subCode);
                matcher.appendReplacement(stringBuffer, (subMessage != null ? subMessage : ("\\${" + subCode + "}")));
            }
            matcher.appendTail(stringBuffer);
            String evaluatedMessage = stringBuffer.toString();
            logger.debug("Evaluated message from '{}' to '{}'.", message, evaluatedMessage);
            return evaluatedMessage;
        }
    }
}
