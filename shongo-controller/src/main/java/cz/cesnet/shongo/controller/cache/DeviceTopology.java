package cz.cesnet.shongo.controller.cache;

import cz.cesnet.shongo.PrintableObject;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.cache.topology.Edge;
import cz.cesnet.shongo.controller.cache.topology.Node;
import cz.cesnet.shongo.controller.booking.resource.DeviceResource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents a topology of device resources and their reachability.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class DeviceTopology extends PrintableObject
{
    /**
     * List of {@link cz.cesnet.shongo.controller.cache.topology.Node}s in device topology.
     */
    private List<Node> nodes = new ArrayList<Node>();

    /**
     * List of {@link cz.cesnet.shongo.controller.cache.topology.Edge}s in device topology.
     */
    private List<Edge> edges = new ArrayList<Edge>();

    /**
     * @param deviceResource device to be added to the device topology
     */
    public void addDeviceResource(DeviceResource deviceResource)
    {
        Node newNode = new Node(deviceResource);
        for (Node node : nodes) {
            createEdgesBetweenNodes(newNode, node);
            createEdgesBetweenNodes(node, newNode);
        }
        nodes.add(newNode);
    }

    /**
     * @param deviceResource device to be removed from the device topology
     */
    public void removeDeviceResource(DeviceResource deviceResource)
    {
        Node node = null;
        for (Node possibleNode : nodes) {
            if (possibleNode.getDeviceResource().getId().equals(deviceResource.getId())) {
                node = possibleNode;
                break;
            }
        }
        if (node == null) {
            throw new IllegalArgumentException("Device resource '"
                    + deviceResource.getId() + "' is not in device topology.");
        }
        List<Edge> listIncomingEdges = node.getIncomingEdges();
        while (listIncomingEdges.size() > 0) {
            removeEdge(listIncomingEdges.get(0));
        }
        List<Edge> listOutgoingEdges = node.getOutgoingEdges();
        while (listOutgoingEdges.size() > 0) {
            removeEdge(listOutgoingEdges.get(0));
        }
        nodes.remove(node);
    }

    /**
     * Create edges between nodes
     *
     * @param nodeFrom
     * @param nodeTo
     */
    private void createEdgesBetweenNodes(Node nodeFrom, Node nodeTo)
    {
        DeviceResource deviceFrom = nodeFrom.getDeviceResource();
        DeviceResource deviceTo = nodeTo.getDeviceResource();
        Set<Technology> technologiesFrom = deviceFrom.getTechnologies();
        Set<Technology> technologiesTo = deviceFrom.getTechnologies();
        for (Technology technology : technologiesFrom) {
            if (technologiesTo.contains(technology)) {
                if (deviceFrom.hasIpAddress() && deviceTo.hasIpAddress()) {
                    addEdge(nodeFrom, nodeTo, technology, Edge.Type.IP_ADDRESS);
                }
                addEdge(nodeFrom, nodeTo, technology, Edge.Type.ALIAS);
            }
        }
    }

    /**
     * Add a new edge to the device topology
     *
     * @param nodeFrom
     * @param nodeTo
     * @param technology
     * @param type
     */
    private void addEdge(Node nodeFrom, Node nodeTo, Technology technology, Edge.Type type)
    {
        Edge edge = new Edge(nodeFrom, nodeTo, technology, type);
        nodeFrom.addOutgoingEdge(edge);
        nodeTo.addIncomingEdge(edge);
        edges.add(edge);
    }

    /**
     * Remove edge from the device topology
     *
     * @param edge
     */
    private void removeEdge(Edge edge)
    {
        Node nodeFrom = edge.getNodeFrom();
        Node nodeTo = edge.getNodeTo();
        nodeFrom.removeOutgoingEdge(edge);
        nodeTo.removeIncomingEdge(edge);
        edges.remove(edge);
    }

    /**
     * Remove all device resources.
     */
    public void clear()
    {
        nodes.clear();
        edges.clear();
    }

    @Override
    protected void fillDescriptionMap(Map<String, Object> map)
    {
        super.fillDescriptionMap(map);

        map.put("nodes", nodes);
        map.put("edges", edges);
    }
}
