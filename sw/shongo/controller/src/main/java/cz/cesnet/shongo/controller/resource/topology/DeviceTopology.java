package cz.cesnet.shongo.controller.resource.topology;

import cz.cesnet.shongo.controller.resource.DeviceResource;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a topology of device resources and their reachability.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class DeviceTopology
{
    /**
     * List of {@link Node}s in device topology.
     */
    private List<Node> nodes = new ArrayList<Node>();

    /**
     * List of {@link Edge}s in device topology.
     */
    private List<Edge> edges = new ArrayList<Edge>();

    /**
     * @param deviceResource device to be added to the device topology
     */
    public void addDeviceResource(DeviceResource deviceResource)
    {
        throw new RuntimeException("TODO: Implement DeviceTopology.addDeviceResource");
    }

    /**
     * @param deviceResource device to be updated in the device topology
     */
    public void updateDeviceResource(DeviceResource deviceResource)
    {
        throw new RuntimeException("TODO: Implement DeviceTopology.updateDeviceResource");
    }

    /**
     * @param deviceResource device to be removed from the device topology
     */

    public void removeDeviceResource(DeviceResource deviceResource)
    {
        throw new RuntimeException("TODO: Implement DeviceTopology.removeDeviceResource");
    }
}
