package cz.cesnet.shongo.client.web.models;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.client.web.ClientWebConfiguration;
import cz.cesnet.shongo.client.web.support.MessageProvider;
import cz.cesnet.shongo.controller.api.ReservationRequestSummary;
import cz.cesnet.shongo.controller.api.Tag;

import java.util.List;
import java.util.stream.Collectors;

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
    ADHOC_ROOM(true, false),

    /**
     * For permanent room.
     */
    PERMANENT_ROOM(true, false),

    /**
     * For permanent room capacity.
     */
    PERMANENT_ROOM_CAPACITY(false, false),

    /**
     * For meeting room.
     */
    MEETING_ROOM(false, true),

    /**
     * For parking place.
     */
    PARKING_PLACE(false, true),

    /**
     * For vehicles.
     */
    VEHICLE(false, true);

    /**
     * Specifies whether it is a room.
     */
    private final boolean isRoom;

    /**
     * Specifies whether it is physical resource.
     */
    private final boolean isPhysical;

    /**
     * Constructor.
     *
     * @param isRoom sets the {@link #isRoom}
     * @param isPhysical
     */
    private SpecificationType(boolean isRoom, boolean isPhysical)
    {
        this.isRoom = isRoom;
        this.isPhysical = isPhysical;
    }

    /**
     * @return {@link #isRoom}
     */
    public boolean isRoom()
    {
        return isRoom;
    }

    /**
     * @return {@link #isPhysical}
     */
    public boolean isPhysical() {
        return isPhysical;
    }

    /**
     * @param messageProvider
     * @return message for
     */
    public String getForMessage(MessageProvider messageProvider)
    {
        return messageProvider.getMessage("views.specificationType.for." + this);
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
            case RESOURCE:
                List<String> resourceTags = reservationRequestSummary.getResourceTags()
                        .stream()
                        .map(Tag::getName)
                        .collect(Collectors.toList());
                String parkTagName = ClientWebConfiguration.getInstance().getParkingPlaceTagName();
                String vehicleTagName = ClientWebConfiguration.getInstance().getVehicleTagName();
                if (parkTagName != null && resourceTags.contains(parkTagName)) {
                    return PARKING_PLACE;
                }
                else if (vehicleTagName != null && resourceTags.contains(vehicleTagName)) {
                    return VEHICLE;
                }
                return MEETING_ROOM;
            default:
                throw new TodoImplementException(reservationRequestSummary.getSpecificationType());
        }
    }

    /**
     * @param string
     * @return {@link SpecificationType} from given {@code string}
     */
    public static SpecificationType fromString(String string)
    {
        if (string == null) {
            return null;
        } else if (string.equals(ClientWebConfiguration.getInstance().getMeetingRoomTagName())) {
            return MEETING_ROOM;
        } else if (string.equals(ClientWebConfiguration.getInstance().getVehicleTagName())) {
            return VEHICLE;
        } else if (string.equals(ClientWebConfiguration.getInstance().getParkingPlaceTagName())) {
            return PARKING_PLACE;
        }
        throw new TodoImplementException("SpecificationType.fromString for " + string);

    }
}
