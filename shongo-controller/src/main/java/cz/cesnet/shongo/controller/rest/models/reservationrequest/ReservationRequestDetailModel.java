package cz.cesnet.shongo.controller.rest.models.reservationrequest;

import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.ObjectPermission;
import cz.cesnet.shongo.controller.api.AllocationState;
import cz.cesnet.shongo.controller.api.ExecutableState;
import cz.cesnet.shongo.controller.api.ReservationRequestSummary;
import cz.cesnet.shongo.controller.rest.models.room.RoomAuthorizedData;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents reservation request's detail info.
 *
 * @author Filip Karnis
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ReservationRequestDetailModel extends ReservationRequestModel {

    private AllocationState allocationState;
    private ExecutableState executableState;
    private Boolean notifyParticipants; // TODO
    private RoomAuthorizedData authorizedData;
    private List<ReservationRequestHistoryModel> history;

    public ReservationRequestDetailModel(
            ReservationRequestSummary summary,
            Map<String, Set<ObjectPermission>> permissionsByReservationRequestId,
            UserInformation ownerInformation,
            RoomAuthorizedData authorizedData,
            List<ReservationRequestHistoryModel> history)
    {
        super(summary, permissionsByReservationRequestId, ownerInformation);

        this.allocationState = summary.getAllocationState();
        this.executableState = summary.getExecutableState();
        this.authorizedData = authorizedData;
        this.history = history;
    }
}
