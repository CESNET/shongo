package cz.cesnet.shongo.controller.rest.models.resource;

import cz.cesnet.shongo.controller.rest.models.TimeInterval;
import lombok.Data;
import org.joda.time.Interval;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class ResourceUtilizationModel
{

    private TimeInterval interval;
    private List<UtilizationModel> resources;

    public static ResourceUtilizationModel fromApi(
            Interval interval,
            Map<ResourceCapacity, ResourceCapacityUtilization> resourceCapacityUtilizations)
    {
        List<UtilizationModel> resources = new ArrayList<>();
        resourceCapacityUtilizations.forEach((resourceCapacity, resourceCapacityUtilization) -> {
            ResourceCapacity.Room roomCapacity = (ResourceCapacity.Room) resourceCapacity;

            UtilizationModel utilizationModel = new UtilizationModel();
            utilizationModel.setId(roomCapacity.getResourceId());
            utilizationModel.setName(roomCapacity.getResourceName());
            utilizationModel.setTotalCapacity(roomCapacity.getLicenseCount());
            utilizationModel.setUsedCapacity((resourceCapacityUtilization != null)
                    ? resourceCapacityUtilization.getPeakBucket().getLicenseCount() : 0);
            resources.add(utilizationModel);
        });

        ResourceUtilizationModel resourceUtilizationModel = new ResourceUtilizationModel();
        resourceUtilizationModel.setInterval(TimeInterval.fromApi(interval));
        resourceUtilizationModel.setResources(resources);
        return resourceUtilizationModel;
    }

    @Data
    public static class UtilizationModel
    {

        private String id;
        private String name;
        private int totalCapacity;
        private int usedCapacity;
    }
}
