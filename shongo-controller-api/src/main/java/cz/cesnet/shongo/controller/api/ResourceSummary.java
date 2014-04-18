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

    private static final String USER_ID = "userId";
    private static final String NAME = "name";
    private static final String TECHNOLOGIES = "technologies";
    private static final String PARENT_RESOURCE_ID = "parentResourceId";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(USER_ID, userId);
        dataMap.set(NAME, name);
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
        technologies = dataMap.getString(TECHNOLOGIES);
        parentResourceId = dataMap.getString(PARENT_RESOURCE_ID);
    }
}
