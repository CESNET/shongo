package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.*;
import org.joda.time.Interval;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents an abstract class for a room in a device.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class AbstractRoomExecutable extends Executable
{
    /**
     * Specifies the original slot which was requested by {@link RoomAvailability}.
     * Unlike the {@link #slot}, the {@link #originalSlot} does not reflect the
     * {@link RoomAvailability#slotMinutesBefore} and {@link RoomAvailability#slotMinutesAfter}.
     * The {@link #originalSlot} must be contained in the {@link #slot}.
     */
    private Interval originalSlot;

    /**
     * Set of technologies which the room supports.
     */
    private Set<Technology> technologies = new HashSet<Technology>();

    /**
     * License count.
     */
    private int licenseCount;

    /**
     * Description of the room.
     */
    private String description;

    /**
     * List of assigned {@link cz.cesnet.shongo.api.Alias}es to the {@link EndpointExecutable}.
     */
    private List<Alias> aliases = new ArrayList<Alias>();

    /**
     * List of {@link cz.cesnet.shongo.api.RoomSetting}s for the {@link cz.cesnet.shongo.controller.api.AbstractRoomExecutable}.
     */
    private List<RoomSetting> roomSettings = new ArrayList<RoomSetting>();

    /**
     * @see RoomExecutableParticipantConfiguration
     */
    private RoomExecutableParticipantConfiguration participantConfiguration;

    /**
     * Specifies whether this room have recording service.
     */
    private boolean hasRecordingService = false;

    /**
     * Specifies whether this room have recordings.
     */
    private boolean hasRecordings = false;

    /**
     * @return {@link #originalSlot}
     */
    public Interval getOriginalSlot()
    {
        return originalSlot;
    }

    /**
     * @param originalSlot sets the {@link #originalSlot}
     */
    public void setOriginalSlot(Interval originalSlot)
    {
        this.originalSlot = originalSlot;
    }

    /**
     * @return {@link #technologies}
     */
    public Set<Technology> getTechnologies()
    {
        return technologies;
    }

    /**
     * @param technologies sets the {@link #technologies}
     */
    public void setTechnologies(Set<Technology> technologies)
    {
        this.technologies = technologies;
    }

    /**
     * Clear {@link #technologies}.
     */
    public void clearTechnologies()
    {
        technologies.clear();
    }

    /**
     * @param technology technology to be added to the set of technologies that the device support.
     */
    public void addTechnology(Technology technology)
    {
        technologies.add(technology);
    }

    /**
     * @return {@link #licenseCount}
     */
    public int getLicenseCount()
    {
        return licenseCount;
    }

    /**
     * @param licenseCount sets the {@link #licenseCount}
     */
    public void setLicenseCount(int licenseCount)
    {
        this.licenseCount = licenseCount;
    }

    /**
     * @return {@link #description}
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * @param description sets the {@link #description}
     */
    public void setDescription(String description)
    {
        this.description = description;
    }

    /**
     * @return {@link #aliases}
     */
    public List<Alias> getAliases()
    {
        return aliases;
    }

    /**
     * @param aliasType
     * @return first {@link cz.cesnet.shongo.api.Alias} of given {@code aliasType}
     */
    public Alias getAliasByType(AliasType aliasType)
    {
        for (Alias alias : aliases) {
            if (alias.getType().equals(aliasType)) {
                return alias;
            }
        }
        return null;
    }

    /**
     * @param aliases sets the {@link #aliases}
     */
    public void setAliases(List<Alias> aliases)
    {
        this.aliases = aliases;
    }

    /**
     * Clear {@link #aliases}.
     */
    public void clearAliases()
    {
        aliases.clear();
    }

    /**
     * @param alias to be added to the {@link #aliases}
     */
    public void addAlias(Alias alias)
    {
        aliases.add(alias);
    }

    /**
     * @return {@link #roomSettings}
     */
    public List<RoomSetting> getRoomSettings()
    {
        return roomSettings;
    }

    /**
     * @param roomSettings sets the {@link #roomSettings}
     */
    public void setRoomSettings(List<RoomSetting> roomSettings)
    {
        this.roomSettings = roomSettings;
    }

    /**
     * @param roomSettingType
     * @return {@link RoomSetting} of given {@code roomSettingType} or null if doesn't exist
     */
    public <T extends RoomSetting> T getRoomSetting(Class<T> roomSettingType)
    {
        for (RoomSetting roomSetting : getRoomSettings()) {
            if (roomSettingType.isInstance(roomSetting)) {
                return roomSettingType.cast(roomSetting);
            }
        }
        return null;
    }

    /**
     * @param roomSetting to be added to the {@link #roomSettings}
     */
    public void addRoomSetting(RoomSetting roomSetting)
    {
        RoomSetting existingRoomSetting = getRoomSetting(roomSetting.getClass());
        if (existingRoomSetting != null) {
            existingRoomSetting.merge(roomSetting);
        }
        else {
            roomSettings.add(roomSetting);
        }
    }

    /**
     * @return {@link #participantConfiguration}
     */
    public RoomExecutableParticipantConfiguration getParticipantConfiguration()
    {
        return participantConfiguration;
    }

    /**
     * @param participantConfiguration sets the {@link #participantConfiguration}
     */
    public void setParticipantConfiguration(RoomExecutableParticipantConfiguration participantConfiguration)
    {
        this.participantConfiguration = participantConfiguration;
    }

    /**
     * @return {@link #hasRecordingService}
     */
    public boolean hasRecordingService()
    {
        return hasRecordingService;
    }

    /**
     * @param hasRecordingService sets the {@link #hasRecordingService}
     */
    public void setHasRecordingService(boolean hasRecordingService)
    {
        this.hasRecordingService = hasRecordingService;
    }

    /**
     * @return {@link #hasRecordings}
     */
    public boolean hasRecordings()
    {
        return hasRecordings;
    }

    /**
     * @param hashRecordings sets the {@link #hasRecordings}
     */
    public void setHasRecordings(boolean hashRecordings)
    {
        this.hasRecordings = hashRecordings;
    }

    private static final String ORIGINAL_SLOT = "originalSlot";
    private static final String TECHNOLOGIES = "technologies";
    private static final String LICENSE_COUNT = "licenseCount";
    private static final String DESCRIPTION = "description";
    private static final String ALIASES = "aliases";
    private static final String ROOM_SETTINGS = "roomSettings";
    private static final String PARTICIPANT_CONFIGURATION = "participantConfiguration";
    private static final String HAS_RECORDING_SERVICE = "hasRecordingService";
    private static final String HAS_RECORDINGS = "hasRecordings";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(ORIGINAL_SLOT, originalSlot);
        dataMap.set(TECHNOLOGIES, technologies);
        dataMap.set(LICENSE_COUNT, licenseCount);
        dataMap.set(DESCRIPTION, description);
        dataMap.set(ALIASES, aliases);
        dataMap.set(ROOM_SETTINGS, roomSettings);
        dataMap.set(PARTICIPANT_CONFIGURATION, participantConfiguration);
        dataMap.set(HAS_RECORDING_SERVICE, hasRecordingService);
        dataMap.set(HAS_RECORDINGS, hasRecordings);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        originalSlot = dataMap.getInterval(ORIGINAL_SLOT);
        technologies = dataMap.getSet(TECHNOLOGIES, Technology.class);
        licenseCount = dataMap.getInt(LICENSE_COUNT);
        description = dataMap.getString(DESCRIPTION);
        aliases = dataMap.getList(ALIASES, Alias.class);
        roomSettings = dataMap.getList(ROOM_SETTINGS, RoomSetting.class);
        participantConfiguration = dataMap.getComplexType(
                PARTICIPANT_CONFIGURATION, RoomExecutableParticipantConfiguration.class);
        hasRecordingService = dataMap.getBool(HAS_RECORDING_SERVICE);
        hasRecordings = dataMap.getBool(HAS_RECORDINGS);
    }

    public String getPin()
    {
        String pin = null;
        for (RoomSetting setting : roomSettings) {
            if (setting instanceof H323RoomSetting
                    && (technologies.contains(Technology.H323) || technologies.contains(Technology.SIP))) {
                H323RoomSetting h323RoomSetting = (H323RoomSetting) setting;
                if (h323RoomSetting.getPin() != null) {
                    if (pin != null) {
                        throw new RuntimeException("Multiple PIN specified.");
                    }
                    pin = h323RoomSetting.getPin();
                }
            }
            else if (setting instanceof AdobeConnectRoomSetting && technologies.contains(Technology.ADOBE_CONNECT)) {
                AdobeConnectRoomSetting adobeConnectRoomSetting = (AdobeConnectRoomSetting) setting;
                if (adobeConnectRoomSetting.getPin() != null) {
                    if (pin != null) {
                        throw new RuntimeException("Multiple PIN specified.");
                    }
                    pin = adobeConnectRoomSetting.getPin();
                }
            } else if (setting instanceof PexipRoomSetting
                    && (technologies.contains(Technology.H323))) {
                PexipRoomSetting pexipRoomSetting = (PexipRoomSetting) setting;
                if (pexipRoomSetting.getGuestPin() != null) {
                    if (pin != null) {
                        throw new RuntimeException("Multiple PIN specified.");
                    }
                    pin = pexipRoomSetting.getGuestPin();
                }
            }
        }
        return pin;
    }

    public String getAdminPin()
    {
        String pin = null;
        for (RoomSetting setting : roomSettings) {
            if (setting instanceof PexipRoomSetting
                    && (technologies.contains(Technology.H323))) {
                PexipRoomSetting pexipRoomSetting = (PexipRoomSetting) setting;
                if (pexipRoomSetting.getHostPin() != null) {
                    pin = pexipRoomSetting.getHostPin();
                }
            }
        }
        return pin;
    }

    public String getGuestPin()
    {
        String pin = null;
        for (RoomSetting setting : roomSettings) {
            if (setting instanceof PexipRoomSetting
                    && (technologies.contains(Technology.H323))) {
                PexipRoomSetting pexipRoomSetting = (PexipRoomSetting) setting;
                if (pexipRoomSetting.getGuestPin() != null) {
                    pin = pexipRoomSetting.getGuestPin();
                }
            }
        }
        return pin;
    }

    public Boolean getAllowGuests()
    {
        Boolean allowGuests = null;
        for (RoomSetting setting : roomSettings) {
            if (setting instanceof PexipRoomSetting
                    && (technologies.contains(Technology.H323))) {
                PexipRoomSetting pexipRoomSetting = (PexipRoomSetting) setting;
                if (pexipRoomSetting.getAllowGuests() != null) {
                    allowGuests = pexipRoomSetting.getAllowGuests();
                }
            }
        }
        return allowGuests;
    }
}
