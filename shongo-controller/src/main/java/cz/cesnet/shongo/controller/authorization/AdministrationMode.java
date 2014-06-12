package cz.cesnet.shongo.controller.authorization;

import cz.cesnet.shongo.controller.api.AclEntry;

/**
 * Enumeration of all possible administration modes in which an user can act.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public enum  AdministrationMode
{
    /**
     * User shall have all possible permissions.
     */
    ADMINISTRATOR,

    /**
     * User shall see everything in system but should not be able to modify anything except objects
     * to which he has proper {@link AclEntry}s.
     */
    OPERATOR
}
