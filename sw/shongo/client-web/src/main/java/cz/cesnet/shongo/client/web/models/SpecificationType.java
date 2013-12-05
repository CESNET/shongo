package cz.cesnet.shongo.client.web.models;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.api.ReservationRequestSummary;

/**
 * Type of specification for a reservation request.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public enum SpecificationType
{
    /**
     * For ad-hoc room.
     */
    ADHOC_ROOM(true),

    /**
     * For permanent room.
     */
    PERMANENT_ROOM(true),

    /**
     * For permanent room capacity.
     */
    PERMANENT_ROOM_CAPACITY(false);

    /**
     * Specifies whether it is a room.
     */
    private final boolean isRoom;

    /**
     * Constructor.
     *
     * @param isRoom sets the {@link #isRoom}
     */
    private SpecificationType(boolean isRoom)
    {
        this.isRoom = isRoom;
    }

    /**
     * @return {@link #isRoom}
     */
    public boolean isRoom()
    {
        return isRoom;
    }

    /**
     * @param reservationRequestSummary
     * @return {@link SpecificationType} from given {@code reservationRequestSummary}
     */
    public static SpecificationType fromReservationRequestSummary(ReservationRequestSummary reservationRequestSummary)
    {
        switch (reservationRequestSummary.getSpecificationType()) {
            case ROOM:
                return ADHOC_ROOM;

            case PERMANENT_ROOM:
                    return PERMANENT_ROOM;
            case USED_ROOM:
                return PERMANENT_ROOM_CAPACITY;
            default:
                throw new TodoImplementException(reservationRequestSummary.getSpecificationType());
        }
    }
}
