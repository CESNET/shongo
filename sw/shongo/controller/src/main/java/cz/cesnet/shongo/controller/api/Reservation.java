package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.common.api.TimeSlot;
import cz.cesnet.shongo.common.api.UserIdentity;

/**
 * Represents a single reservation
 *
 * @author Martin srom
 */
public class Reservation
{
    /**
     * Unique identifier
     */
    private String id;

    /**
     * Type of a reservation
     */
    private ReservationType type;

    /**
     * Specifies whether resources from other domain can be allocated
     */
    private boolean interDomain;

    /**
     * Long description
     */
    private String description;

    /**
     * Requested resources
     */
    private Resource[] resources;

    /**
     * Requested time slots
     */
    private TimeSlot[] slots;

    /**
     * Child reservations
     */
    private String[] reservations;

    /**
     * List of permited users
     */
    private UserIdentity[] users;

    public String getId()
    {
        return id;
    }

    public ReservationType getType()
    {
        return type;
    }

    public boolean isInterDomain()
    {
        return interDomain;
    }

    public void setInterDomain(boolean interDomain)
    {
        this.interDomain = interDomain;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public Resource[] getResources()
    {
        return resources;
    }

    public void setResources(Resource[] resources)
    {
        this.resources = resources;
    }

    public TimeSlot[] getSlots()
    {
        return slots;
    }

    public void setSlots(TimeSlot[] slots)
    {
        this.slots = slots;
    }

    public String[] getReservations()
    {
        return reservations;
    }

    public void setReservations(String[] reservations)
    {
        this.reservations = reservations;
    }

    public UserIdentity[] getUsers()
    {
        return users;
    }

    public void setUsers(UserIdentity[] users)
    {
        this.users = users;
    }
}
