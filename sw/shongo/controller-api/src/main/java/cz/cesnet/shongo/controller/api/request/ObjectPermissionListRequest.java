package cz.cesnet.shongo.controller.api.request;

import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.controller.ObjectPermission;
import cz.cesnet.shongo.controller.api.SecurityToken;

import java.util.HashSet;
import java.util.Set;

/**
 * {@link ListRequest} for {@link ObjectPermission}s for objects.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ObjectPermissionListRequest extends AbstractRequest
{
    /**
     * Identifier of the Shongo public entity.
     */
    private Set<String> objectIds = new HashSet<String>();

    /**
     * Constructor.
     */
    public ObjectPermissionListRequest()
    {
    }

    /**
     * Constructor.
     *
     * @param securityToken sets the {@link #securityToken}
     */
    public ObjectPermissionListRequest(SecurityToken securityToken)
    {
        super(securityToken);
    }

    /**
     * Constructor.
     *
     * @param securityToken sets the {@link #securityToken}
     * @param objectId to be added to the {@link #objectIds}
     */
    public ObjectPermissionListRequest(SecurityToken securityToken, String objectId)
    {
        super(securityToken);
        this.objectIds.add(objectId);
    }

    /**
     * Constructor.
     *
     * @param securityToken sets the {@link #securityToken}
     * @param objectIds to be added to the {@link #objectIds}
     */
    public ObjectPermissionListRequest(SecurityToken securityToken, Set<String> objectIds)
    {
        super(securityToken);
        this.objectIds.addAll(objectIds);
    }

    public Set<String> getObjectIds()
    {
        return objectIds;
    }

    public void setObjectIds(Set<String> objectIds)
    {
        this.objectIds = objectIds;
    }

    public void addObjectId(String objectId)
    {
        objectIds.add(objectId);
    }

    private static final String OBJECT_IDS = "objectIds";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(OBJECT_IDS, objectIds);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        objectIds = dataMap.getSet(OBJECT_IDS, String.class);
    }
}
