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
public abstract class StandaloneRoomSpecification extends AbstractRoomSpecification
{
    /**
     * Preferred {@link Resource} shongo-id with {@link RoomProviderCapability}.
     */
    private String resourceId;

    /**
     * Set of technologies which the virtual rooms must support.
     */
    private Set<Technology> technologies = new HashSet<Technology>();

    /**
     * {@link cz.cesnet.shongo.controller.api.AliasSpecification}s for the virtual room.
     */
    private List<AliasSpecification> aliasSpecifications = new LinkedList<AliasSpecification>();

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
     * @return {@link #aliasSpecifications}
     */
    public List<AliasSpecification> getAliasSpecifications()
    {
        return aliasSpecifications;
    }

    /**
     * @param aliasType
     * @return {@link cz.cesnet.shongo.controller.api.AliasSpecification} which specifies given {@code aliasType}
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
    public void setAliasSpecifications(List<AliasSpecification> aliasSpecifications)
    {
        this.aliasSpecifications = aliasSpecifications;
    }

    /**
     * @param aliasSpecification to be added to the {@link #aliasSpecifications}
     */
    public void addAliasSpecification(AliasSpecification aliasSpecification)
    {
        aliasSpecifications.add(aliasSpecification);
    }

    /**
     * @param aliasSpecification to be removed from the {@link #aliasSpecifications}
     */
    public void removeAliasSpecification(AliasSpecification aliasSpecification)
    {
        aliasSpecifications.remove(aliasSpecification);
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
        dataMap.set(ALIAS_SPECIFICATIONS, aliasSpecifications);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        resourceId = dataMap.getString(RESOURCE_ID);
        technologies = dataMap.getSetRequired(TECHNOLOGIES, Technology.class);
        aliasSpecifications = dataMap.getList(ALIAS_SPECIFICATIONS, AliasSpecification.class);
    }
}
