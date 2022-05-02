package cz.cesnet.shongo.controller.rest.models.reservationrequest;

import cz.cesnet.shongo.controller.api.ResourceSummary;
import lombok.Data;

@Data
public class PhysicalResourceData
{

    private String resourceId;
    private String resourceName;
    private String resourceDescription;
    private String periodicity;

    public static PhysicalResourceData fromApi(ResourceSummary summary) {
        if (summary == null) {
            return null;
        }
        PhysicalResourceData physicalResourceData = new PhysicalResourceData();
        physicalResourceData.setResourceId(summary.getId());
        physicalResourceData.setResourceName(summary.getName());
        physicalResourceData.setResourceDescription(summary.getDescription());
        return physicalResourceData;
    }
}
