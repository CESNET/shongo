package cz.cesnet.shongo.connector.api;

import org.joda.time.Duration;
import org.joda.time.Period;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Device options for connectors.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public abstract class ConnectorOptions
{
    public abstract String getString(String key);

    public abstract List<String> getStringList(String key);

    public abstract List<ConnectorOptions> getOptionsList(String key);

    public String getString(String key, String defaultValue)
    {
        String value = getString(key);
        if (value == null) {
            return defaultValue;
        }
        else {
            return value;
        }
    }

    public boolean getBool(String key)
    {
        String value = getString(key);
        if (value != null) {
            return Boolean.parseBoolean(value);
        }
        else {
            return false;
        }
    }

    public int getInt(String key)
    {
        String value = getString(key);
        if (value != null) {
            return Integer.parseInt(value);
        }
        else {
            return 0;
        }
    }

    public int getInt(String key, int defaultValue)
    {
        String value = getString(key);
        if (value != null) {
            return Integer.parseInt(value);
        }
        else {
            return defaultValue;
        }
    }

    public Pattern getPattern(String key)
    {
        String h323Number = getString(key);
        if (h323Number != null) {
            return Pattern.compile(h323Number);
        }
        return null;
    }

    public Duration getDuration(String key, Duration defaultValue)
    {
        String duration = getString(key);
        if (duration != null) {
            return Duration.parse(duration);
        }
        return defaultValue;
    }
}
