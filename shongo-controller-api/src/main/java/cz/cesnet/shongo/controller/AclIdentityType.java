package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.controller.api.AclEntry;

/**
 * Represents type of identity for {@link AclEntry}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */

public enum AclIdentityType
{
    /**
     * User.
     */
    USER,

    /**
     * Group of users.
     */
    GROUP
}
