package cz.cesnet.shongo.connector.api;

import cz.cesnet.shongo.api.util.DeviceAddress;
import org.joda.time.Duration;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Represents a configuration for single connector instance.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class ConnectorConfiguration
{
    /**
     * @return connector agent name
     */
    public abstract String getAgentName();

    /**
     * @return connector class
     */
    public abstract Class<? extends CommonService> getConnectorClass();

    /**
     * @return {@link DeviceConfiguration} for managed device
     */
    public DeviceConfiguration getDeviceConfiguration()
    {
        return null;
    }

    /**
     * @param option
     * @return value for given {@code option}
     */
    public abstract String getOptionString(String option);

    /**
     * @param option
     * @return list of option configurations
     */
    public abstract List<Configuration> getOptionConfigurationList(String option);

    /**
     * @param option
     * @return value for given {@code option} which must be not null
     */
    public String getOptionStringRequired(String option)
    {
        String value = getOptionString(option);
        if (value == null) {
            throw new IllegalArgumentException(
                    "Option '" + option + "' must be set in connector '" + getAgentName() + "' configuration.");
        }
        return value;
    }

    /**
     * @param option
     * @return value for given {@code option} which must be not null
     */
    public URL getOptionURLRequired(String option)
    {
        String value = getOptionString(option);
        if (value == null) {
            throw new IllegalArgumentException(
                    "Option '" + option + "' must be set in connector '" + getAgentName() + "' configuration.");
        }
        try {
            return new URL(value);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Given URL is malformed (option: " + option + ")", e);
        }
    }

    /**
     * @param option
     * @param defaultValue
     * @return value of given {@code option}
     */
    public String getOptionString(String option, String defaultValue)
    {
        String value = getOptionString(option);
        if (value == null) {
            return defaultValue;
        }
        else {
            return value;
        }
    }

    /**
     * @param option
     * @param defaultValue
     * @return value of given {@code option}
     */
    public int getOptionInt(String option, int defaultValue)
    {
        String value = getOptionString(option);
        if (value != null) {
            return Integer.parseInt(value);
        }
        else {
            return defaultValue;
        }
    }

    /**
     * @param option
     * @return value of given {@code option}
     */
    public int getOptionIntRequired(String option)
    {
        return Integer.parseInt(getOptionStringRequired(option));
    }

    /**
     * @param option
     * @return value of given {@code option}
     */
    public boolean getOptionBool(String option)
    {
        String value = getOptionString(option);
        if (value != null) {
            return Boolean.parseBoolean(value);
        }
        else {
            return false;
        }
    }

    /**
     * @param option
     * @param defaultValue
     * @return value of given {@code option}
     */
    public Duration getOptionDuration(String option, Duration defaultValue)
    {
        String duration = getOptionString(option);
        if (duration != null) {
            return Duration.parse(duration);
        }
        return defaultValue;
    }

    /**
     * @param option
     * @return value of given {@code option}
     */
    public Pattern getOptionPattern(String option)
    {
        String value = getOptionString(option);
        if (value != null) {
            return Pattern.compile(value);
        }
        return null;
    }
}
