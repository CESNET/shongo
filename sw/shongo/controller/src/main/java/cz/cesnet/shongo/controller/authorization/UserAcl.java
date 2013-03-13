package cz.cesnet.shongo.controller.authorization;

import cz.cesnet.shongo.controller.Role;
import cz.cesnet.shongo.controller.common.EntityIdentifier;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class UserAcl
{

    private Map<EntityIdentifier, Set<Role>> rolesByEntity = new HashMap<EntityIdentifier, Set<Role>>();

    public void addUserRoleForEntity(EntityIdentifier entityId, Role role)
    {
        Set<Role> roles = rolesByEntity.get(entityId);
        if (roles == null) {
            roles = new HashSet<Role>();
            rolesByEntity.put(entityId, roles);
        }
        roles.add(role);
    }

    public void removeUserRoleForEntity(EntityIdentifier entityId, Role role)
    {
        Set<Role> roles = rolesByEntity.get(entityId);
        if (roles == null) {
            return;
        }
        roles.remove(role);
        if (roles.size() == 0) {
            rolesByEntity.remove(entityId);
        }
    }

    public Set<Role> getUserRolesForEntity(EntityIdentifier entityId)
    {
        return rolesByEntity.get(entityId);
    }
}
