package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.api.RoomSetting;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * {@link Specification} for a meeting room.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class RoomSpecification extends Specification
{
    /**
     * Preferred {@link Resource} shongo-id with {@link AliasProviderCapability}.
     */
    private String resourceId;

    /**
     * Set of technologies which the virtual rooms must support.
     */
    private Set<Technology> technologies = new HashSet<Technology>();

    /**
     * Number of ports which must be allocated for the virtual room.
     */
    private Integer participantCount;

    /**
     * {@link cz.cesnet.shongo.api.RoomSetting}s for the virtual room.
     */
    private List<RoomSetting> roomSettings = new LinkedList<RoomSetting>();

    /**
     * {@link AliasSpecification}s for the virtual room.
     */
    private List<AliasSpecification> aliasSpecifications = new LinkedList<AliasSpecification>();

    /**
     * Collection of {@link AbstractParticipant}s for the meeting room.
     */
    private List<AbstractParticipant> participants = new LinkedList<AbstractParticipant>();

    /**
     * {@link ExecutableServiceSpecification}s for the virtual room.
     */
    private List<ExecutableServiceSpecification> serviceSpecifications = new LinkedList<ExecutableServiceSpecification>();

    /**
     * Constructor.
     */
    public RoomSpecification()
    {
    }

    /**
     * Constructor.
     *
     * @param participantCount sets the {@link #participantCount}
     * @param technology       to be added to the {@link #technologies}
     */
    public RoomSpecification(int participantCount, Technology technology)
    {
        setParticipantCount(participantCount);
        addTechnology(technology);
    }

    /**
     * Constructor.
     *
     * @param participantCount sets the {@link #participantCount}
     * @param technology       to be added to the {@link #technologies}
     * @param resourceId       sets the {@link #resourceId}
     */
    public RoomSpecification(int participantCount, Technology technology, String resourceId)
    {
        setParticipantCount(participantCount);
        addTechnology(technology);
        setResourceId(resourceId);
    }

    /**
     * Constructor.
     *
     * @param participantCount sets the {@link #participantCount}
     * @param technologies     to be added to the {@link #technologies}
     */
    public RoomSpecification(int participantCount, Technology[] technologies)
    {
        setParticipantCount(participantCount);
        for (Technology technology : technologies) {
            addTechnology(technology);
        }
    }

    /**
     * @param aliasType for the new {@link AliasSpecification}
     * @param value     for the new {@link AliasSpecification}
     * @return this {@link RoomSpecification}
     */
    public RoomSpecification withAlias(AliasType aliasType, String value)
    {
        addAlias(new AliasSpecification(aliasType).withValue(value));
        return this;
    }

    /**
     * @return {@link #resourceId}
     */
    public String getResourceId()
    {
        return resourceId;
    }

    /**
     * @param resourceId sets the {@link #resourceId}
     */
    public void setResourceId(String resourceId)
    {
        this.resourceId = resourceId;
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
     * @param technology technology to be added to the {@link #technologies}
     */
    public void addTechnology(Technology technology)
    {
        technologies.add(technology);
    }

    /**
     * @param technology technology to be removed from the {@link #technologies}
     */
    public void removeTechnology(Technology technology)
    {
        technologies.remove(technology);
    }

    /**
     * @return {@link #participantCount}
     */
    public Integer getParticipantCount()
    {
        return participantCount;
    }

    /**
     * @param participantCount sets the {@link #participantCount}
     */
    public void setParticipantCount(Integer participantCount)
    {
        this.participantCount = participantCount;
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
     * @param roomSetting to be added to the {@link #roomSettings}
     */
    public void addRoomSetting(RoomSetting roomSetting)
    {
        roomSettings.add(roomSetting);
    }

    /**
     * @param roomSetting to be removed from the {@link #roomSettings}
     */
    public void removeRoomSetting(RoomSetting roomSetting)
    {
        roomSettings.remove(roomSetting);
    }

    /**
     * @return {@link #aliasSpecifications}
     */
    public List<AliasSpecification> getAliasSpecifications()
    {
        return aliasSpecifications;
    }

    /**
     * @param aliasType
     * @return {@link AliasSpecification} which specifies given {@code aliasType}
     */
    public AliasSpecification getAliasSpecificationByType(AliasType aliasType)
    {
        for (AliasSpecification aliasSpecification : aliasSpecifications) {
            if (aliasSpecification.getAliasTypes().contains(aliasType)) {
                return aliasSpecification;
            }
        }
        return null;
    }

    /**
     * @param aliasSpecifications sets the {@link #aliasSpecifications}
     */
    public void setAliases(List<AliasSpecification> aliasSpecifications)
    {
        this.aliasSpecifications = aliasSpecifications;
    }

    /**
     * @param aliasSpecification to be added to the {@link #aliasSpecifications}
     */
    public void addAlias(AliasSpecification aliasSpecification)
    {
        aliasSpecifications.add(aliasSpecification);
    }

    /**
     * @param aliasSpecification to be removed from the {@link #aliasSpecifications}
     */
    public void removeAlias(AliasSpecification aliasSpecification)
    {
        aliasSpecifications.remove(aliasSpecification);
    }

    /**
     * @return {@link #participants}
     */
    public List<AbstractParticipant> getParticipants()
    {
        return participants;
    }

    /**
     * @param participant to be added to the {@link #participants}
     */
    public void addParticipant(AbstractParticipant participant)
    {
        participants.add(participant);
    }

    /**
     * @return {@link #serviceSpecifications}
     */
    public List<ExecutableServiceSpecification> getServiceSpecifications()
    {
        return serviceSpecifications;
    }

    /**
     * @param serviceSpecification to be added to the {@link #serviceSpecifications}
     */
    public void addServiceSpecification(ExecutableServiceSpecification serviceSpecification)
    {
        serviceSpecifications.add(serviceSpecification);
    }

    public static final String RESOURCE_ID = "resourceId";
    public static final String TECHNOLOGIES = "technologies";
    public static final String PARTICIPANT_COUNT = "participantCount";
    public static final String ROOM_SETTINGS = "roomSettings";
    public static final String ALIAS_SPECIFICATIONS = "aliasSpecifications";
    public static final String PARTICIPANTS = "participants";
    public static final String SERVICE_SPECIFICATIONS = "serviceSpecifications";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(RESOURCE_ID, resourceId);
        dataMap.set(TECHNOLOGIES, technologies);
        dataMap.set(PARTICIPANT_COUNT, participantCount);
        dataMap.set(ROOM_SETTINGS, roomSettings);
        dataMap.set(ALIAS_SPECIFICATIONS, aliasSpecifications);
        dataMap.set(PARTICIPANTS, participants);
        dataMap.set(SERVICE_SPECIFICATIONS, serviceSpecifications);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        resourceId = dataMap.getString(RESOURCE_ID);
        technologies = dataMap.getSetRequired(TECHNOLOGIES, Technology.class);
        participantCount = dataMap.getIntegerRequired(PARTICIPANT_COUNT);
        roomSettings = dataMap.getList(ROOM_SETTINGS, RoomSetting.class);
        aliasSpecifications = dataMap.getList(ALIAS_SPECIFICATIONS, AliasSpecification.class);
        serviceSpecifications = dataMap.getList(SERVICE_SPECIFICATIONS, ExecutableServiceSpecification.class);
    }
}
