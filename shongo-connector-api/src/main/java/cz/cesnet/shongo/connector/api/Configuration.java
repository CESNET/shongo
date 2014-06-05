package cz.cesnet.shongo.connector.api;

import org.joda.time.Duration;

import java.util.regex.Pattern;

/**
 * Represents a configuration for single connector instance.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class Configuration
{
    /**
     * @param attribute
     * @return value for given {@code attribute}
     */
    public abstract String getString(String attribute);

    /**
     * @param attribute
     * @return value for given {@code attribute} which must be not null
     */
    public String getStringRequired(String attribute)
    {
        String value = getString(attribute);
        if (value == null) {
            throw new IllegalArgumentException(
                    "Attribute '" + attribute + "' must be set in configuration.");
        }
        return value;
    }

    /**
     * @param attribute
     * @param defaultValue
     * @return value of given {@code attribute}
     */
    public String getString(String attribute, String defaultValue)
    {
        String value = getString(attribute);
        if (value == null) {
            return defaultValue;
        }
        else {
            return value;
        }
    }

    /**
     * @param attribute
     * @param defaultValue
     * @return value of given {@code attribute}
     */
    public int getInt(String attribute, int defaultValue)
    {
        String value = getString(attribute);
        if (value != null) {
            return Integer.parseInt(value);
        }
        else {
            return defaultValue;
        }
    }

    /**
     * @param attribute
     * @return value of given {@code attribute}
     */
    public int getIntRequired(String attribute)
    {
        return Integer.parseInt(getStringRequired(attribute));
    }

    /**
     * @param attribute
     * @return value of given {@code attribute}
     */
    public boolean getBool(String attribute)
    {
        String value = getString(attribute);
        if (value != null) {
            return Boolean.parseBoolean(value);
        }
        else {
            return false;
        }
    }

    /**
     * @param attribute
     * @param defaultValue
     * @return value of given {@code attribute}
     */
    public Duration getDuration(String attribute, Duration defaultValue)
    {
        String duration = getString(attribute);
        if (duration != null) {
            return Duration.parse(duration);
        }
        return defaultValue;
    }

    /**
     * @param attribute
     * @return value of given {@code attribute}
     */
    public Pattern getOptionPattern(String attribute)
    {
        String value = getString(attribute);
        if (value != null) {
            return Pattern.compile(value);
        }
        return null;
    }
}
