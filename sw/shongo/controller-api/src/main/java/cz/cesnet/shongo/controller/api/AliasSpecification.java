package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.DataMap;

import java.util.*;

/**
 * Represents a {@link Specification} for an {@link Alias}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AliasSpecification extends Specification
{
    /**
     * Restricts {@link AliasType} for allocation of {@link Alias}.
     */
    private Set<AliasType> aliasTypes = new HashSet<AliasType>();

    /**
     * Restricts {@link Technology} for allocation of {@link Alias}.
     */
    private Set<Technology> technologies = new HashSet<Technology>();

    /**
     * Restricts {@link Alias#value}.
     */
    private String value;

    /**
     * {@link Resource} with {@link AliasProviderCapability} from which the {@link Alias} should be allocated.
     */
    private String resourceId;

    /**
     * Specifies whether the {@link Alias} should represent a permanent room (should get allocated {@link RoomExecutable}).
     */
    private boolean permanentRoom = false;

    /**
     * Collection of {@link AbstractParticipant}s for the permanent room.
     */
    private List<AbstractParticipant> permanentRoomParticipants = new LinkedList<AbstractParticipant>();

    /**
     * Constructor.
     */
    public AliasSpecification()
    {
    }

    /**
     * Constructor.
     *
     * @param technology to be added to the {@link #technologies}
     */
    public AliasSpecification(Technology technology)
    {
        addTechnology(technology);
    }

    /**
     * Constructor.
     *
     * @param aliasType to be added to the {@link #aliasTypes}
     */
    public AliasSpecification(AliasType aliasType)
    {
        addAliasType(aliasType);
    }

    /**
     * Constructor.
     *
     * @param aliasTypes sets the {@link #aliasTypes}
     */
    public AliasSpecification(AliasType[] aliasTypes)
    {
        for (AliasType aliasType : aliasTypes) {
            addAliasType(aliasType);
        }
    }

    /**
     * @param resourceId sets the {@link #resourceId}
     * @return this {@link AliasSpecification} with {@link #resourceId} set to {@code resourceId}
     */
    public AliasSpecification withResourceId(String resourceId)
    {
        setResourceId(resourceId);
        return this;
    }

    /**
     * @param value sets the {@link #value}
     * @return this {@link AliasSpecification} with {@link #value} set to {@code value}
     */
    public AliasSpecification withValue(String value)
    {
        setValue(value);
        return this;
    }

    /**
     * @return this {@link AliasSpecification} with {@link #permanentRoom} set to {@code true}
     */
    public AliasSpecification withPermanentRoom()
    {
        setPermanentRoom(true);
        return this;
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
     * @param technologies to be added to the {@link #technologies}
     */
    public void addTechnologies(Collection<Technology> technologies)
    {
        for (Technology technology : technologies) {
            addTechnology(technology);
        }
    }

    /**
     * @param technology technology to be removed from the {@link #technologies}
     */
    public void removeTechnology(Technology technology)
    {
        technologies.remove(technology);
    }

    /**
     * @return {@link #aliasTypes}
     */
    public Set<AliasType> getAliasTypes()
    {
        return aliasTypes;
    }

    /**
     * @param aliasTypes sets the {@link #aliasTypes}
     */
    public void setAliasTypes(Set<AliasType> aliasTypes)
    {
        this.aliasTypes = aliasTypes;
    }

    /**
     * @param aliasType to be added to the {@link #aliasTypes}
     */
    public void addAliasType(AliasType aliasType)
    {
        aliasTypes.add(aliasType);
    }

    /**
     * @param aliasType to be removed from the {@link #aliasTypes}
     */
    public void removeAliasType(AliasType aliasType)
    {
        aliasTypes.remove(aliasType);
    }

    /**
     * @return {@link #value}
     */
    public String getValue()
    {
        return  value;
    }

    /**
     * @param value sets the {@link #value}
     */
    public void setValue(String value)
    {
        this.value = value;
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
     * @return {@link #permanentRoom}
     */
    public boolean isPermanentRoom()
    {
        return permanentRoom;
    }

    /**
     * @param permanentRoom sets the {@link #permanentRoom}
     */
    public void setPermanentRoom(boolean permanentRoom)
    {
        this.permanentRoom = permanentRoom;
    }

    /**
     * @return {@link #permanentRoomParticipants}
     */
    public List<AbstractParticipant> getPermanentRoomParticipants()
    {
        return permanentRoomParticipants;
    }

    /**
     * @param permanentRoomParticipant to be added to the {@link #permanentRoomParticipants}
     */
    public void addPermanentRoomParticipant(AbstractParticipant permanentRoomParticipant)
    {
        permanentRoomParticipants.add(permanentRoomParticipant);
    }

    public static final String ALIAS_TYPES = "aliasTypes";
    public static final String TECHNOLOGIES = "technologies";
    public static final String VALUE = "value";
    public static final String RESOURCE_ID = "resourceId";
    public static final String PERMANENT_ROOM = "permanentRoom";
    public static final String PERMANENT_ROOM_PARTICIPANTS = "permanentRoomParticipants";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(ALIAS_TYPES, aliasTypes);
        dataMap.set(TECHNOLOGIES, technologies);
        dataMap.set(VALUE, value);
        dataMap.set(RESOURCE_ID, resourceId);
        dataMap.set(PERMANENT_ROOM, permanentRoom);
        dataMap.set(PERMANENT_ROOM_PARTICIPANTS, permanentRoomParticipants);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        aliasTypes = dataMap.getSet(ALIAS_TYPES, AliasType.class);
        technologies = dataMap.getSet(TECHNOLOGIES, Technology.class);
        value = dataMap.getString(VALUE);
        resourceId = dataMap.getString(RESOURCE_ID);
        permanentRoom = dataMap.getBool(PERMANENT_ROOM);
        permanentRoomParticipants = dataMap.getList(PERMANENT_ROOM_PARTICIPANTS, AbstractParticipant.class);
    }
}
