package cz.cesnet.shongo.controller.rest.models.reservationrequest;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.Controller;
import cz.cesnet.shongo.controller.ControllerConfiguration;
import cz.cesnet.shongo.controller.api.ReservationRequestSummary;

/**
 * Type of specification for a reservation request.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @author Filip Karnis
 */
public enum SpecificationType
{
    /**
     * For permanent room.
     */
    VIRTUAL_ROOM(true, false),

    /**
     * For room capacity.
     */
    ROOM_CAPACITY(false, false),

    /**
     * For physical resource.
     */
    PHYSICAL_RESOURCE(false, true),

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
     * @param isRoom     sets the {@link #isRoom}
     * @param isPhysical
     */
    SpecificationType(boolean isRoom, boolean isPhysical)
    {
        this.isRoom = isRoom;
        this.isPhysical = isPhysical;
    }

    /**
     * @param reservationRequestSummary
     * @return {@link SpecificationType} from given {@code reservationRequestSummary}
     */
    public static SpecificationType fromReservationRequestSummary(ReservationRequestSummary reservationRequestSummary)
    {
        return fromReservationRequestSummary(reservationRequestSummary, false);
    }

    /**
     * @param reservationRequestSummary
     * @return {@link SpecificationType} from given {@code reservationRequestSummary}
     */
    public static SpecificationType fromReservationRequestSummary(
            ReservationRequestSummary reservationRequestSummary,
            boolean onlyGeneralType)
    {
        ControllerConfiguration configuration = getControllerConfiguration();

        switch (reservationRequestSummary.getSpecificationType()) {
            case ROOM:
            case PERMANENT_ROOM:
                return VIRTUAL_ROOM;
            case USED_ROOM:
                return ROOM_CAPACITY;
            case RESOURCE:
                if (onlyGeneralType) {
                    return PHYSICAL_RESOURCE;
                }
                String resourceTags = reservationRequestSummary.getResourceTags();
                String parkTagName = configuration.getParkingPlaceTagName();
                String vehicleTagName = configuration.getVehicleTagName();
                if (resourceTags != null) {
                    if (parkTagName != null && resourceTags.contains(parkTagName)) {
                        return PARKING_PLACE;
                    }
                    else if (vehicleTagName != null && resourceTags.contains(vehicleTagName)) {
                        return VEHICLE;
                    }
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
        ControllerConfiguration configuration = getControllerConfiguration();

        if (string == null) {
            return null;
        }
        else if (string.equals(configuration.getMeetingRoomTagName())) {
            return MEETING_ROOM;
        }
        else if (string.equals(configuration.getVehicleTagName())) {
            return VEHICLE;
        }
        else if (string.equals(configuration.getParkingPlaceTagName())) {
            return PARKING_PLACE;
        }
        throw new TodoImplementException("SpecificationType.fromString for " + string);

    }

    private static ControllerConfiguration getControllerConfiguration()
    {
        Controller controller = Controller.getInstance();
        return controller.getConfiguration();
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
    public boolean isPhysical()
    {
        return isPhysical;
    }
}
