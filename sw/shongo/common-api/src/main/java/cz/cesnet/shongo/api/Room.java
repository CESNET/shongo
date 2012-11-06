package cz.cesnet.shongo.api;

import cz.cesnet.shongo.api.xmlrpc.StructType;
import jade.content.Concept;

import java.util.*;

/**
 * Represents a virtual room on a multipoint server device.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class Room implements Concept, StructType
{
    private String identifier;
    private String name = null;
    private int portCount = -1;
    private List<Alias> aliases;

    private Map<Option, Object> options = new EnumMap<Option, Object>(Option.class);


    public Room()
    {
    }

    public Room(String name, int portCount)
    {
        if (name == null) {
            throw new NullPointerException("name");
        }
        if (portCount < 0) {
            throw new IllegalArgumentException("Port count must be non-negative");
        }

        this.name = name;
        this.portCount = portCount;
    }

    public String getIdentifier()
    {
        return identifier;
    }

    public void setIdentifier(String identifier)
    {
        this.identifier = identifier;
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
     * @return list of aliases under which the room is accessible
     */
    public List<Alias> getAliases()
    {
        return aliases;
    }

    /**
     * Adds a new alias under which the room is accessible.
     *
     * @param alias alias to add for the room
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
     * Returns the complete map of options set for this room.
     * <p/>
     * NOTE: Used by Jade to compose messages containing Room objects.
     *
     * @return options set for the room
     */
    public Map<Option, Object> getOptions()
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
     * Room options.
     */
    public static enum Option
    {
        /**
         * A string option - some description of the room.
         */
        DESCRIPTION("description", String.class),

        /**
         * A string option - the PIN that must be entered to get to the room.
         */
        PIN("pin", String.class),

        /**
         * A boolean option whether to list the room in public lists. Defaults to false.
         */
        LISTED_PUBLICLY("listedPublicly", Boolean.class),

        /**
         * A boolean option whether participants may contribute content. Defaults to true.
         */
        ALLOW_CONTENT("allowContent", Boolean.class),

        /**
         * A boolean option whether guests should be allowed to join. Defaults to true.
         */
        ALLOW_GUESTS("allowGuests", Boolean.class),

        /**
         * A boolean option whether audio should be muted on join. Defaults to false.
         */
        JOIN_AUDIO_MUTED("joinAudioMuted", Boolean.class),

        /**
         * A boolean option whether video should be muted on join. Defaults to false.
         */
        JOIN_VIDEO_MUTED("joinVideoMuted", Boolean.class),

        /**
         * A boolean option whether to register the aliases with the gatekeeper. Defaults to false.
         */
        REGISTER_WITH_H323_GATEKEEPER("registerWithH323Gatekeeper", Boolean.class),

        /**
         * A boolean option whether to register the aliases with the SIP registrar. Defaults to false.
         */
        REGISTER_WITH_SIP_REGISTRAR("registerWithSIPRegistrar", Boolean.class),

        /**
         * A boolean option whether the room should be locked when started. Defaults to false.
         */
        START_LOCKED("startLocked", Boolean.class),

        /**
         * A boolean option whether the ConferenceMe should be enabled for the room. Defaults to false.
         */
        CONFERENCE_ME_ENABLED("conferenceMeEnabled", Boolean.class);

        private String name;
        private Class valueClass;

        private Option(String name, Class valueClass)
        {
            this.name = name;
            this.valueClass = valueClass;
        }

        public String getName()
        {
            return name;
        }

        public Class getValueClass()
        {
            return valueClass;
        }

        @Override
        public String toString()
        {
            return name;
        }
    }


    /**
     * Sets options for this room.
     *
     * @param options options to set for the room
     * @throws IllegalArgumentException if a value is not of the type required by the corresponding option
     */
    public void setOptions(Map<Option, Object> options)
    {
        for (Map.Entry<Option, Object> entry : options.entrySet()) {
            validateOption(entry.getKey(), entry.getValue());
        }
        this.options = options;
    }

    /**
     * Finds out whether a given option is set.
     *
     * @param option option to find
     * @return <code>true</code> if option with the given name is set, <code>false</code> if not
     */
    public boolean hasOption(Option option)
    {
        return options.containsKey(option);
    }

    /**
     * Returns the value of an option.
     *
     * @param option option to get value of
     * @return value of option, or <code>null</code> if the option is not set
     */
    public Object getOption(Option option)
    {
        return options.get(option);
    }

    /**
     * Returns the value of an option if it is set, or default value if the option is not set.
     *
     * @param option       option to get value of
     * @param defaultValue default value to return if the option is not set
     * @return value of option, or <code>defaultValue</code> if the option is not set
     */
    public Object getOption(Option option, Object defaultValue)
    {
        Object value = options.get(option);
        return (value == null ? defaultValue : value);
    }

    /**
     * Sets a single option.
     *
     * @param option option to set
     * @param value  value to be set; or <code>null</code> to unset the option
     * @throws IllegalArgumentException if the value is not of the type required by the specified option
     */
    public void setOption(Option option, Object value)
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
     * Validates that a given option has the correct type of value.
     *
     * @param option option to validate value of
     * @param value  value to be set
     * @throws IllegalArgumentException if the value is not of the type required by the specified option
     */
    private static void validateOption(Option option, Object value)
    {
        if (!option.getValueClass().isInstance(value)) {
            String message = "Option " + option + " requires value of class " + option.getValueClass().getName();
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Unsets a single option.
     *
     * @param option option to unset
     */
    public void unsetOption(Option option)
    {
        options.remove(option);
    }

}
