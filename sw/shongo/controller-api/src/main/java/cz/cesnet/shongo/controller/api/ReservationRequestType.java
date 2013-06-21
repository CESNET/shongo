package cz.cesnet.shongo.controller.api;

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
     * Reservation request is newly created.
     */
    NEW,

    /**
     * Reservation request is modification of another reservation request.
     */
    MODIFIED,

    /**
     * Reservation request is deleted.
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
