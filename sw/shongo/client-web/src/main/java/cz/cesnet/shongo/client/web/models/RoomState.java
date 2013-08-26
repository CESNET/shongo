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
    NOT_STARTED(false, false),

    /**
     * Room is started.
     */
    STARTED(true, true),

    /**
     * Room is not available for participants to join.
     */
    STARTED_NOT_AVAILABLE(true, false),

    /**
     * Room is available for participants to join.
     */
    STARTED_AVAILABLE(true, true),

    /**
     * Room has been stopped.
     */
    STOPPED(true, false),

    /**
     * Room is not available for participants to join due to error.
     */
    FAILED(false, false);

    /**
     * Specifies whether this state represents an started room.
     */
    private final boolean isStarted;

    /**
     * Specifies whether this state represents an available for participants to join room.
     */
    private final boolean isAvailable;

    /**
     * Constructor.
     *
     * @param isAvailable sets the {@link #isAvailable}
     */
    RoomState(boolean isStarted, boolean isAvailable)
    {
        this.isStarted = isStarted;
        this.isAvailable = isAvailable;
    }

    /**
     * @return {@link #isStarted}
     */
    public boolean isStarted()
    {
        return isStarted;
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
