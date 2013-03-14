package cz.cesnet.shongo.controller.authorization;

import cz.cesnet.shongo.controller.EntityType;
import cz.cesnet.shongo.controller.Permission;
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
public class AclUserState
{

    private Set<AclRecord> aclRecords = new HashSet<AclRecord>();
    private Map<String, AclRecord> aclRecordById = new HashMap<String, AclRecord>();

    private Map<EntityIdentifier, EntityState> entityStateByEntityId = new HashMap<EntityIdentifier, EntityState>();

    public void addAclRecord(AclRecord aclRecord)
    {
        if (aclRecords.add(aclRecord)) {
            aclRecordById.put(aclRecord.getId(), aclRecord);

            EntityIdentifier entityId = aclRecord.getEntityId();
            EntityState entityState = entityStateByEntityId.get(entityId);
            if (entityState == null) {
                entityState = new EntityState();
                entityStateByEntityId.put(entityId, entityState);
            }

            // Update records
            entityState.aclRecords.add(aclRecord);

            // Update permissions
            EntityType entityType = entityId.getEntityType();
            for (Permission permission : entityType.getRolePermissions(aclRecord.getRole())) {
                entityState.permissions.add(permission);
            }
        }
    }

    public void removeAclRecord(AclRecord aclRecord)
    {
        if (aclRecords.remove(aclRecord)) {
            aclRecordById.remove(aclRecord.getId());

            EntityIdentifier entityId = aclRecord.getEntityId();
            EntityState entityState = entityStateByEntityId.get(entityId);
            if (entityState == null) {
                return;
            }

            // Update records
            entityState.aclRecords.remove(aclRecord);

            // Update permissions
            entityState.permissions.clear();
            for (AclRecord existingAclRecord : entityState.aclRecords) {
                EntityType entityType = entityId.getEntityType();
                for (Permission permission : entityType.getRolePermissions(existingAclRecord.getRole())) {
                    entityState.permissions.add(permission);
                }
            }

            // Remove entity states
            if (entityState.aclRecords.size() == 0) {
                entityStateByEntityId.remove(entityId);
            }
        }
    }

    public Set<AclRecord> getAclRecords(EntityIdentifier entityId)
    {
        EntityState entityState = entityStateByEntityId.get(entityId);
        if (entityState != null) {
            return entityState.aclRecords;
        }
        return null;
    }

    public Set<Permission> getPermissions(EntityIdentifier entityId)
    {
        EntityState entityState = entityStateByEntityId.get(entityId);
        if (entityState != null) {
            return entityState.permissions;
        }
        return null;
    }

    private static class EntityState
    {
        Set<AclRecord> aclRecords = new HashSet<AclRecord>();

        Set<Permission> permissions = new HashSet<Permission>();
    }
}
