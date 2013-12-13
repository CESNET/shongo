package cz.cesnet.shongo.controller.authorization;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.EntityRole;
import cz.cesnet.shongo.controller.acl.AclEntry;
import cz.cesnet.shongo.controller.acl.AclIdentity;

import java.util.*;

/**
 * Represents an object state in the {@link AuthorizationCache}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AclObjectState
{
    /**
     * Set of {@link AclEntry}s for the object.
     */
    private Map<Long, AclEntry> aclRecords = new HashMap<Long, AclEntry>();

    /**
     * Map of user-ids which by role which has the role for the object.
     */
    private Map<EntityRole, Set<String>> userIdsByRole = new HashMap<EntityRole, Set<String>>();

    /**
     * @param aclEntry to be added to the {@link AclObjectState}
     */
    public synchronized void addAclRecord(AclEntry aclEntry)
    {
        Long aclRecordId = aclEntry.getId();
        if (aclRecords.put(aclRecordId, aclEntry) == null) {
            EntityRole role = EntityRole.valueOf(aclEntry.getRole());
            Set<String> userIds = userIdsByRole.get(role);
            if (userIds == null) {
                userIds = new HashSet<String>();
                userIdsByRole.put(role, userIds);
            }
            AclIdentity identity = aclEntry.getIdentity();
            switch (identity.getType()) {
                case USER:
                    userIds.add(identity.getPrincipalId());
                    break;
                default:
                    throw new TodoImplementException(identity.getType());
            }
        }
    }

    /**
     * @param aclEntry to be removed from the {@link AclObjectState}
     */
    public synchronized void removeAclRecord(AclEntry aclEntry)
    {
        Long aclRecordId = aclEntry.getId();
        if (aclRecords.remove(aclRecordId) != null) {
            EntityRole role = EntityRole.valueOf(aclEntry.getRole());
            Set<String> userIds = userIdsByRole.get(role);
            if (userIds != null) {
                AclIdentity identity = aclEntry.getIdentity();
                switch (identity.getType()) {
                    case USER:
                        userIds.remove(identity.getPrincipalId());
                        break;
                    default:
                        throw new TodoImplementException(identity.getType());
                }
                if (userIds.size() == 0) {
                    userIdsByRole.remove(role);
                }
            }
        }
    }

    /**
     * @return {@link #aclRecords}
     */
    public synchronized Collection<AclEntry> getAclRecords()
    {
        return aclRecords.values();
    }

    /**
     * @return {@link Set} of user-ids which has the given {@code role} for the object
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
