package cz.cesnet.shongo.controller.rest.models.reservationrequest;

import cz.cesnet.shongo.controller.api.ReservationRequestSummary;
import lombok.Data;

/**
 * Represents capacity information for {@link cz.cesnet.shongo.controller.api.RoomAvailability}.
 *
 * @author Filip Karnis
 */
@Data
public class RoomCapacityModel
{

    private String roomReservationRequestId;
    private Integer capacityParticipantCount;
    private Boolean hasRoomRecordingService;
    private Boolean hasRoomRecordings;
    private Boolean isRecordingActive;

    public RoomCapacityModel(ReservationRequestSummary summary)
    {
        this.roomReservationRequestId = summary.getReusedReservationRequestId();
        this.capacityParticipantCount = summary.getRoomParticipantCount();
        this.hasRoomRecordingService = summary.hasRoomRecordingService();
        this.hasRoomRecordings = summary.hasRoomRecordings();
    }
}
