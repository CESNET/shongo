package cz.cesnet.shongo.client.web.models;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.api.ExecutableSummary;
import cz.cesnet.shongo.controller.api.ReservationRequestSummary;

/**
 * Type of {@link RoomModel}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public enum RoomType
{
    /**
     * Ad-hoc room.
     */
    ADHOC_ROOM,

    /**
     * Permanent room.
     */
    PERMANENT_ROOM,

    /**
     * Used room.
     */
    USED_ROOM;

    /**
     * @param executableSummary
     * @return {@link cz.cesnet.shongo.client.web.models.RoomType} from given {@code executableSummary}
     */
    public static RoomType fromExecutableSummary(ExecutableSummary executableSummary)
    {
        if (executableSummary.getType().equals(ExecutableSummary.Type.ROOM)) {
            if (executableSummary.getRoomLicenseCount() == 0) {
                return PERMANENT_ROOM;
            }
            else {
                return ADHOC_ROOM;
            }
        }
        else if (executableSummary.getType().equals(ExecutableSummary.Type.USED_ROOM)) {
            return USED_ROOM;
        }
        else {
            throw new TodoImplementException(executableSummary.getType());
        }
    }
}
