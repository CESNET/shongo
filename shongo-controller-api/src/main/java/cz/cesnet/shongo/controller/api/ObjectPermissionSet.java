package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.AbstractComplexType;
import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.controller.ObjectPermission;

import java.util.Set;

/**
 * Represents a set of entity permissions
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ObjectPermissionSet extends AbstractComplexType
{
    private Set<ObjectPermission> objectPermissions;

    public ObjectPermissionSet()
    {
    }

    public ObjectPermissionSet(Set<ObjectPermission> objectPermissions)
    {
        this.objectPermissions = objectPermissions;
    }

    public Set<ObjectPermission> getObjectPermissions()
    {
        return objectPermissions;
    }

    public void setObjectPermissions(Set<ObjectPermission> objectPermissions)
    {
        this.objectPermissions = objectPermissions;
    }

    public boolean contains(ObjectPermission objectPermission)
    {
        if (this.objectPermissions == null) {
            return false;
        }
        return this.objectPermissions.contains(objectPermission);
    }

    private static final String OBJECT_PERMISSIONS = "objectPermissions";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(OBJECT_PERMISSIONS, objectPermissions);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        objectPermissions = dataMap.getSet(OBJECT_PERMISSIONS, ObjectPermission.class);
    }
}
