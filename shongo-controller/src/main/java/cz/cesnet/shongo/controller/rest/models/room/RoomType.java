package cz.cesnet.shongo.controller.rest.models.room;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.api.AbstractRoomExecutable;
import cz.cesnet.shongo.controller.api.ExecutableSummary;
import cz.cesnet.shongo.controller.api.RoomExecutable;
import cz.cesnet.shongo.controller.api.UsedRoomExecutable;

/**
 * Type of {@link RoomModel}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public enum RoomType
{

    /**
     * Permanent room.
     */
    VIRTUAL_ROOM,

    /**
     * Used room.
     */
    USED_ROOM;

    /**
     * @param executableSummary
     * @return {@link RoomType} from given {@code executableSummary}
     */
    public static RoomType fromExecutableSummary(ExecutableSummary executableSummary)
    {
        if (executableSummary.getType().equals(ExecutableSummary.Type.ROOM)) {
            return VIRTUAL_ROOM;
        }
        else if (executableSummary.getType().equals(ExecutableSummary.Type.USED_ROOM)) {
            return USED_ROOM;
        }
        else {
            throw new TodoImplementException(executableSummary.getType());
        }
    }

    /**
     * @param roomExecutable
     * @return {@link RoomType} from given {@code roomExecutable}
     */
    public static RoomType fromRoomExecutable(AbstractRoomExecutable roomExecutable)
    {
        if (roomExecutable instanceof RoomExecutable) {
            return VIRTUAL_ROOM;
        }
        else if (roomExecutable instanceof UsedRoomExecutable) {
            return USED_ROOM;
        }
        else {
            throw new TodoImplementException(roomExecutable.getClass());
        }
    }
}
