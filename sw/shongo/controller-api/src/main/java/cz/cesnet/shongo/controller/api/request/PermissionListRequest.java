package cz.cesnet.shongo.controller.api.request;

import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.controller.api.SecurityToken;

import java.util.HashSet;
import java.util.Set;

/**
 * {@link ListRequest} for reservations.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class PermissionListRequest extends AbstractRequest
{
    /**
     * Identifier of the Shongo public entity.
     */
    private Set<String> entityIds = new HashSet<String>();

    /**
     * Constructor.
     */
    public PermissionListRequest()
    {
    }

    /**
     * Constructor.
     *
     * @param securityToken sets the {@link #securityToken}
     */
    public PermissionListRequest(SecurityToken securityToken)
    {
        super(securityToken);
    }

    /**
     * Constructor.
     *
     * @param securityToken sets the {@link #securityToken}
     * @param entityId to be added to the {@link #entityIds}
     */
    public PermissionListRequest(SecurityToken securityToken, String entityId)
    {
        super(securityToken);
        this.entityIds.add(entityId);
    }

    /**
     * Constructor.
     *
     * @param securityToken sets the {@link #securityToken}
     * @param entityIds to be added to the {@link #entityIds}
     */
    public PermissionListRequest(SecurityToken securityToken, Set<String> entityIds)
    {
        super(securityToken);
        this.entityIds.addAll(entityIds);
    }

    public Set<String> getEntityIds()
    {
        return entityIds;
    }

    public void setEntityIds(Set<String> entityIds)
    {
        this.entityIds = entityIds;
    }

    public void addEntityId(String entityId)
    {
        entityIds.add(entityId);
    }

    private static final String ENTITY_IDS = "entityIds";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(ENTITY_IDS, entityIds);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        entityIds = dataMap.getSet(ENTITY_IDS, String.class);
    }
}
