package cz.cesnet.shongo.client.web.models;

import cz.cesnet.shongo.controller.api.ExecutableState;

/**
* Represents a room state.
*
* @author Martin Srom <martin.srom@cesnet.cz>
*/
public enum RoomState
{
    NOT_STARTED(ExecutableState.NOT_STARTED.isAvailable()),

    /**
     * {@link cz.cesnet.shongo.controller.api.Executable} is already started.
     */
    STARTED(ExecutableState.STARTED.isAvailable()),

    /**
     * {@link cz.cesnet.shongo.controller.api.Executable} failed to start.
     */
    STARTING_FAILED(ExecutableState.STARTING_FAILED.isAvailable()),

    /**
     * {@link cz.cesnet.shongo.controller.api.Executable} has been already stopped.
     */
    STOPPED(ExecutableState.STOPPED.isAvailable()),

    /**
     * {@link cz.cesnet.shongo.controller.api.Executable} failed to stop.
     */
    STOPPING_FAILED(ExecutableState.STOPPING_FAILED.isAvailable()),

    /**
     * Permanent room is not available for participants to join.
     */
    NOT_AVAILABLE(false),

    /**
     * Permanent room is available for participants to join.
     */
    AVAILABLE(true),

    /**
     * Permanent room is not available for participants to join due to error.
     */
    FAILED(false);

    /**
     * Specifies whether this state represents an available room.
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
     * @param permanentRoom
     * @param licenseCount
     * @return {@link RoomState}
     */
    public static RoomState fromRoomState(ExecutableState roomState, boolean permanentRoom, int licenseCount)
    {
        if (permanentRoom) {
            switch (roomState) {
                case STARTED:
                case STOPPING_FAILED:
                    if (licenseCount > 0) {
                        return RoomState.AVAILABLE;
                    }
                    break;
                case STARTING_FAILED:
                    return RoomState.FAILED;
                case STOPPED:
                    return RoomState.STOPPED;
            }
            return NOT_AVAILABLE;
        }
        else {
            return RoomState.valueOf(roomState.toString());
        }
    }
}
