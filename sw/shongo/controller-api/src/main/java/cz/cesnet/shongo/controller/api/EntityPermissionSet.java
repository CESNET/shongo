package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.AbstractComplexType;
import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.controller.EntityPermission;

import java.util.Set;

/**
 * Represents a set of entity permissions
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class EntityPermissionSet extends AbstractComplexType
{
    private Set<EntityPermission> entityPermissions;

    public EntityPermissionSet()
    {
    }

    public EntityPermissionSet(Set<EntityPermission> entityPermissions)
    {
        this.entityPermissions = entityPermissions;
    }

    public Set<EntityPermission> getEntityPermissions()
    {
        return entityPermissions;
    }

    public void setEntityPermissions(Set<EntityPermission> entityPermissions)
    {
        this.entityPermissions = entityPermissions;
    }

    private static final String ENTITY_PERMISSIONS = "entityPermissions";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(ENTITY_PERMISSIONS, entityPermissions);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        entityPermissions = dataMap.getSet(ENTITY_PERMISSIONS, EntityPermission.class);
    }
}
