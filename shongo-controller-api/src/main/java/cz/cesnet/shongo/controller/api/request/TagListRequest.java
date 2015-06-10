package cz.cesnet.shongo.controller.api.request;

import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.controller.api.Domain;
import cz.cesnet.shongo.controller.api.SecurityToken;

/**
 *
 * @author Ondřej Pavelka <pavelka@cesnet.cz>
*/
public class TagListRequest extends AbstractRequest
{
    String resourceId;

    String domainId;

    String foreignResourceType;

    public TagListRequest() {
    }

    public TagListRequest(SecurityToken securityToken)
    {
        super(securityToken);
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getForeignResourceType()
    {
        return foreignResourceType;
    }

    public void setForeignResourceType(String foreignResourceType)
    {
        this.foreignResourceType = foreignResourceType;
    }

    public String getDomainId()
    {
        return domainId;
    }

    public void setDomainId(String domainId)
    {
        this.domainId = domainId;
    }

    private static final String RESOURCE_ID = "resourceId";
    private static final String FOREIGN_RESOURCE_TYPE = "foreignResourceType";
    private static final String DOMAIN_ID = "domainId";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(RESOURCE_ID, resourceId);
        dataMap.set(FOREIGN_RESOURCE_TYPE, foreignResourceType);
        dataMap.set(DOMAIN_ID, domainId);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        resourceId = dataMap.getString(RESOURCE_ID);
        foreignResourceType = dataMap.getString(FOREIGN_RESOURCE_TYPE);
        domainId = dataMap.getString(DOMAIN_ID);
    }
}
