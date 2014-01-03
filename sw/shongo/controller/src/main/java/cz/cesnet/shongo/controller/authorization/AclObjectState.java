package cz.cesnet.shongo.controller.authorization;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.ObjectRole;
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
    private Map<Long, AclEntry> aclEntries = new HashMap<Long, AclEntry>();

    /**
     * Map of user-ids which by role which has the role for the object.
     */
    private Map<ObjectRole, Set<String>> userIdsByRole = new HashMap<ObjectRole, Set<String>>();

    /**
     * @param aclEntry to be added to the {@link AclObjectState}
     */
    public synchronized void addAclEntry(AclEntry aclEntry, Authorization authorization)
    {
        Long aclEntryId = aclEntry.getId();
        if (aclEntries.put(aclEntryId, aclEntry) == null) {
            ObjectRole role = ObjectRole.valueOf(aclEntry.getRole());
            Set<String> userIds = userIdsByRole.get(role);
            if (userIds == null) {
                userIds = new HashSet<String>();
                userIdsByRole.put(role, userIds);
            }
            userIds.addAll(authorization.getUserIds(aclEntry.getIdentity()));
        }
    }

    /**
     * @param aclEntry to be removed from the {@link AclObjectState}
     */
    public synchronized void removeAclEntry(AclEntry aclEntry, Authorization authorization)
    {
        Long aclEntryId = aclEntry.getId();
        if (aclEntries.remove(aclEntryId) != null) {
            ObjectRole role = ObjectRole.valueOf(aclEntry.getRole());
            Set<String> userIds = userIdsByRole.get(role);
            if (userIds != null) {
                userIds.removeAll(authorization.getUserIds(aclEntry.getIdentity()));
                if (userIds.size() == 0) {
                    userIdsByRole.remove(role);
                }
            }
        }
    }

    /**
     * @return {@link #aclEntries}
     */
    public synchronized Collection<AclEntry> getAclEntries()
    {
        return aclEntries.values();
    }

    /**
     * @return {@link Set} of user-ids which has the given {@code role} for the object
     */
    public synchronized Set<String> getUserIdsByRole(ObjectRole role)
    {
        Set<String> userIds = userIdsByRole.get(role);
        if (userIds != null) {
            return Collections.unmodifiableSet(userIds);
        }
        return null;
    }
}
