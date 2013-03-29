package cz.cesnet.shongo.controller.authorization;

import cz.cesnet.shongo.controller.Role;
import cz.cesnet.shongo.controller.common.EntityIdentifier;

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
    private Set<AclRecord> aclRecords = new HashSet<AclRecord>();

    /**
     * Map of user-ids which by role which has the role for the entity.
     */
    private Map<Role, Set<String>> userIdsByRole = new HashMap<Role, Set<String>>();

    /**
     * @param aclRecord to be added to the {@link AclEntityState}
     */
    public synchronized void addAclRecord(AclRecord aclRecord)
    {
        if (aclRecords.add(aclRecord)) {
            Role role = aclRecord.getRole();
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
        if (aclRecords.remove(aclRecord)) {
            Role role = aclRecord.getRole();
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
    public synchronized Set<AclRecord> getAclRecords()
    {
        return Collections.unmodifiableSet(aclRecords);
    }

    /**
     * @return {@link Set} of user-ids which has the given {@code role} for the entity
     */
    public synchronized Set<String> getUserIdsByRole(Role role)
    {
        Set<String> userIds = userIdsByRole.get(role);
        if (userIds != null) {
            return Collections.unmodifiableSet(userIds);
        }
        return null;
    }
}
