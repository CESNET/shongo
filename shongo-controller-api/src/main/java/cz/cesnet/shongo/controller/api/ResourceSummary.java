package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.api.IdentifiedComplexType;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents an entity that can be scheduled.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ResourceSummary extends IdentifiedComplexType
{
    /**
     * User-id of the owner user.
     */
    private String userId;

    /**
     * Name of the resource.
     */
    private String name;

    /**
     * Technologies of the resource.
     */
    private Set<Technology> technologies = new HashSet<Technology>();

    /**
     * Parent resource shongo-id.
     */
    private String parentResourceId;

    /**
     * Specifies whether resource can be scheduled by a scheduler.
     */
    private Boolean allocatable;

    /**
     * Order in which the resource should be tried to be allocated ({@code null} means the last).
     */
    private Integer allocationOrder;

    /**
     * Description of the resource.
     */
    private String description;

    /**
     * @return {@link #userId}
     */
    public String getUserId()
    {
        return userId;
    }

    /**
     * @param userId sets the {@link #userId}
     */
    public void setUserId(String userId)
    {
        this.userId = userId;
    }

    /**
     * @return {@link #name}
     */
    public String getName()
    {
        return name;
    }

    /**
     * @param name sets the {@link #name}
     */
    public void setName(String name)
    {
        this.name = name;
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
     * @param technology to be added to the {@link #technologies}
     */
    public void addTechnology(Technology technology)
    {
        this.technologies.add(technology);
    }

    /**
     * @return {@link #parentResourceId}
     */
    public String getParentResourceId()
    {
        return parentResourceId;
    }

    /**
     * @param parentResourceId sets the {@link #parentResourceId}
     */
    public void setParentResourceId(String parentResourceId)
    {
        this.parentResourceId = parentResourceId;
    }

    /**
     * @return {@link #allocatable}
     */
    public Boolean getAllocatable()
    {
        return (allocatable != null ? allocatable : Boolean.FALSE);
    }

    /**
     * @param allocatable sets the {@link #allocatable}
     */
    public void setAllocatable(Boolean allocatable)
    {
        this.allocatable = allocatable;
    }

    /**
     * @return {@link #allocationOrder}
     */
    public Integer getAllocationOrder()
    {
        return allocationOrder;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @param allocationOrder sets the {@link #allocationOrder}
     */
    public void setAllocationOrder(Integer allocationOrder)
    {
        this.allocationOrder = allocationOrder;
    }

    private static final String USER_ID = "userId";
    private static final String NAME = "name";
    private static final String TECHNOLOGIES = "technologies";
    private static final String PARENT_RESOURCE_ID = "parentResourceId";
    private static final String ALLOCATABLE = "allocatable";
    private static final String ALLOCATION_ORDER = "allocationOrder";
    private static final String DESCRIPTION = "description";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(USER_ID, userId);
        dataMap.set(NAME, name);
        dataMap.set(ALLOCATABLE, allocatable);
        dataMap.set(ALLOCATION_ORDER, allocationOrder);
        dataMap.set(TECHNOLOGIES, technologies);
        dataMap.set(PARENT_RESOURCE_ID, parentResourceId);
        dataMap.set(DESCRIPTION,description);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        userId = dataMap.getString(USER_ID);
        name = dataMap.getString(NAME);
        allocatable = dataMap.getBool(ALLOCATABLE);
        allocationOrder = dataMap.getInteger(ALLOCATION_ORDER);
        technologies = dataMap.getSet(TECHNOLOGIES, Technology.class);
        parentResourceId = dataMap.getString(PARENT_RESOURCE_ID);
        description = dataMap.getString(DESCRIPTION);
    }
}
