package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.AbstractComplexType;
import cz.cesnet.shongo.api.DataMap;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Represents that new room should be established.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class RoomEstablishment extends AbstractComplexType
{
    /**
     * Preferred {@link cz.cesnet.shongo.controller.api.Resource} shongo-id with {@link cz.cesnet.shongo.controller.api.RoomProviderCapability}.
     */
    private String resourceId;

    /**
     * Set of technologies which the virtual rooms must support.
     */
    protected Set<Technology> technologies = new HashSet<Technology>();

    /**
     * {@link AliasSpecification}s for the virtual room.
     */
    private List<AliasSpecification> aliasSpecifications = new LinkedList<AliasSpecification>();

    /**
     * Constructor.
     */
    public RoomEstablishment()
    {
    }

    /**
     * Constructor.
     *
     * @param technology to be added to the {@link #technologies}
     */
    public RoomEstablishment(Technology technology)
    {
        addTechnology(technology);
    }

    /**
     * Constructor.
     *
     * @param technology to be added to the {@link #technologies}
     */
    public RoomEstablishment(Technology technology, String resourceId)
    {
        setResourceId(resourceId);
        addTechnology(technology);
    }

    /**
     * Constructor.
     *
     * @param technologies sets the {@link #technologies}
     */
    public RoomEstablishment(Technology[] technologies)
    {
        for (Technology technology : technologies) {
            addTechnology(technology);
        }
    }

    /**
     * Constructor.
     *
     * @param aliasType to be added to the {@link #aliasSpecifications}
     */
    public RoomEstablishment(AliasType aliasType)
    {
        addAliasSpecification(new AliasSpecification(aliasType));
    }

    /**
     * Constructor.
     *
     * @param aliasTypes sets the {@link #aliasSpecifications}
     */
    public RoomEstablishment(AliasType[] aliasTypes)
    {
        for (AliasType aliasType : aliasTypes) {
            addAliasSpecification(new AliasSpecification(aliasType));
        }
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
        this.technologies.clear();
        this.technologies.addAll(technologies);
    }

    /**
     * @param technology sets the {@link #technologies}
     */
    public void setTechnology(Technology technology)
    {
        this.technologies.clear();
        this.technologies.add(technology);
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
    public static final String ALIAS_SPECIFICATIONS = "aliasSpecifications";

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
        technologies = dataMap.getSet(TECHNOLOGIES, Technology.class);
        aliasSpecifications = dataMap.getList(ALIAS_SPECIFICATIONS, AliasSpecification.class);
    }
}
