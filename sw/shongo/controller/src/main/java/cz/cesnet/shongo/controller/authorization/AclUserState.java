package cz.cesnet.shongo.controller.authorization;

import cz.cesnet.shongo.controller.EntityType;
import cz.cesnet.shongo.controller.Permission;
import cz.cesnet.shongo.controller.Role;
import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.fault.TodoImplementException;

import java.util.*;

/**
 * Represents an user state in the {@link AuthorizationCache}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AclUserState
{
    /**
     * Set of {@link AclRecord}s for the user.
     */
    private Set<AclRecord> aclRecords = new HashSet<AclRecord>();

    /**
     * {@link EntityState} for the user.
     */
    private Map<EntityIdentifier, EntityState> entityStateByEntityId = new HashMap<EntityIdentifier, EntityState>();

    /**
     * Map of entities which are accessible to the user (he has {@link Permission#READ} for them) by {@link EntityType}.
     */
    private Map<EntityType, Set<Long>> accessibleEntitiesByType = new HashMap<EntityType, Set<Long>>();

    /**
     * @param aclRecord to be added to the {@link AclUserState}
     * @return given {@code aclRecord} if it does not exist,
     *         already existing {@link AclRecord} otherwise
     */
    public synchronized AclRecord addAclRecord(AclRecord aclRecord)
    {
        EntityIdentifier entityId = aclRecord.getEntityId();
        if (aclRecords.add(aclRecord)) {
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

            // Update accessible entities
            if (entityState.permissions.contains(Permission.READ)) {
                Set<Long> entities = accessibleEntitiesByType.get(entityType);
                if (entities == null) {
                    entities = new HashSet<Long>();
                    accessibleEntitiesByType.put(entityType, entities);
                }
                entities.add(entityId.getPersistenceId());
            }
            return aclRecord;
        }
        else {
            EntityState entityState = entityStateByEntityId.get(entityId);
            for (AclRecord existingAclRecord : entityState.aclRecords) {
                if (existingAclRecord.equals(aclRecord)) {
                    return existingAclRecord;
                }
            }
        }
        throw new IllegalStateException();
    }

    /**
     * @param aclRecord to be removed from the {@link AclUserState}
     */
    public synchronized void removeAclRecord(AclRecord aclRecord)
    {
        if (aclRecords.remove(aclRecord)) {
            EntityIdentifier entityId = aclRecord.getEntityId();
            EntityState entityState = entityStateByEntityId.get(entityId);
            if (entityState == null) {
                return;
            }

            // Update records
            entityState.aclRecords.remove(aclRecord);

            // Update permissions
            entityState.permissions.clear();
            EntityType entityType = entityId.getEntityType();
            for (AclRecord existingAclRecord : entityState.aclRecords) {
                for (Permission permission : entityType.getRolePermissions(existingAclRecord.getRole())) {
                    entityState.permissions.add(permission);
                }
            }

            // Update accessible entities
            if (!entityState.permissions.contains(Permission.READ)) {
                Set<Long> entities = accessibleEntitiesByType.get(entityType);
                if (entities != null) {
                    entities.remove(entityId.getPersistenceId());
                    if (entities.size() == 0) {
                        accessibleEntitiesByType.remove(entityType);
                    }
                }
            }

            // Remove entity states
            if (entityState.aclRecords.size() == 0) {
                entityStateByEntityId.remove(entityId);
            }
        }
    }

    /**
     * @param entityId for which the {@link AclRecord}s should be returned
     * @return set of {@link AclRecord}s for given {@code entityId}
     */
    public synchronized Set<AclRecord> getAclRecords(EntityIdentifier entityId)
    {
        EntityState entityState = entityStateByEntityId.get(entityId);
        if (entityState != null) {
            return Collections.unmodifiableSet(entityState.aclRecords);
        }
        return null;
    }

    /**
     * @param entityId for which the {@link Permission}s should be returned
     * @return set of {@link Permission} for given {@code entityId}
     */
    public synchronized Set<Permission> getPermissions(EntityIdentifier entityId)
    {
        EntityState entityState = entityStateByEntityId.get(entityId);
        if (entityState != null) {
            return Collections.unmodifiableSet(entityState.permissions);
        }
        return null;
    }

    /**
     * @param entityType of which the {@link EntityIdentifier}s should be returned
     * @param permission which the user must have for the returned {@link EntityIdentifier}s
     * @return set of {@link EntityIdentifier}s of given {@code entityType}
     *         for which the user has given {@code permission}
     */
    public synchronized Set<Long> getEntitiesByPermission(EntityType entityType, Permission permission)
    {
        if (!permission.equals(Permission.READ)) {
            throw new TodoImplementException(permission.getCode());
        }
        Set<Long> entities = accessibleEntitiesByType.get(entityType);
        if (entities != null) {
            return Collections.unmodifiableSet(entities);
        }
        return null;
    }

    /**
     * Represents an entity state for the user.
     */
    private static class EntityState
    {
        /**
         * Set of {@link AclRecord}s for the entity.
         */
        Set<AclRecord> aclRecords = new HashSet<AclRecord>();

        /**
         * Set of {@link Permission}s for the entity.
         */
        Set<Permission> permissions = new HashSet<Permission>();
    }
}
