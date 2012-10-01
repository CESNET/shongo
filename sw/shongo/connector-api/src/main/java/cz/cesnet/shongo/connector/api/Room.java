package cz.cesnet.shongo.connector.api;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a virtual room on a multipoint server device.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class Room
{
    private String name = null;
    private int licenseCount = -1;
    private Date startTime = null;
    private Date endTime = null;
    private Map<String, Object> options = new HashMap<String, Object>();


    public Room()
    {
    }

    public Room(String name, int licenseCount)
    {
        if (name == null) {
            throw new NullPointerException("name");
        }
        if (licenseCount < 0) {
            throw new IllegalArgumentException("licenseCount must be non-negative");
        }

        this.name = name;
        this.licenseCount = licenseCount;
    }

    /**
     * @return name of the room
     */
    public String getName()
    {
        return name;
    }

    /**
     * @param name    name of the room
     */
    public void setName(String name)
    {
        if (name == null) {
            throw new NullPointerException("name");
        }

        this.name = name;
    }

    /**
     * @return number of licenses that multipoint server can utilize for this room
     */
    public int getLicenseCount()
    {
        return licenseCount;
    }

    /**
     * @param licenseCount number of licenses that multipoint server can utilize for this room
     */
    public void setLicenseCount(int licenseCount)
    {
        if (licenseCount < 0) {
            throw new IllegalArgumentException("licenseCount must be non-negative");
        }

        this.licenseCount = licenseCount;
    }

    /**
     * @return time when the room shall start, or null if it shall start immediately
     */
    public Date getStartTime()
    {
        return startTime;
    }

    /**
     * @param startTime    time when the room shall start, or null if it shall start immediately
     */
    public void setStartTime(Date startTime)
    {
        this.startTime = startTime;
    }

    /**
     * @return time when the room shall end, or null if the room shall not ever stop
     */
    public Date getEndTime()
    {
        return endTime;
    }

    /**
     * @param endTime    time when the room shall end, or null if the room shall not ever stop
     */
    public void setEndTime(Date endTime)
    {
        this.endTime = endTime;
    }

    /**
     * Returns the complete map of platform-specific options set for this room.
     *
     * See the setOptions() method for more details about what can be had.
     *
     * @return platform-specific options
     */
    public Map<String, Object> getOptions()
    {
        return Collections.unmodifiableMap(options);
    }

    /**
     * Sets platform-specific options for this room.
     *
     * There are string option names mapped to some values. The following might be recognized by some connectors
     * (in parentheses, the default value is specified, which the connector should apply when the option is not set):
     * - listedPublicly             Boolean (false) whether to list the room in public lists
     * - allowContent               Boolean (true)  whether participants may contribute content
     * - allowGuests                Boolean (true)  whether guests should be allowed to join
     * - joinAudioMuted             Boolean (false) whether audio should be muted on join
     * - joinVideoMuted             Boolean (false) whether video should be muted on join
     * - roomNumber                 String          number under which the room can be called
     * - registerWithH323Gatekeeper Boolean (false) whether to register the roomNumber with the gatekeeper
     * - registerWithSIPRegistrar   Boolean (false) whether to register the roomNumber with the SIP registrar
     * - pin                        String          the PIN that must be entered to get to the room
     * - description                String          some description of the room
     * - startLocked                Boolean (false) whether the room should be locked when started
     * - conferenceMeEnabled        Boolean (false) whether the ConferenceMe should be enabled for the room
     *
     * Note that no option name should be the same as any of this class's attribute names.
     *
     * The list above must be kept in sync with the validateOption() method.
     *
     * @param options    platform-specific options
     * @throws IllegalArgumentException if a value is not of the type required by the corresponding option
     */
    public void setOptions(Map<String, Object> options)
    {
        for (Map.Entry<String, Object> entry : options.entrySet()) {
            validateOption(entry.getKey(), entry.getValue());
        }
        this.options = options;
    }

    /**
     * Finds out whether a given platform-specific option is set.
     *
     * See the setOptions() method for more details about available options.
     *
     * @param option    option name
     * @return true if option with the given name is set, false if not
     */
    public boolean hasOption(String option)
    {
        return options.containsKey(option);
    }

    /**
     * Returns the value of a platform-specific option.
     *
     * See the setOptions() method for more details about available options.
     *
     * @param option          option name
     * @return value of option, or null if the option is not set
     */
    public Object getOption(String option)
    {
        return options.get(option);
    }

    /**
     * Returns the value of a platform-specific option if it is set, or default value if the option is not set.
     *
     * See the setOptions() method for more details about available options.
     *
     * @param option          option name
     * @param defaultValue    default value to return if the option is not set
     * @return value of option, or defaultValue if the option is not set
     */
    public Object getOption(String option, Object defaultValue)
    {
        Object value = options.get(option);
        return (value == null ? defaultValue : value);
    }

    /**
     * Sets a single platform-specific option
     *
     * See the setOptions() method for more details about available options.
     *
     * @param option    option name
     * @param value     value to be set; or null to unset the option
     * @throws IllegalArgumentException if the value is not of the type required by the specified option
     */
    public void setOption(String option, Object value)
    {
        if (value == null) {
            unsetOption(option);
        }
        else {
            validateOption(option, value);
            options.put(option, value);
        }
    }

    /**
     * Validates that a given platform-specific option has the correct type of value.
     *
     * Must be kept in sync with the list of options in docs of setOptions() method.
     *
     * @param option    option name
     * @param value     value to be set
     * @throws IllegalArgumentException if the value is not of the type required by the specified option
     */
    private static void validateOption(String option, Object value)
    {
        if (option.equals("listedPublicly")) {
            assertInstance(option, value, Boolean.class);
        }
        else if (option.equals("allowContent")) {
            assertInstance(option, value, Boolean.class);
        }
        else if (option.equals("allowGuests")) {
            assertInstance(option, value, Boolean.class);
        }
        else if (option.equals("joinAudioMuted")) {
            assertInstance(option, value, Boolean.class);
        }
        else if (option.equals("joinVideoMuted")) {
            assertInstance(option, value, Boolean.class);
        }
        else if (option.equals("roomNumber")) {
            assertInstance(option, value, String.class);
        }
        else if (option.equals("registerWithH323Gatekeeper")) {
            assertInstance(option, value, Boolean.class);
        }
        else if (option.equals("registerWithSIPRegistrar")) {
            assertInstance(option, value, Boolean.class);
        }
        else if (option.equals("pin")) {
            assertInstance(option, value, String.class);
        }
        else if (option.equals("description")) {
            assertInstance(option, value, String.class);
        }
        else if (option.equals("startLocked")) {
            assertInstance(option, value, Boolean.class);
        }
        else if (option.equals("conferenceMeEnabled")) {
            assertInstance(option, value, Boolean.class);
        }
        else {
            throw new IllegalArgumentException("Unknown option: " + option);
        }
    }

    private static void assertInstance(String option, Object obj, Class clazz)
    {
        if (!clazz.isInstance(obj)) {
            throw new IllegalArgumentException("Option " + option + " requires value of class " + clazz.getName());
        }
    }

    /**
     * Unsets a single platform-specific option.
     *
     * See the setOptions() method for more details about available options.
     *
     * @param option    option name
     */
    public void unsetOption(String option)
    {
        options.remove(option);
    }

}
