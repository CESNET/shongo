package cz.cesnet.shongo.connector.api;

import cz.cesnet.shongo.api.Technology;
import cz.cesnet.shongo.controller.allocation.Reservation;
import cz.cesnet.shongo.controller.common.AbsoluteDateTimeSpecification;

/**
 * A brief info about a virtual room at a server.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class RoomInfo
{
    private String name;
    private String owner;
    private AbsoluteDateTimeSpecification creation;
    private Reservation reservation;
    private Technology type;

    /**
     * @return Date and time when the room was created.
     */
    public AbsoluteDateTimeSpecification getCreation()
    {
        return creation;
    }

    /**
     * @param creation    Date and time when the room was created.
     */
    public void setCreation(AbsoluteDateTimeSpecification creation)
    {
        this.creation = creation;
    }

    /**
     * @return Name of the room.
     */
    public String getName()
    {
        return name;
    }

    /**
     * @param name    Name of the room.
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * @return Identification of the room owner.
     */
    public String getOwner()
    {
        return owner;
    }

    /**
     * @param owner    Identification of the room owner.
     */
    public void setOwner(String owner)
    {
        this.owner = owner;
    }

    /**
     * @return Reservation for which this room was created
     */
    public Reservation getReservation()
    {
        return reservation;
    }

    /**
     * @param reservation    Reservation for which this room was created
     */
    public void setReservation(Reservation reservation)
    {
        this.reservation = reservation;
    }

    /**
     * @return Type of the room.
     */
    public Technology getType()
    {
        return type;
    }

    /**
     * @param type    Type of the room.
     */
    public void setType(Technology type)
    {
        this.type = type;
    }
}
