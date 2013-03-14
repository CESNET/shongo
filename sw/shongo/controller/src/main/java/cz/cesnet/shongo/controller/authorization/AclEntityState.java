package cz.cesnet.shongo.controller.authorization;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AclEntityState
{
    private Set<AclRecord> aclRecords = new HashSet<AclRecord>();
    private Map<String, AclRecord> aclRecordById = new HashMap<String, AclRecord>();

    public void addAclRecord(AclRecord aclRecord)
    {
        if (aclRecords.add(aclRecord)) {
            aclRecordById.put(aclRecord.getId(), aclRecord);
        }
    }

    public void removeAclRecord(AclRecord aclRecord)
    {
        if (aclRecords.remove(aclRecord)) {
            aclRecordById.remove(aclRecord.getId());
        }
    }

    public Set<AclRecord> getAclRecords()
    {
        return aclRecords;
    }

}
