package cz.cesnet.shongo.client.web.models;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.api.ExecutableState;

/**
* Represents a room state.
*
* @author Martin Srom <martin.srom@cesnet.cz>
*/
public enum RoomState
{
    /**
     * Room is not started.
     */
    NOT_STARTED(false),

    /**
     * Room is started.
     */
    STARTED(true),

    /**
     * Room is not available for participants to join.
     */
    STARTED_NOT_AVAILABLE(false),

    /**
     * Room is available for participants to join.
     */
    STARTED_AVAILABLE(true),

    /**
     * Room has been stopped.
     */
    STOPPED(false),

    /**
     * Room is not available for participants to join due to error.
     */
    FAILED(false);

    /**
     * Specifies whether this state represents an available for participants to join.available room.
     */
    private final boolean isAvailable;

    /**
     * Constructor.
     *
     * @param isAvailable sets the {@link #isAvailable}
     */
    RoomState(boolean isAvailable)
    {
        this.isAvailable = isAvailable;
    }

    /**
     * @return {@link #isAvailable}
     */
    public boolean isAvailable()
    {
        return isAvailable;
    }

    /**
     * @param roomState
     * @param roomLicenseCount
     * @param roomUsageState
     * @return {@link RoomState}
     */
    public static RoomState fromRoomState(ExecutableState roomState, int roomLicenseCount, ExecutableState roomUsageState)
    {
        switch (roomState) {
            case NOT_STARTED:
                return NOT_STARTED;
            case STARTED:
                if (roomUsageState != null) {
                    // Permanent room with earliest usage
                    return roomUsageState.isAvailable() ? STARTED_AVAILABLE : STARTED_NOT_AVAILABLE;
                }
                else if (roomLicenseCount == 0) {
                    // Permanent room without earliest usage
                    return STARTED_NOT_AVAILABLE;
                }
                else {
                    // Other room
                    return STARTED;
                }
            case STOPPED:
            case STOPPING_FAILED:
                return RoomState.STOPPED;
            case STARTING_FAILED:
                return RoomState.FAILED;
            default:
                throw new TodoImplementException(roomState.toString());
        }
    }
}
