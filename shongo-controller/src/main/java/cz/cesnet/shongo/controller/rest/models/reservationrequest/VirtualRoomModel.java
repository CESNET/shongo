package cz.cesnet.shongo.controller.rest.models.reservationrequest;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.api.ExecutableState;
import cz.cesnet.shongo.controller.api.ReservationRequestSummary;
import cz.cesnet.shongo.controller.rest.models.TechnologyModel;
import lombok.Data;

import java.util.Set;

@Data
public class VirtualRoomModel {

    private String roomName;
    private ExecutableState state;
    private TechnologyModel technology;
    private Boolean hasRoomRecordings;

    public VirtualRoomModel(ReservationRequestSummary summary) {
        this.roomName = summary.getRoomName();
        this.hasRoomRecordings = summary.hasRoomRecordings();
        this.state = summary.getExecutableState();

        Set<Technology> technologies = summary.getSpecificationTechnologies();
        this.technology = TechnologyModel.find(technologies);
    }
}
