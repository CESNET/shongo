package cz.cesnet.shongo.controller.reservation;

import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.common.Room;
import cz.cesnet.shongo.controller.executor.Endpoint;
import cz.cesnet.shongo.controller.executor.Executable;
import cz.cesnet.shongo.controller.executor.RoomEndpoint;

import javax.persistence.*;

/**
 * Represents a {@link cz.cesnet.shongo.controller.reservation.Reservation} for a {@link Endpoint}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class RoomReservation extends EndpointReservation
{
    /**
     * Allocated port count.
     */
    private Room room = new Room();

    /**
     * @return {@link #room}
     */
    @Embedded
    @Access(AccessType.FIELD)
    public Room getRoom()
    {
        return room;
    }

    /**
     * @param room sets the {@link #room}
     */
    public void setRoom(Room room)
    {
        this.room = room;
    }

    @Override
    public void setExecutable(Executable executable)
    {
        if (!(executable instanceof RoomEndpoint)) {
            throw new IllegalArgumentException("Only room endpoint can be executed by the room reservation.");
        }
        super.setExecutable(executable);
    }

    @Transient
    @Override
    public RoomEndpoint getEndpoint()
    {
        return (RoomEndpoint) getExecutable();
    }

    @Override
    protected cz.cesnet.shongo.controller.api.Reservation createApi()
    {
        return new cz.cesnet.shongo.controller.api.RoomReservation();
    }

    @Override
    protected void toApi(cz.cesnet.shongo.controller.api.Reservation api, Domain domain)
    {
        cz.cesnet.shongo.controller.api.RoomReservation virtualRoomReservationApi =
                (cz.cesnet.shongo.controller.api.RoomReservation) api;
        virtualRoomReservationApi.setLicenseCount(room.getLicenseCount());
        super.toApi(api, domain);
    }
}
