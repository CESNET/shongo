package cz.cesnet.shongo.controller.reservation;

import cz.cesnet.shongo.controller.common.RoomConfiguration;
import cz.cesnet.shongo.controller.executor.Endpoint;
import cz.cesnet.shongo.controller.executor.Executable;
import cz.cesnet.shongo.controller.executor.RoomEndpoint;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

/**
 * Represents a {@link cz.cesnet.shongo.controller.reservation.Reservation} for a {@link Endpoint}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class RoomReservation extends EndpointReservation
{
    /**
     * @see RoomConfiguration
     */
    private RoomConfiguration roomConfiguration = new RoomConfiguration();

    /**
     * @return {@link #roomConfiguration}
     */
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    public RoomConfiguration getRoomConfiguration()
    {
        return roomConfiguration;
    }

    /**
     * @param roomConfiguration sets the {@link #roomConfiguration}
     */
    public void setRoomConfiguration(RoomConfiguration roomConfiguration)
    {
        this.roomConfiguration = roomConfiguration;
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
    protected void toApi(cz.cesnet.shongo.controller.api.Reservation api)
    {
        cz.cesnet.shongo.controller.api.RoomReservation roomReservationApi =
                (cz.cesnet.shongo.controller.api.RoomReservation) api;
        roomReservationApi.setLicenseCount(roomConfiguration.getLicenseCount());
        super.toApi(api);
    }
}
