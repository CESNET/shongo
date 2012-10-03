package cz.cesnet.shongo.controller.compartment;

import cz.cesnet.shongo.PersistentObject;
import org.hibernate.annotations.Type;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a conference (e.g., video conference, audio conference, etc.).
 * <p/>
 * In each {@link Compartment} participates multiple {@link Endpoint}s which are interconnected by {@link Connection}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class Compartment extends PersistentObject
{
    /**
     * Interval start date/time.
     */
    private DateTime slotStart;

    /**
     * Interval end date/time.
     */
    private DateTime slotEnd;

    /**
     * List of {@link Endpoint}s which participates in the {@link Compartment}.
     */
    private List<Endpoint> endpoints = new ArrayList<Endpoint>();

    /**
     * List of {@link VirtualRoom}s which participates in the {@link Compartment}.
     */
    private List<VirtualRoom> virtualRooms = new ArrayList<VirtualRoom>();

    /**
     * List of {@link Connection}s which are established in the {@link Compartment}.
     */
    private List<Connection> connections = new ArrayList<Connection>();

    /**
     * Total sum of endpoints (calculated as sum of {@link Endpoint#getCount()} because one
     * {@link Endpoint} can represent multiple endpoints).
     */
    private int totalEndpointCount = 0;

    /**
     * Current state of the {@link Compartment}.
     */
    private State state;

    /**
     * @return {@link #slotStart}
     */
    @Column
    @Type(type = "DateTime")
    @Access(AccessType.PROPERTY)
    public DateTime getSlotStart()
    {
        return slotStart;
    }

    /**
     * @param slotStart sets the {@link #slotStart}
     */
    public void setSlotStart(DateTime slotStart)
    {
        this.slotStart = slotStart;
    }

    /**
     * @return {@link #slotEnd}
     */
    @Column
    @Type(type = "DateTime")
    @Access(AccessType.PROPERTY)
    public DateTime getSlotEnd()
    {
        return slotEnd;
    }

    /**
     * @param slotEnd sets the {@link #slotEnd}
     */
    public void setSlotEnd(DateTime slotEnd)
    {
        this.slotEnd = slotEnd;
    }

    /**
     * @return slot ({@link #slotStart}, {@link #slotEnd})
     */
    @Transient
    public Interval getSlot()
    {
        return new Interval(slotStart, slotEnd);
    }

    /**
     * @param slot sets the slot
     */
    public void setSlot(Interval slot)
    {
        setSlotStart(slot.getStart());
        setSlotEnd(slot.getEnd());
    }

    /**
     * Sets the slot to new interval created from given {@code start} and {@code end}.
     *
     * @param start
     * @param end
     */
    public void setSlot(DateTime start, DateTime end)
    {
        setSlotStart(start);
        setSlotEnd(end);
    }

    /**
     * @return {@link #endpoints}
     */
    @OneToMany(cascade = CascadeType.ALL)
    @Access(AccessType.FIELD)
    public List<Endpoint> getEndpoints()
    {
        return endpoints;
    }

    /**
     * @param endpoint to be added to the {@link #endpoints}
     */
    public void addEndpoint(Endpoint endpoint)
    {
        endpoints.add(endpoint);

        // Update total endpoint count
        totalEndpointCount += endpoint.getCount();
    }

    /**
     * @param endpoint to be removed from the {@link #endpoints}
     */
    public void removeEndpoint(Endpoint endpoint)
    {
        // Update total endpoint count
        totalEndpointCount -= endpoint.getCount();

        endpoints.remove(endpoint);
    }

    /**
     * @return {@link #virtualRooms}
     */
    @OneToMany(cascade = CascadeType.ALL)
    @Access(AccessType.FIELD)
    public List<VirtualRoom> getVirtualRooms()
    {
        return virtualRooms;
    }

    /**
     * @param virtualRoom to be added to the {@link #virtualRooms}
     */
    public void addVirtualRoom(VirtualRoom virtualRoom)
    {
        virtualRooms.add(virtualRoom);
    }

    /**
     * @param virtualRoom to be removed from the {@link #virtualRooms}
     */
    public void removeVirtualRoom(VirtualRoom virtualRoom)
    {
        virtualRooms.remove(virtualRoom);
    }

    /**
     * @return {@link #connections}
     */
    @OneToMany(cascade = CascadeType.ALL)
    @Access(AccessType.FIELD)
    public List<Connection> getConnections()
    {
        return connections;
    }

    /**
     * @param connection to be added to the {@link #connections}
     */
    public void addConnection(Connection connection)
    {
        connections.add(connection);
    }

    /**
     * @param connection to be removed from the {@link #connections}
     */
    public void removeConnection(Connection connection)
    {
        connections.remove(connection);
    }

    /**
     * @return {@link #totalEndpointCount}
     */
    @Column
    @Access(AccessType.FIELD)
    public int getTotalEndpointCount()
    {
        return totalEndpointCount;
    }

    /**
     * @return {@link #state}
     */
    @Column
    @Enumerated(EnumType.STRING)
    public State getState()
    {
        return state;
    }

    /**
     * @param state sets the {@link #state}
     */
    public void setState(State state)
    {
        this.state = state;
    }

    @PrePersist
    protected void onCreate()
    {
        if (state == null) {
            state = State.NOT_STARTED;
        }
    }

    /**
     * State of the {@link Compartment}.
     */
    public static enum State
    {
        /**
         * {@link Compartment} has not been created yet.
         */
        NOT_STARTED,

        /**
         * {@link Compartment} is already created.
         */
        STARTED,

        /**
         * {@link Compartment} has been already deleted.
         */
        FINISHED;

        /**
         * @return converted to {@link cz.cesnet.shongo.controller.api.CompartmentReservation.Compartment.State}
         */
        public cz.cesnet.shongo.controller.api.CompartmentReservation.Compartment.State toApi()
        {
            switch (this) {
                case NOT_STARTED:
                    return cz.cesnet.shongo.controller.api.CompartmentReservation.Compartment.State.NOT_STARTED;
                case STARTED:
                    return cz.cesnet.shongo.controller.api.CompartmentReservation.Compartment.State.STARTED;
                case FINISHED:
                    return cz.cesnet.shongo.controller.api.CompartmentReservation.Compartment.State.FINISHED;
                default:
                    throw new IllegalStateException("Cannot convert " + this.toString() + " to API.");
            }
        }
    }
}
