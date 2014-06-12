package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.controller.api.AbstractReservationRequest;
import cz.cesnet.shongo.controller.api.UserSettings;

/**
 * Enumeration of all possible permissions for the whole system.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public enum SystemPermission
{
    /**
     * User can switch on the {@link UserSettings#administrationMode}.
     */
    ADMINISTRATION,

    /**
     * User can see everything.
     */
    OPERATOR,

    /**
     * User can create new {@link AbstractReservationRequest}s.
     */
    RESERVATION
}
