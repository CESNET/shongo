package cz.cesnet.shongo.controller.rest.models.resource;

import lombok.Data;
import org.joda.time.Interval;

import java.util.Collection;
import java.util.Map;

@Data
public class ResourceCapacityUtilizationModel
{

    private Collection<ResourceCapacity> resourceCapacitySet;
    private Map<Interval, Map<ResourceCapacity, ResourceCapacityUtilization>> resourceCapacityUtilization;

    public ResourceCapacityUtilizationModel(
            Collection<ResourceCapacity> resourceCapacitySet,
            Map<Interval, Map<ResourceCapacity, ResourceCapacityUtilization>> resourceCapacityUtilization)
    {
        this.resourceCapacitySet = resourceCapacitySet;
        this.resourceCapacityUtilization = resourceCapacityUtilization;
    }
}
