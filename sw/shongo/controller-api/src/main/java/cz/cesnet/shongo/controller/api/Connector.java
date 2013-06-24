package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.AbstractComplexType;
import cz.cesnet.shongo.api.DataMap;

/**
 * Represents an information about known connector in a controlled domain.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Connector extends AbstractComplexType
{
    /**
     * A unique connector name within the domain (Jade agent name).
     */
    private String name;

    /**
     * Id of a resource which is managed by the connector.
     */
    private String resourceId;

    /**
     * Status of the connector.
     */
    private Status status;

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
     * @return {@link #status}
     */
    public Status getStatus()
    {
        return status;
    }

    /**
     * @param status sets the {@link #status}
     */
    public void setStatus(Status status)
    {
        this.status = status;
    }

    private static final String NAME = "name";
    private static final String RESOURCE_ID = "resourceId";
    private static final String STATUS = "status";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(NAME, name);
        dataMap.set(RESOURCE_ID, resourceId);
        dataMap.set(STATUS, status);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        name = dataMap.getString(NAME);
        resourceId = dataMap.getString(RESOURCE_ID);
        status = dataMap.getEnum(STATUS, Status.class);
    }
}
