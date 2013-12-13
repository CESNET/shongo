package cz.cesnet.shongo.controller.authorization;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.ObjectPermission;
import cz.cesnet.shongo.controller.EntityRole;
import cz.cesnet.shongo.controller.EntityType;
import cz.cesnet.shongo.controller.acl.AclEntry;
import cz.cesnet.shongo.controller.acl.AclObjectClass;
import cz.cesnet.shongo.controller.acl.AclObjectIdentity;
import cz.cesnet.shongo.controller.booking.EntityTypeResolver;

import java.util.*;

/**
 * Represents an user state in the {@link AuthorizationCache}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AclUserState
{
    /**
     * Set of {@link AclEntry}s for the user.
     */
    private Map<Long, AclEntry> aclRecords = new HashMap<Long, AclEntry>();

    /**
     * {@link cz.cesnet.shongo.controller.authorization.AclUserState.ObjectState} for the user.
     */
    private Map<AclObjectIdentity, ObjectState> objectStateByObjectIdentity =
            new HashMap<AclObjectIdentity, ObjectState>();

    /**
     * Map of objects which are accessible to the user (he has {@link ObjectPermission#READ} for them) by {@link AclObjectClass}.
     */
    private Map<AclObjectClass, Set<Long>> accessibleObjectsByClass =
            new HashMap<AclObjectClass, Set<Long>>();

    /**
     * @param aclEntry to be added to the {@link AclUserState}
     */
    public synchronized void addAclRecord(AclEntry aclEntry)
    {
        Long aclRecordId = aclEntry.getId();
        AclObjectIdentity objectIdentity = aclEntry.getObjectIdentity();
        if (aclRecords.put(aclRecordId, aclEntry) == null) {
            ObjectState objectState = objectStateByObjectIdentity.get(objectIdentity);
            if (objectState == null) {
                objectState = new ObjectState();
                objectStateByObjectIdentity.put(objectIdentity, objectState);
            }

            // Update records
            objectState.aclRecords.put(aclRecordId, aclEntry);

            // Update permissions
            AclObjectClass objectClass = objectIdentity.getObjectClass();
            EntityRole entityRole = EntityRole.valueOf(aclEntry.getRole());
            EntityType entityType = EntityTypeResolver.getEntityType(objectClass);
            for (ObjectPermission objectPermission : entityType.getRolePermissions(entityRole)) {
                objectState.permissions.add(objectPermission);
            }

            // Update accessible entities
            if (objectState.permissions.contains(ObjectPermission.READ)) {
                Set<Long> entities = accessibleObjectsByClass.get(objectClass);
                if (entities == null) {
                    entities = new HashSet<Long>();
                    accessibleObjectsByClass.put(objectClass, entities);
                }
                entities.add(objectIdentity.getObjectId());
            }
        }
    }

    /**
     * @param aclEntry to be removed from the {@link AclUserState}
     */
    public synchronized void removeAclRecord(AclEntry aclEntry)
    {
        Long aclRecordId = aclEntry.getId();
        if (aclRecords.remove(aclRecordId) != null) {
            AclObjectIdentity objectIdentity = aclEntry.getObjectIdentity();
            ObjectState objectState = objectStateByObjectIdentity.get(objectIdentity);
            if (objectState == null) {
                return;
            }

            // Update records
            objectState.aclRecords.remove(aclRecordId);

            // Update permissions
            objectState.permissions.clear();
            AclObjectClass objectClass = objectIdentity.getObjectClass();
            EntityType entityType = EntityTypeResolver.getEntityType(objectClass);
            for (AclEntry existingAclEntry : objectState.aclRecords.values()) {
                EntityRole entityRole = EntityRole.valueOf(existingAclEntry.getRole());
                for (ObjectPermission objectPermission : entityType.getRolePermissions(entityRole)) {
                    objectState.permissions.add(objectPermission);
                }
            }

            // Update accessible entities
            if (!objectState.permissions.contains(ObjectPermission.READ)) {
                Set<Long> entities = accessibleObjectsByClass.get(objectClass);
                if (entities != null) {
                    entities.remove(objectIdentity.getObjectId());
                    if (entities.size() == 0) {
                        accessibleObjectsByClass.remove(objectClass);
                    }
                }
            }

            // Remove objects states
            if (objectState.aclRecords.size() == 0) {
                objectStateByObjectIdentity.remove(objectIdentity);
            }
        }
    }

    /**
     * @param objectIdentity for which the {@link AclEntry}s should be returned
     * @return set of {@link AclEntry}s for given {@code entityId}
     */
    public synchronized Collection<AclEntry> getAclRecords(AclObjectIdentity objectIdentity)
    {
        ObjectState objectState = objectStateByObjectIdentity.get(objectIdentity);
        if (objectState != null) {
            return objectState.aclRecords.values();
        }
        return null;
    }

    /**
     * @param objectIdentity for which the {@link ObjectPermission}s should be returned
     * @return set of {@link ObjectPermission} for given {@code objectIdentity}
     */
    public synchronized Set<ObjectPermission> getObjectPermissions(AclObjectIdentity objectIdentity)
    {
        ObjectState objectState = objectStateByObjectIdentity.get(objectIdentity);
        if (objectState != null) {
            return Collections.unmodifiableSet(objectState.permissions);
        }
        return null;
    }

    /**
     * @param objectIdentity for which the {@link ObjectPermission}s should be returned
     * @return true if the user has given {@code permission} for the object,
     *         false otherwise
     */
    public synchronized boolean hasObjectPermission(AclObjectIdentity objectIdentity, ObjectPermission objectPermission)
    {
        ObjectState objectState = objectStateByObjectIdentity.get(objectIdentity);
        return objectState != null && objectState.permissions.contains(objectPermission);
    }

    /**
     * @param objectClass of which the {@link AclObjectIdentity#objectId}s should be returned
     * @param objectPermission which the user must have for the returned {@link AclObjectIdentity#objectId}s
     * @return set of {@link AclObjectIdentity#objectId}s of given {@code objectClass} for which the user has given {@code permission}
     */
    public synchronized Set<Long> getObjectsByPermission(
            AclObjectClass objectClass, ObjectPermission objectPermission)
    {
        if (!objectPermission.equals(ObjectPermission.READ)) {
            throw new TodoImplementException(objectPermission);
        }
        Set<Long> entities = accessibleObjectsByClass.get(objectClass);
        if (entities != null) {
            return Collections.unmodifiableSet(entities);
        }
        return null;
    }

    /**
     * Represents an object state for the user.
     */
    private static class ObjectState
    {
        /**
         * Set of {@link AclEntry}s for the object.
         */
        private Map<Long, AclEntry> aclRecords = new HashMap<Long, AclEntry>();

        /**
         * Set of {@link ObjectPermission}s for the object.
         */
        Set<ObjectPermission> permissions = new HashSet<ObjectPermission>();
    }
}
