package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.AbstractComplexType;
import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.controller.ObjectPermission;
import cz.cesnet.shongo.controller.SystemPermission;

import java.util.Set;

/**
 * Represents a set of system permissions
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class SystemPermissionSet extends AbstractComplexType
{
    private Set<SystemPermission> systemPermissions;

    public SystemPermissionSet()
    {
    }

    public SystemPermissionSet(Set<SystemPermission> systemPermissions)
    {
        this.systemPermissions = systemPermissions;
    }

    public Set<SystemPermission> getSystemPermissions()
    {
        return systemPermissions;
    }

    public void setSystemPermissions(Set<SystemPermission> systemPermissions)
    {
        this.systemPermissions = systemPermissions;
    }

    private static final String SYSTEM_PERMISSIONS = "systemPermissions";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(SYSTEM_PERMISSIONS, systemPermissions);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        systemPermissions = dataMap.getSet(SYSTEM_PERMISSIONS, SystemPermission.class);
    }
}
