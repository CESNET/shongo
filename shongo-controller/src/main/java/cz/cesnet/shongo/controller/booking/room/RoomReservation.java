package cz.cesnet.shongo.controller.booking.room;

import cz.cesnet.shongo.controller.api.Reservation;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.executable.Endpoint;
import cz.cesnet.shongo.controller.booking.executable.EndpointProvider;
import cz.cesnet.shongo.controller.booking.executable.Executable;
import cz.cesnet.shongo.controller.booking.reservation.TargetedReservation;
import cz.cesnet.shongo.controller.booking.resource.DeviceResource;
import cz.cesnet.shongo.controller.booking.resource.Resource;

import javax.persistence.*;

/**
 * Represents a {@link cz.cesnet.shongo.controller.booking.reservation.Reservation} for a {@link Endpoint}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class RoomReservation extends TargetedReservation implements EndpointProvider
{
    /**
     * {@link RoomProviderCapability} in which the room is allocated.
     */
    private RoomProviderCapability roomProviderCapability;

    /**
     * Number of licenses which are allocated.
     */
    private int licenseCount;

    /**
     * Constructor.
     */
    public RoomReservation()
    {
    }

    /**
     * @return {@link #roomProviderCapability}
     */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
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
     * @return {@link #licenseCount}
     */
    @Column(nullable = false)
    public int getLicenseCount()
    {
        return licenseCount;
    }

    /**
     * @param licenseCount sets the {@link #licenseCount}
     */
    public void setLicenseCount(int licenseCount)
    {
        this.licenseCount = licenseCount;
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
    protected void toApi(Reservation api, EntityManager entityManager, boolean admin)
    {
        cz.cesnet.shongo.controller.api.RoomReservation roomReservationApi =
                (cz.cesnet.shongo.controller.api.RoomReservation) api;
        DeviceResource deviceResource = getAllocatedResource();
        roomReservationApi.setResourceId(ObjectIdentifier.formatId(deviceResource));
        roomReservationApi.setResourceName(deviceResource.getName());
        roomReservationApi.setLicenseCount(getLicenseCount());
        super.toApi(api, entityManager, admin);
    }

    @Override
    @Transient
    public Long getTargetId()
    {
        return roomProviderCapability.getId();
    }

    @Override
    @Transient
    public DeviceResource getAllocatedResource()
    {
        return roomProviderCapability.getDeviceResource();
    }
}
