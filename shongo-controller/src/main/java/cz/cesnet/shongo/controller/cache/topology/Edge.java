package cz.cesnet.shongo.controller.cache.topology;

import cz.cesnet.shongo.PrintableObject;
import cz.cesnet.shongo.Technology;

import java.util.Map;

/**
 * Represents an edge between two nodes in a device topology. Edge means that
 * nodeTo is reachable from nodeFrom in specified technology.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Edge extends PrintableObject
{
    /**
     * Type of edge.
     */
    public enum Type
    {
        IP_ADDRESS,
        ALIAS
    }

    /**
     * Node from which the {@link #nodeTo} is reachable.
     */
    private Node nodeFrom;

    /**
     * Node that is reachable from {@link #nodeFrom}.
     */
    private Node nodeTo;

    /**
     * Technology in which the {@link #nodeTo} is reachable to {@link #nodeFrom}.
     */
    private Technology technology;

    /**
     * Type of edge.
     */
    private Type type;

    /**
     * Constructor.
     *
     * @param nodeFrom   sets the {@link #nodeFrom}
     * @param nodeTo     sets the {@link #nodeTo}
     * @param technology sets the {@link #technology}
     * @param type       sets the {@link #type}
     */
    public Edge(Node nodeFrom, Node nodeTo, Technology technology, Type type)
    {
        this.nodeFrom = nodeFrom;
        this.nodeTo = nodeTo;
        this.technology = technology;
        this.type = type;
    }

    /**
     * @return {@link #nodeFrom}
     */
    public Node getNodeFrom()
    {
        return nodeFrom;
    }

    /**
     * @return {@link #nodeTo}
     */
    public Node getNodeTo()
    {
        return nodeTo;
    }

    /**
     * @return {@link #technology}
     */
    public Technology getTechnology()
    {
        return technology;
    }

    /**
     * @return {@link #type}
     */
    public Type getType()
    {
        return type;
    }

    @Override
    protected void fillDescriptionMap(Map<String, Object> map)
    {
        super.fillDescriptionMap(map);

        map.put("from", getNodeFrom().getDeviceResource().getId());
        map.put("to", getNodeTo().getDeviceResource().getId());
        map.put("technology", getTechnology());
        map.put("type", getType());
    }
}
