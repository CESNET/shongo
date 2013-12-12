package cz.cesnet.shongo.controller.authorization;

import cz.cesnet.shongo.controller.EntityRole;

import java.util.*;

/**
 * Represents an entity state in the {@link AuthorizationCache}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AclEntityState
{
    /**
     * Set of {@link AclRecord}s for the entity.
     */
    private Map<Long, AclRecord> aclRecords = new HashMap<Long, AclRecord>();

    /**
     * Map of user-ids which by role which has the role for the entity.
     */
    private Map<EntityRole, Set<String>> userIdsByRole = new HashMap<EntityRole, Set<String>>();

    /**
     * @param aclRecord to be added to the {@link AclEntityState}
     */
    public synchronized void addAclRecord(AclRecord aclRecord)
    {
        Long aclRecordId = aclRecord.getId();
        if (aclRecords.put(aclRecordId, aclRecord) == null) {
            EntityRole role = aclRecord.getEntityRole();
            Set<String> userIds = userIdsByRole.get(role);
            if (userIds == null) {
                userIds = new HashSet<String>();
                userIdsByRole.put(role, userIds);
            }
            userIds.add(aclRecord.getUserId());
        }
    }

    /**
     * @param aclRecord to be removed from the {@link AclEntityState}
     */
    public synchronized void removeAclRecord(AclRecord aclRecord)
    {
        Long aclRecordId = aclRecord.getId();
        if (aclRecords.remove(aclRecordId) != null) {
            EntityRole role = aclRecord.getEntityRole();
            Set<String> userIds = userIdsByRole.get(role);
            if (userIds != null) {
                userIds.remove(aclRecord.getUserId());
                if (userIds.size() == 0) {
                    userIdsByRole.remove(role);
                }
            }
        }
    }

    /**
     * @return {@link #aclRecords}
     */
    public synchronized Collection<AclRecord> getAclRecords()
    {
        return aclRecords.values();
    }

    /**
     * @return {@link Set} of user-ids which has the given {@code role} for the entity
     */
    public synchronized Set<String> getUserIdsByRole(EntityRole role)
    {
        Set<String> userIds = userIdsByRole.get(role);
        if (userIds != null) {
            return Collections.unmodifiableSet(userIds);
        }
        return null;
    }
}
