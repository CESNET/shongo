package cz.cesnet.shongo.client.web.auth;

import cz.cesnet.shongo.controller.SystemPermission;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public enum UserPermission
{
    /**
     * @see SystemPermission#ADMINISTRATION
     */
    ADMINISTRATION,

    /**
     * @see SystemPermission#OPERATOR
     */
    OPERATOR,

    /**
     * @see SystemPermission#RESERVATION
     */
    RESERVATION,

    /**
     * User can see resource management in web interface.
     */
    RESOURCE_MANAGEMENT
}
