package cz.cesnet.shongo.common.api;

import cz.cesnet.shongo.common.xmlrpc.StructType;

/**
 * Represents a user identity
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class UserIdentity implements StructType
{
    /**
     * eduID.cz identity
     */
    private String id;

    public String getId()
    {
        return id;
    }
}
