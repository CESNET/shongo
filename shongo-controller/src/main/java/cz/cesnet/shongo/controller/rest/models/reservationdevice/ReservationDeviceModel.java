package cz.cesnet.shongo.controller.rest.models.reservationdevice;

import cz.cesnet.shongo.controller.api.ReservationDevice;
import lombok.Data;

/**
 * Represents a reservation device with authorization to create/view reservations of a particular resource.
 *
 * @author Michal Drobňák
 */
@Data
public class ReservationDeviceModel {
    private String id;
    private String resourceId;

    public ReservationDeviceModel(ReservationDevice reservationDevice) {
        this.id = reservationDevice.getId();
        this.resourceId = reservationDevice.getResourceId();
    }
}
