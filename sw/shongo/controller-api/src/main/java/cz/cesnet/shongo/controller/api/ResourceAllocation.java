package cz.cesnet.shongo.controller.api;

import org.joda.time.Interval;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an information about reservations of a resource.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ResourceAllocation
{
    /**
     * Identifier of the resource.
     */
    private String identifier;

    /**
     * Name of the resource.
     */
    private String name;

    /**
     * Interval for the allocation information.
     */
    private Interval interval;

    /**
     * {@link ResourceReservation} of the resource.
     */
    private List<ResourceReservation> reservations = new ArrayList<ResourceReservation>();

    /**
     * @return {@link #identifier}
     */
    public String getIdentifier()
    {
        return identifier;
    }

    /**
     * @param identifier {@link #identifier}
     */
    public void setIdentifier(String identifier)
    {
        this.identifier = identifier;
    }

    /**
     * @return {@link #name}
     */
    public String getName()
    {
        return name;
    }

    /**
     * @param name sets the {@link #name}
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * @return {@link #interval}
     */
    public Interval getInterval()
    {
        return interval;
    }

    /**
     * @param interval sets the {@link #interval}
     */
    public void setInterval(Interval interval)
    {
        this.interval = interval;
    }

    /**
     * @return {@link #reservations}
     */
    public List<ResourceReservation> getReservations()
    {
        return reservations;
    }

    /**
     * @param reservations sets the {@link #reservations}
     */
    public void setReservations(List<ResourceReservation> reservations)
    {
        this.reservations = reservations;
    }

    /**
     * @param reservation to be added to the {@link #reservations}
     */
    public void addReservation(ResourceReservation reservation)
    {
        reservations.add(reservation);
    }
}
