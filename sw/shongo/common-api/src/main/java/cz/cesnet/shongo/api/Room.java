package cz.cesnet.shongo.api;

import jade.content.Concept;

import java.util.*;

/**
 * Represents a virtual room on a multipoint server device.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class Room implements Concept
{
    private String name = null;
    private int portCount = -1;
    private List<Alias> aliases;

    private Map<String, Object> options = new HashMap<String, Object>();

    // FIXME: will startTime and endTime ever be used by Shongo?
    private Date startTime = null;
    private Date endTime = null;


    public Room()
    {
    }

    public Room(String name, int portCount)
    {
        if (name == null) {
            throw new NullPointerException("name");
        }
        if (portCount < 0) {
            throw new IllegalArgumentException("portCount must be non-negative");
        }

        this.name = name;
        this.portCount = portCount;
    }

    /**
     * @return name of the room
     */
    public String getName()
    {
        return name;
    }

    /**
     * @param name name of the room
     */
    public void setName(String name)
    {
        if (name == null) {
            throw new NullPointerException("name");
        }

        this.name = name;
    }

    /**
     * @return number of ports that multipoint server can utilize for this room
     */
    public int getPortCount()
    {
        return portCount;
    }

    /**
     * @param portCount number of ports that multipoint server can utilize for this room
     */
    public void setPortCount(int portCount)
    {
        if (portCount < 0) {
            throw new IllegalArgumentException("portCount must be non-negative");
        }

        this.portCount = portCount;
    }

    /**
     * @return time when the room shall start, or null if it shall start immediately
     */
    public Date getStartTime()
    {
        return startTime;
    }

    /**
     * @param startTime time when the room shall start, or null if it shall start immediately
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
     * @param endTime time when the room shall end, or null if the room shall not ever stop
     */
    public void setEndTime(Date endTime)
    {
        this.endTime = endTime;
    }

    /**
     * @return list of aliases under which the room is accessible
     */
    public List<Alias> getAliases()
    {
        return aliases;
    }

    /**
     * Adds a new alias under which the room is accessible.
     *
     * @param alias
     */
    public void addAlias(Alias alias)
    {
        if (aliases == null) {
            aliases = new ArrayList<Alias>();
        }
        aliases.add(alias);
    }

    /**
     * @param aliases aliases under which the room is accessible
     */
    public void setAliases(List<Alias> aliases)
    {
        this.aliases = aliases;
    }

    /**
     * Returns the complete map of platform-specific options set for this room.
     * <p/>
     * See the setOptions() method for more details about what can be had.
     *
     * @return platform-specific options
     */
    public Map<String, Object> getOptions()
    {
        return Collections.unmodifiableMap(options);
    }


    /**
     * Room attribute names.
     *
     * Suitable for modifyRoom() command.
     *
     * NOTE: No value should be the same as any of this class's option names (OPT_* constants).
     *
     * NOTE: Keep in sync with actual attributes of the class.
     */

    /**
     * Room name. Type: String
     */
    public static final String NAME = "name";
    /**
     * Number of ports to use for the room. Type: int
     */
    public static final String PORT_COUNT = "portCount";
    /**
     * Aliases of the room. Type: List<Alias>
     */
    public static final String ALIASES = "aliases";
    /**
     * Time of starting the room. Type: java.util.Date
     */
    public static final String START_TIME = "startTime";
    /**
     * Time of ending the room. Type: java.util.Date
     */
    public static final String END_TIME = "endTime";


    /**
     * Room options.
     *
     * Suitable for modifyRoom() command.
     *
     * NOTE: No option name should be the same as any of this class's attribute names (constant values above).
     *
     * NOTE: The option constants must be kept in sync with the validateOption() method.
     */

    /**
     * A boolean option whether to list the room in public lists. Default false.
     */
    public static final String OPT_LISTED_PUBLICLY = "listedPublicly";
    /**
     * A boolean option whether participants may contribute content. Default true.
     */
    public static final String OPT_ALLOW_CONTENT = "allowContent";
    /**
     * A boolean option whether guests should be allowed to join. Default true.
     */
    public static final String OPT_ALLOW_GUESTS = "allowGuests";
    /**
     * A boolean option whether audio should be muted on join. Default false.
     */
    public static final String OPT_JOIN_AUDIO_MUTED = "joinAudioMuted";
    /**
     * A boolean option whether video should be muted on join. Default false.
     */
    public static final String OPT_JOIN_VIDEO_MUTED = "joinVideoMuted";
    /**
     * A boolean option whether to register the aliases with the gatekeeper. Default false.
     */
    public static final String OPT_REGISTER_WITH_H323_GATEKEEPER = "registerWithH323Gatekeeper";
    /**
     * A boolean option whether to register the aliases with the SIP registrar. Default false.
     */
    public static final String OPT_REGISTER_WITH_SIP_REGISTRAR = "registerWithSIPRegistrar";
    /**
     * A string option - the PIN that must be entered to get to the room.
     */
    public static final String OPT_PIN = "pin";
    /**
     * A string option - some description of the room.
     */
    public static final String OPT_DESCRIPTION = "description";
    /**
     * A boolean option whether the room should be locked when started. Default false.
     */
    public static final String OPT_START_LOCKED = "startLocked";
    /**
     * A boolean option whether the ConferenceMe should be enabled for the room. Default false.
     */
    public static final String OPT_CONFERENCE_ME_ENABLED = "conferenceMeEnabled";

    /**
     * Sets platform-specific options for this room.
     * <p/>
     * There are option names mapped to some values. See the OPT_* constants for options that might be recognized.
     *
     * @param options platform-specific options
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
     * <p/>
     * See the OPT_* constants for available options.
     *
     * @param option option name
     * @return true if option with the given name is set, false if not
     */
    public boolean hasOption(String option)
    {
        return options.containsKey(option);
    }

    /**
     * Returns the value of a platform-specific option.
     * <p/>
     * See the OPT_* constants for available options.
     *
     * @param option option name
     * @return value of option, or null if the option is not set
     */
    public Object getOption(String option)
    {
        return options.get(option);
    }

    /**
     * Returns the value of a platform-specific option if it is set, or default value if the option is not set.
     * <p/>
     * See the OPT_* constants for available options.
     *
     * @param option       option name
     * @param defaultValue default value to return if the option is not set
     * @return value of option, or defaultValue if the option is not set
     */
    public Object getOption(String option, Object defaultValue)
    {
        Object value = options.get(option);
        return (value == null ? defaultValue : value);
    }

    /**
     * Sets a single platform-specific option
     * <p/>
     * See the OPT_* constants for available options.
     *
     * @param option option name
     * @param value  value to be set; or null to unset the option
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
     * <p/>
     * Must be kept in sync with OPT_* constants.
     *
     * @param option option name
     * @param value  value to be set
     * @throws IllegalArgumentException if the value is not of the type required by the specified option
     */
    private static void validateOption(String option, Object value)
    {
        if (option.equals(OPT_LISTED_PUBLICLY)) {
            assertInstance(option, value, Boolean.class);
        }
        else if (option.equals(OPT_ALLOW_CONTENT)) {
            assertInstance(option, value, Boolean.class);
        }
        else if (option.equals(OPT_ALLOW_GUESTS)) {
            assertInstance(option, value, Boolean.class);
        }
        else if (option.equals(OPT_JOIN_AUDIO_MUTED)) {
            assertInstance(option, value, Boolean.class);
        }
        else if (option.equals(OPT_JOIN_VIDEO_MUTED)) {
            assertInstance(option, value, Boolean.class);
        }
        else if (option.equals(OPT_REGISTER_WITH_H323_GATEKEEPER)) {
            assertInstance(option, value, Boolean.class);
        }
        else if (option.equals(OPT_REGISTER_WITH_SIP_REGISTRAR)) {
            assertInstance(option, value, Boolean.class);
        }
        else if (option.equals(OPT_PIN)) {
            assertInstance(option, value, String.class);
        }
        else if (option.equals(OPT_DESCRIPTION)) {
            assertInstance(option, value, String.class);
        }
        else if (option.equals(OPT_START_LOCKED)) {
            assertInstance(option, value, Boolean.class);
        }
        else if (option.equals(OPT_CONFERENCE_ME_ENABLED)) {
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
     * <p/>
     * See the OPT_* constants for available options.
     *
     * @param option option name
     */
    public void unsetOption(String option)
    {
        options.remove(option);
    }

}
