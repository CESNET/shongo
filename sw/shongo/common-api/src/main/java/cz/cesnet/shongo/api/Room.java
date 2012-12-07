package cz.cesnet.shongo.api;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.util.IdentifiedChangeableObject;
import cz.cesnet.shongo.api.xmlrpc.StructType;
import jade.content.Concept;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents a virtual room on a multipoint server device.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class Room extends IdentifiedChangeableObject implements StructType, Concept
{
    /**
     * Room name. Type: String
     */
    public static final String NAME = "name";

    /**
     * Set of {@link cz.cesnet.shongo.Technology}s for the room.
     */
    public static final String TECHNOLOGIES = "technologies";

    /**
     * Number of licenses to use for the room. Type: int
     */
    public static final String LICENSE_COUNT = "licenseCount";

    /**
     * Aliases of the room. Type: List<Alias>
     */
    public static final String ALIASES = "aliases";

    /**
     * Options of the room. Type: Map<Option, Object>
     */
    public static final String OPTIONS = "options";

    public Room()
    {
    }

    public Room(String name, int licenseCount)
    {
        if (name == null) {
            throw new NullPointerException("name");
        }
        if (licenseCount < 0) {
            throw new IllegalArgumentException("License count must be non-negative");
        }

        setName(name);
        setLicenseCount(licenseCount);
    }

    /**
     * @return name of the room
     */
    public String getName()
    {
        return getPropertyStorage().getValue(NAME);
    }

    /**
     * @param name name of the room
     */
    public void setName(String name)
    {
        if (name == null) {
            throw new NullPointerException("name");
        }

        getPropertyStorage().setValue(NAME, name);
    }

    /**
     * @return {@link #TECHNOLOGIES}
     */
    public Set<Technology> getTechnologies()
    {
        return getPropertyStorage().getCollection(TECHNOLOGIES, Set.class);
    }

    /**
     * @param technologies sets the {@link #TECHNOLOGIES}
     */
    public void setTechnologies(Set<Technology> technologies)
    {
        getPropertyStorage().setCollection(TECHNOLOGIES, technologies);
    }

    /**
     * @param technology technology to be added to the {@link #TECHNOLOGIES}
     */
    public void addTechnology(Technology technology)
    {
        getPropertyStorage().addCollectionItem(TECHNOLOGIES, technology, Set.class);
    }

    /**
     * @param technology technology to be removed from the {@link #TECHNOLOGIES}
     */
    public void removeTechnology(Technology technology)
    {
        getPropertyStorage().removeCollectionItem(TECHNOLOGIES, technology);
    }

    /**
     * @return number of ports that multipoint server can utilize for this room
     */
    public int getLicenseCount()
    {
        return getPropertyStorage().getValueAsInt(LICENSE_COUNT);
    }

    /**
     * @param licenseCount number of license that multipoint server can utilize for this room
     */
    public void setLicenseCount(int licenseCount)
    {
        if (licenseCount < 0) {
            throw new IllegalArgumentException("License count must be non-negative");
        }

        getPropertyStorage().setValue(LICENSE_COUNT, licenseCount);
    }

    /**
     * @return list of aliases under which the room is accessible
     */
    public List<Alias> getAliases()
    {
        return getPropertyStorage().getCollection(ALIASES, List.class);
    }

    /**
     * Adds a new alias under which the room is accessible.
     *
     * @param alias alias to add for the room
     */
    public void addAlias(Alias alias)
    {
        getPropertyStorage().addCollectionItem(ALIASES, alias, List.class);
    }

    /**
     * @param aliases aliases under which the room is accessible
     */
    public void setAliases(List<Alias> aliases)
    {
        getPropertyStorage().setCollection(ALIASES, aliases);
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
        return getPropertyStorage().getMap(OPTIONS);
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
        getPropertyStorage().setMap(OPTIONS, options);
    }

    /**
     * Finds out whether a given option is set.
     *
     * @param option option to find
     * @return <code>true</code> if option with the given name is set, <code>false</code> if not
     */
    public boolean hasOption(Option option)
    {
        return getPropertyStorage().getMap(OPTIONS).containsKey(option);
    }

    /**
     * Returns the value of an option.
     *
     * @param option option to get value of
     * @return value of option, or <code>null</code> if the option is not set
     */
    public Object getOption(Option option)
    {
        return getPropertyStorage().getMap(OPTIONS).get(option);
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
        Object value = getOption(option);
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
            removeOption(option);
        }
        else {
            validateOption(option, value);
            getPropertyStorage().addMapItem(OPTIONS, option, value);
        }
    }

    /**
     * Unsets a single option.
     *
     * @param option option to unset
     */
    public void removeOption(Option option)
    {
        getPropertyStorage().removeMapItem(OPTIONS, option);
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
     * Fill {@link #OPTIONS} from given {@code roomSetting}
     * @param roomSetting
     */
    public void fillOptions(RoomSetting roomSetting)
    {
        // TODO: use RoomSetting in the Room instead of map of options
        if (roomSetting instanceof RoomSetting.H323) {
            RoomSetting.H323 roomSettingH323 = (RoomSetting.H323) roomSetting;
            if (roomSettingH323.getPin() != null) {
                setOption(Option.PIN, roomSettingH323.getPin());
            }
        }
        else if (roomSetting instanceof RoomSetting.AdobeConnect) {
            RoomSetting.AdobeConnect roomSettingAdobeConnect = (RoomSetting.AdobeConnect) roomSetting;
            setOption(Option.PARTICIPANTS, roomSettingAdobeConnect.getParticipants());
        }
    }

    /**
     * @return
     */
    public Alias getAliase(AliasType aliasType)
    {
        for (Alias alias : this.getAliases()) {
            if (alias.getType() == aliasType)
                return alias;
        }

        return null;
    }

    /**
     * Room options.
     */
    public static enum Option
    {
        /**
         * A string option - some description of the room.
         */
        DESCRIPTION(String.class),

        /**
         * A string option - the PIN that must be entered to get to the room.
         */
        PIN(String.class),

        /**
         * A boolean option whether to list the room in public lists. Defaults to false.
         */
        LISTED_PUBLICLY(Boolean.class),

        /**
         * A boolean option whether participants may contribute content. Defaults to true.
         */
        ALLOW_CONTENT(Boolean.class),

        /**
         * A boolean option whether guests should be allowed to join. Defaults to true.
         */
        ALLOW_GUESTS(Boolean.class),

        /**
         * A boolean option whether audio should be muted on join. Defaults to false.
         */
        JOIN_AUDIO_MUTED(Boolean.class),

        /**
         * A boolean option whether video should be muted on join. Defaults to false.
         */
        JOIN_VIDEO_MUTED(Boolean.class),

        /**
         * A boolean option whether to register the aliases with the gatekeeper. Defaults to false.
         */
        REGISTER_WITH_H323_GATEKEEPER(Boolean.class),

        /**
         * A boolean option whether to register the aliases with the SIP registrar. Defaults to false.
         */
        REGISTER_WITH_SIP_REGISTRAR(Boolean.class),

        /**
         * A boolean option whether the room should be locked when started. Defaults to false.
         */
        START_LOCKED(Boolean.class),

        /**
         * A boolean option whether the ConferenceMe should be enabled for the room. Defaults to false.
         */
        CONFERENCE_ME_ENABLED(Boolean.class),

        /**
         * List of EPPNs of allowed participants for the room.
         */
        PARTICIPANTS(List.class);

        private Class valueClass;

        Option(Class valueClass)
        {
            this.valueClass = valueClass;
        }

        public Class getValueClass()
        {
            return valueClass;
        }
    }
}
