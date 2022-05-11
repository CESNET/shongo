package cz.cesnet.shongo.controller.rest.models.resource;

import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.api.ReservationSummary;
import cz.cesnet.shongo.controller.rest.models.TimeInterval;
import lombok.Data;

@Data
public class ReservationModel
{

    private String id;
    private TimeInterval slot;
    private int licenseCount;
    private String requestId;
    private UserInformation user;

    public static ReservationModel fromApi(ReservationSummary reservationSummary, UserInformation user)
    {
        ReservationModel reservationModel = new ReservationModel();
        reservationModel.setId(reservationSummary.getId());
        reservationModel.setSlot(TimeInterval.fromApi(reservationSummary.getSlot()));
        reservationModel.setLicenseCount(reservationSummary.getRoomLicenseCount());
        reservationModel.setRequestId(reservationSummary.getReservationRequestId());
        reservationModel.setUser(user);
        return reservationModel;
    }
}
