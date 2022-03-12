package cz.cesnet.shongo.controller.rest.models.reservationrequest;

import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.ObjectPermission;
import cz.cesnet.shongo.controller.api.ReservationRequestSummary;
import cz.cesnet.shongo.controller.api.ReservationRequestType;
import cz.cesnet.shongo.controller.rest.models.TimeInterval;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.joda.time.DateTime;

import java.util.Map;
import java.util.Set;

/**
 * Represents reservation request.
 *
 * @author Filip Karnis
 */
@Data
@NoArgsConstructor
public class ReservationRequestModel {

    private String id;
    private String description;
    private DateTime createdAt;
    private String parentRequestId;
    private ReservationRequestState state;
    private Boolean isWritable;
    private Boolean isProvidable;
    private String ownerName;
    private String ownerEmail;
    private TimeInterval slot;
    private Boolean isDeprecated;
    private ReservationRequestType type;
    private VirtualRoomModel virtualRoomData;
    private RoomCapacityModel roomCapacityData;
    private String lastReservationId;
    private Integer futureSlotCount;

    public ReservationRequestModel(
            ReservationRequestSummary summary,
            Map<String, Set<ObjectPermission>> permissionsByReservationRequestId,
            UserInformation ownerInformation)
    {
        this.id = summary.getId();
        this.description = summary.getDescription();
        this.createdAt = summary.getDateTime();
        this.parentRequestId = summary.getParentReservationRequestId();
        this.state = ReservationRequestState.fromApi(summary);
        this.ownerName = ownerInformation.getFullName();
        this.ownerEmail = ownerInformation.getEmail();
        this.slot = new TimeInterval(summary.getEarliestSlot());
        this.type = summary.getType();
        this.virtualRoomData = new VirtualRoomModel(summary);
        this.roomCapacityData = new RoomCapacityModel(summary);
        this.lastReservationId = summary.getLastReservationId();
        this.futureSlotCount = summary.getFutureSlotCount();

        Set<ObjectPermission> objectPermissions = permissionsByReservationRequestId.get(id);
        this.isWritable = objectPermissions.contains(ObjectPermission.WRITE);
        this.isProvidable = objectPermissions.contains(ObjectPermission.PROVIDE_RESERVATION_REQUEST);

        switch (state != null ? state : ReservationRequestState.ALLOCATED) {
            case ALLOCATED_STARTED:
            case ALLOCATED_STARTED_AVAILABLE:
                this.isDeprecated = false;
                break;
            default:
                this.isDeprecated = slot != null && slot.getEnd().isBeforeNow();
                break;
        }
    }
}
