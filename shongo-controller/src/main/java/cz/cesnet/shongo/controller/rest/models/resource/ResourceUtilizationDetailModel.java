package cz.cesnet.shongo.controller.rest.models.resource;

import cz.cesnet.shongo.controller.rest.models.TimeInterval;
import lombok.Data;
import org.joda.time.Interval;

import java.util.List;

@Data
public class ResourceUtilizationDetailModel
{

    private String id;
    private String name;
    private int totalCapacity;
    private int usedCapacity;
    private TimeInterval interval;
    private List<ReservationModel> reservations;

    public static ResourceUtilizationDetailModel fromApi(
            ResourceCapacityUtilization resourceCapacityUtilization,
            ResourceCapacity.Room roomCapacity,
            Interval interval,
            List<ReservationModel> reservations)
    {
        int licenseCount = (resourceCapacityUtilization != null)
                ? resourceCapacityUtilization.getPeakBucket().getLicenseCount()
                : 0;

        ResourceUtilizationDetailModel resourceUtilizationDetailModel = new ResourceUtilizationDetailModel();
        resourceUtilizationDetailModel.setId(roomCapacity.getResourceId());
        resourceUtilizationDetailModel.setName(roomCapacity.getResourceName());
        resourceUtilizationDetailModel.setTotalCapacity(roomCapacity.getLicenseCount());
        resourceUtilizationDetailModel.setUsedCapacity(licenseCount);
        resourceUtilizationDetailModel.setInterval(TimeInterval.fromApi(interval));
        resourceUtilizationDetailModel.setReservations(reservations);
        return resourceUtilizationDetailModel;
    }
}
