package cz.cesnet.shongo.controller;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Enumeration of available types for reservation request.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public enum ReservationRequestType
{
    /**
     * Reservation request is created which means that it is visible to users.
     */
    CREATED,

    /**
     * Reservation request is modified which means that another reservation request
     * replaces it and it is preserved only for history purposes.
     */
    MODIFIED,

    /**
     * Reservation request is deleted which means that it is not visible to users and it is
     * preserved only for history purposes.
     */
    DELETED;

    /**
     * Set of all {@link ReservationRequestType}s.
     */
    public static final Set<ReservationRequestType> ALL;

    /**
     * Static initialization.
     */
    static {
        Set<ReservationRequestType> allTypes = new HashSet<ReservationRequestType>();
        Collections.addAll(allTypes, ReservationRequestType.values());
        ALL = Collections.unmodifiableSet(allTypes);
    }
}
