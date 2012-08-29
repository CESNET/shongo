package cz.cesnet.shongo.connector.api;

import cz.cesnet.shongo.Technology;
import org.joda.time.DateTime;

/**
 * A brief info about a virtual room at a server.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class RoomInfo
{
    private String name;
    private String owner;

    private DateTime creation;

    // FIXME: introduces a dependency on the controller module, which the API modules should not be dependent on;
    //        create a class in the common API module for representing a reservation;
    //        this attribute should serve as a reference to the reservation for which the room was created
//    private Reservation reservation;

    private Technology type;

    /**
     * @return Date and time when the room was created.
     */
    public DateTime getCreation()
    {
        return creation;
    }

    /**
     * @param creation Date and time when the room was created.
     */
    public void setCreation(DateTime creation)
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
     * @param name Name of the room.
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
     * @param owner Identification of the room owner.
     */
    public void setOwner(String owner)
    {
        this.owner = owner;
    }

//    /**
//     * @return Reservation for which this room was created
//     */
//    public Reservation getReservation()
//    {
//        return reservation;
//    }
//
//    /**
//     * @param reservation Reservation for which this room was created
//     */
//    public void setReservation(Reservation reservation)
//    {
//        this.reservation = reservation;
//    }

    /**
     * @return Type of the room.
     */
    public Technology getType()
    {
        return type;
    }

    /**
     * @param type Type of the room.
     */
    public void setType(Technology type)
    {
        this.type = type;
    }
}
