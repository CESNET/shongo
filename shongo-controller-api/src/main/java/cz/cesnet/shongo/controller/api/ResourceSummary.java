package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.api.IdentifiedComplexType;

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
    private String technologies;

    /**
     * Parent resource shongo-id.
     */
    private String parentResourceId;

    /**
     * Order in which the resource should be tried to be allocated ({@code null} means the last).
     */
    private Integer allocationOrder;

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
    public String getTechnologies()
    {
        return technologies;
    }

    /**
     * @param technologies sets the {@link #technologies}
     */
    public void setTechnologies(String technologies)
    {
        this.technologies = technologies;
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
     * @return {@link #allocationOrder}
     */
    public Integer getAllocationOrder()
    {
        return allocationOrder;
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
    private static final String ALLOCATION_ORDER = "allocationOrder";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(USER_ID, userId);
        dataMap.set(NAME, name);
        dataMap.set(ALLOCATION_ORDER, allocationOrder);
        dataMap.set(TECHNOLOGIES, technologies);
        dataMap.set(PARENT_RESOURCE_ID, parentResourceId);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        userId = dataMap.getString(USER_ID);
        name = dataMap.getString(NAME);
        allocationOrder = dataMap.getInteger(ALLOCATION_ORDER);
        technologies = dataMap.getString(TECHNOLOGIES);
        parentResourceId = dataMap.getString(PARENT_RESOURCE_ID);
    }
}
