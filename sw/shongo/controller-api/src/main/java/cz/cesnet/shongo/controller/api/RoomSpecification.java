package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.DataMap;

import java.util.LinkedList;
import java.util.List;

/**
 * {@link Specification} for a room.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class RoomSpecification extends StandaloneRoomSpecification
{
    /**
     * Number of ports which must be allocated for the virtual room.
     */
    private Integer participantCount;

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
     * @param resourceId sets the {@link #resourceId}
     * @return this {@link RoomSpecification} with {@link #resourceId} set to {@code resourceId}
     */
    public RoomSpecification withResourceId(String resourceId)
    {
        setResourceId(resourceId);
        return this;
    }

    /**
     * @param aliasType for the new {@link AliasSpecification}
     * @param value     for the new {@link AliasSpecification}
     * @return this {@link RoomSpecification}
     */
    public RoomSpecification withAlias(AliasType aliasType, String value)
    {
        addAliasSpecification(new AliasSpecification(aliasType).withValue(value));
        return this;
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

    public static final String PARTICIPANT_COUNT = "participantCount";
    public static final String SERVICE_SPECIFICATIONS = "serviceSpecifications";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(PARTICIPANT_COUNT, participantCount);
        dataMap.set(SERVICE_SPECIFICATIONS, serviceSpecifications);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        participantCount = dataMap.getIntegerRequired(PARTICIPANT_COUNT);
        technologies = dataMap.getSetRequired(TECHNOLOGIES, Technology.class);
        serviceSpecifications = dataMap.getList(SERVICE_SPECIFICATIONS, ExecutableServiceSpecification.class);
    }
}
