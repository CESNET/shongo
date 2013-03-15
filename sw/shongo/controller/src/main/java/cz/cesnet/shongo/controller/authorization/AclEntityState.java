package cz.cesnet.shongo.controller.authorization;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
     * @param aclRecord to be added to the {@link AclEntityState}
     */
    public synchronized void addAclRecord(AclRecord aclRecord)
    {
        aclRecords.add(aclRecord);
    }

    /**
     * @param aclRecord to be removed from the {@link AclEntityState}
     */
    public synchronized void removeAclRecord(AclRecord aclRecord)
    {
        aclRecords.remove(aclRecord);
    }

    /**
     * @return {@link #aclRecords}
     */
    public synchronized Set<AclRecord> getAclRecords()
    {
        return aclRecords;
    }
}
