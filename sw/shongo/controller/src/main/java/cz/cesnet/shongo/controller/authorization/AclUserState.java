package cz.cesnet.shongo.controller.authorization;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.EntityPermission;
import cz.cesnet.shongo.controller.EntityType;

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
    private Map<Long, AclRecord> aclRecords = new HashMap<Long, AclRecord>();

    /**
     * {@link EntityState} for the user.
     */
    private Map<AclRecord.EntityId, EntityState> entityStateByEntityId =
            new HashMap<AclRecord.EntityId, EntityState>();

    /**
     * Map of entities which are accessible to the user (he has {@link cz.cesnet.shongo.controller.EntityPermission#READ} for them) by {@link EntityType}.
     */
    private Map<AclRecord.EntityType, Set<Long>> accessibleEntitiesByType =
            new HashMap<AclRecord.EntityType, Set<Long>>();

    /**
     * @param aclRecord to be added to the {@link AclUserState}
     */
    public synchronized void addAclRecord(AclRecord aclRecord)
    {
        Long aclRecordId = aclRecord.getId();
        AclRecord.EntityId entityId = aclRecord.getEntityId();
        if (aclRecords.put(aclRecordId, aclRecord) == null) {
            EntityState entityState = entityStateByEntityId.get(entityId);
            if (entityState == null) {
                entityState = new EntityState();
                entityStateByEntityId.put(entityId, entityState);
            }

            // Update records
            entityState.aclRecords.put(aclRecordId, aclRecord);

            // Update permissions
            AclRecord.EntityType aclRecordEntityType = entityId.getEntityType();
            EntityType entityType = aclRecordEntityType.getEntityType();
            for (EntityPermission entityPermission : entityType.getRolePermissions(aclRecord.getEntityRole())) {
                entityState.permissions.add(entityPermission);
            }

            // Update accessible entities
            if (entityState.permissions.contains(EntityPermission.READ)) {
                Set<Long> entities = accessibleEntitiesByType.get(aclRecordEntityType);
                if (entities == null) {
                    entities = new HashSet<Long>();
                    accessibleEntitiesByType.put(aclRecordEntityType, entities);
                }
                entities.add(entityId.getPersistenceId());
            }
        }
    }

    /**
     * @param aclRecord to be removed from the {@link AclUserState}
     */
    public synchronized void removeAclRecord(AclRecord aclRecord)
    {
        Long aclRecordId = aclRecord.getId();
        if (aclRecords.remove(aclRecordId) != null) {
            AclRecord.EntityId entityId = aclRecord.getEntityId();
            EntityState entityState = entityStateByEntityId.get(entityId);
            if (entityState == null) {
                return;
            }

            // Update records
            entityState.aclRecords.remove(aclRecordId);

            // Update permissions
            entityState.permissions.clear();
            AclRecord.EntityType aclRecordEntityType = entityId.getEntityType();
            EntityType entityType = aclRecordEntityType.getEntityType();
            for (AclRecord existingAclRecord : entityState.aclRecords.values()) {
                for (EntityPermission entityPermission : entityType.getRolePermissions(existingAclRecord.getEntityRole())) {
                    entityState.permissions.add(entityPermission);
                }
            }

            // Update accessible entities
            if (!entityState.permissions.contains(EntityPermission.READ)) {
                Set<Long> entities = accessibleEntitiesByType.get(aclRecordEntityType);
                if (entities != null) {
                    entities.remove(entityId.getPersistenceId());
                    if (entities.size() == 0) {
                        accessibleEntitiesByType.remove(aclRecordEntityType);
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
    public synchronized Collection<AclRecord> getAclRecords(AclRecord.EntityId entityId)
    {
        EntityState entityState = entityStateByEntityId.get(entityId);
        if (entityState != null) {
            return entityState.aclRecords.values();
        }
        return null;
    }

    /**
     * @param entityId for which the {@link cz.cesnet.shongo.controller.EntityPermission}s should be returned
     * @return set of {@link cz.cesnet.shongo.controller.EntityPermission} for given {@code entityId}
     */
    public synchronized Set<EntityPermission> getEntityPermissions(AclRecord.EntityId entityId)
    {
        EntityState entityState = entityStateByEntityId.get(entityId);
        if (entityState != null) {
            return Collections.unmodifiableSet(entityState.permissions);
        }
        return null;
    }

    /**
     * @param entityId for which the {@link cz.cesnet.shongo.controller.EntityPermission}s should be returned
     * @return true if the user has given {@code permission} for the entity,
     *         false otherwise
     */
    public synchronized boolean hasEntityPermission(AclRecord.EntityId entityId, EntityPermission entityPermission)
    {
        EntityState entityState = entityStateByEntityId.get(entityId);
        return entityState != null && entityState.permissions.contains(entityPermission);
    }

    /**
     * @param entityType of which the {@link AclRecord.EntityId}s should be returned
     * @param entityPermission which the user must have for the returned {@link AclRecord.EntityId}s
     * @return set of {@link AclRecord.EntityId}s of given {@code entityType}
     *         for which the user has given {@code permission}
     */
    public synchronized Set<Long> getEntitiesByPermission(
            AclRecord.EntityType entityType, EntityPermission entityPermission)
    {
        if (!entityPermission.equals(EntityPermission.READ)) {
            throw new TodoImplementException(entityPermission);
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
        private Map<Long, AclRecord> aclRecords = new HashMap<Long, AclRecord>();

        /**
         * Set of {@link cz.cesnet.shongo.controller.EntityPermission}s for the entity.
         */
        Set<EntityPermission> permissions = new HashSet<EntityPermission>();
    }
}
