package cz.cesnet.shongo.controller.cache.topology;

import cz.cesnet.shongo.PrintableObject;
import cz.cesnet.shongo.controller.booking.resource.DeviceResource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents a node in a device topology.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Node extends PrintableObject
{
    /**
     * Device resource.
     */
    private DeviceResource deviceResource;

    /**
     * List of incoming edges.
     */
    private List<Edge> incomingEdges = new ArrayList<Edge>();

    /**
     * List of outgoing edges.
     */
    private List<Edge> outgoingEdges = new ArrayList<Edge>();

    /**
     * Constructor.
     *
     * @param deviceResource sets the {@link #deviceResource}
     */
    public Node(DeviceResource deviceResource)
    {
        this.deviceResource = deviceResource;
    }

    /**
     * @return {@link #deviceResource}
     */
    public DeviceResource getDeviceResource()
    {
        return deviceResource;
    }

    /**
     * @return {@link #incomingEdges}
     */
    public List<Edge> getIncomingEdges()
    {
        return incomingEdges;
    }

    /**
     * @return {@link #outgoingEdges}
     */
    public List<Edge> getOutgoingEdges()
    {
        return outgoingEdges;
    }

    /**
     * @param edge edge to be added to the {@link #incomingEdges}
     */
    public void addIncomingEdge(Edge edge)
    {
        incomingEdges.add(edge);
    }

    /**
     * @param edge edge to be removed from the {@link #incomingEdges}
     */
    public void removeIncomingEdge(Edge edge)
    {
        incomingEdges.remove(edge);
    }

    /**
     * @param edge edge to be added to the {@link #outgoingEdges}
     */
    public void addOutgoingEdge(Edge edge)
    {
        outgoingEdges.add(edge);
    }

    /**
     * @param edge edge to be removed from the {@link #outgoingEdges}
     */
    public void removeOutgoingEdge(Edge edge)
    {
        outgoingEdges.remove(edge);
    }

    @Override
    protected void fillDescriptionMap(Map<String, Object> map)
    {
        super.fillDescriptionMap(map);

        map.put("device", getDeviceResource().getId());
    }
}
