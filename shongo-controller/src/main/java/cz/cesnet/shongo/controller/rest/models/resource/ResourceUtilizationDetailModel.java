package cz.cesnet.shongo.controller.rest.models.resource;

import lombok.Data;

import java.util.List;

/**
 * Represents a utilization of {@link ResourceCapacity} in a specific interval.
 *
 * @author Filip Karnis
 */
@Data
public class ResourceUtilizationDetailModel
{

    private String id;
    private String name;
    private int totalCapacity;
    private int usedCapacity;
    private List<ReservationModel> reservations;

    public static ResourceUtilizationDetailModel fromApi(
            ResourceCapacityUtilization resourceCapacityUtilization,
            ResourceCapacity.Room roomCapacity,
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
        resourceUtilizationDetailModel.setReservations(reservations);
        return resourceUtilizationDetailModel;
    }
}
