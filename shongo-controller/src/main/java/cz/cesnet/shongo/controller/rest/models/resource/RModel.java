package cz.cesnet.shongo.controller.rest.models.resource;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RModel {

    private ResourceCapacity resourceCapacity;
    private ResourceCapacityUtilization resourceCapacityUtilization;
}
