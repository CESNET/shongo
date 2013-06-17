package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.controller.Permission;

import java.util.Set;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class PermissionSet
{
    private Set<Permission> permissions;

    public PermissionSet()
    {
    }

    public PermissionSet(Set<Permission> permissions)
    {
        this.permissions = permissions;
    }

    public Set<Permission> getPermissions()
    {
        return permissions;
    }

    public void setPermissions(Set<Permission> permissions)
    {
        this.permissions = permissions;
    }
}
