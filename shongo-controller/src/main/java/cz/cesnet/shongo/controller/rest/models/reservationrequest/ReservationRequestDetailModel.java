package cz.cesnet.shongo.controller.rest.models.reservationrequest;

import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.ObjectPermission;
import cz.cesnet.shongo.controller.api.AllocationState;
import cz.cesnet.shongo.controller.api.ExecutableState;
import cz.cesnet.shongo.controller.api.ReservationRequestSummary;
import cz.cesnet.shongo.controller.api.ResourceSummary;
import cz.cesnet.shongo.controller.rest.models.room.RoomAuthorizedData;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents reservation request's detail info.
 * It contains additional information about reservation request
 * including {@link RoomAuthorizedData} and {@link List} {@link ReservationRequestHistoryModel}.
 *
 * @author Filip Karnis
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ReservationRequestDetailModel extends ReservationRequestModel
{

    private AllocationState allocationState;
    private ExecutableState executableState;
    private Boolean notifyParticipants;
    private RoomAuthorizedData authorizedData;
    private List<ReservationRequestHistoryModel> history;

    public ReservationRequestDetailModel(
            ReservationRequestSummary summary,
            VirtualRoomModel virtualRoom,
            Map<String, Set<ObjectPermission>> permissionsByReservationRequestId,
            UserInformation ownerInformation,
            RoomAuthorizedData authorizedData,
            List<ReservationRequestHistoryModel> history,
            ResourceSummary resourceSummary)
    {
        super(summary, virtualRoom, permissionsByReservationRequestId, ownerInformation, resourceSummary);

        this.allocationState = summary.getAllocationState();
        this.executableState = summary.getExecutableState();
        this.authorizedData = authorizedData;
        this.history = history;
    }
}
