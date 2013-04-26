package cz.cesnet.shongo.controller.reservation;

import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.controller.common.RoomConfiguration;
import cz.cesnet.shongo.controller.executor.Endpoint;
import cz.cesnet.shongo.controller.executor.EndpointProvider;
import cz.cesnet.shongo.controller.executor.Executable;
import cz.cesnet.shongo.controller.executor.RoomEndpoint;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.resource.RoomProviderCapability;

import javax.persistence.*;

/**
 * Represents a {@link cz.cesnet.shongo.controller.reservation.Reservation} for a {@link Endpoint}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class RoomReservation extends Reservation implements EndpointProvider
{

    /**
     * {@link RoomProviderCapability} in which the room is allocated.
     */
    private RoomProviderCapability roomProviderCapability;

    /**
     * @see RoomConfiguration
     */
    private RoomConfiguration roomConfiguration = new RoomConfiguration();

    /**
     * Constructor.
     */
    public RoomReservation()
    {
    }

    /**
     * @return {@link #roomProviderCapability}
     */
    @ManyToOne(optional = false)
    @Access(AccessType.FIELD)
    public RoomProviderCapability getRoomProviderCapability()
    {
        return roomProviderCapability;
    }

    /**
     * @param roomProviderCapability sets the {@link #roomProviderCapability}
     */
    public void setRoomProviderCapability(RoomProviderCapability roomProviderCapability)
    {
        this.roomProviderCapability = roomProviderCapability;
    }

    /**
     * @return {@link DeviceResource} of the {@link #roomProviderCapability}
     */
    @Transient
    public DeviceResource getDeviceResource()
    {
        return roomProviderCapability.getDeviceResource();
    }

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
        if (executable != null && !(executable instanceof RoomEndpoint)) {
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
    protected void toApi(cz.cesnet.shongo.controller.api.Reservation api, boolean admin)
    {
        cz.cesnet.shongo.controller.api.RoomReservation roomReservationApi =
                (cz.cesnet.shongo.controller.api.RoomReservation) api;
        DeviceResource deviceResource = getDeviceResource();
        roomReservationApi.setResourceId(EntityIdentifier.formatId(deviceResource));
        roomReservationApi.setResourceName(deviceResource.getName());
        roomReservationApi.setLicenseCount(roomConfiguration.getLicenseCount());
        super.toApi(api, admin);
    }
}
