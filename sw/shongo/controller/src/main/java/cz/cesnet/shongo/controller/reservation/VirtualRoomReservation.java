package cz.cesnet.shongo.controller.reservation;

import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.executor.Compartment;
import cz.cesnet.shongo.controller.executor.ResourceVirtualRoom;
import cz.cesnet.shongo.controller.executor.VirtualRoom;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

/**
 * Represents a {@link cz.cesnet.shongo.controller.reservation.Reservation} for a {@link cz.cesnet.shongo.controller.executor.Endpoint}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class VirtualRoomReservation extends EndpointReservation
{
    /**
     * Allocated port count.
     */
    private Integer portCount;

    /**
     * {@link VirtualRoom} which is allocated by the {@link VirtualRoomReservation}.
     */
    private ResourceVirtualRoom virtualRoom;

    /**
     * @return {@link #portCount}
     */
    public Integer getPortCount()
    {
        return portCount;
    }

    /**
     * @param portCount {@link #portCount}
     */
    public void setPortCount(Integer portCount)
    {
        this.portCount = portCount;
    }

    /**
     * @return {@link #virtualRoom}
     */
    @OneToOne(cascade = CascadeType.PERSIST, optional = false)
    public VirtualRoom getVirtualRoom()
    {
        return virtualRoom;
    }

    /**
     * @param virtualRoom sets the {@link #virtualRoom}
     */
    public void setVirtualRoom(ResourceVirtualRoom virtualRoom)
    {
        this.virtualRoom = virtualRoom;
    }

    /**
     * @return allocated {@link cz.cesnet.shongo.controller.executor.Endpoint} by the {@link cz.cesnet.shongo.controller.reservation.VirtualRoomReservation}
     */
    @Transient
    @Override
    public ResourceVirtualRoom getEndpoint()
    {
        return virtualRoom;
    }

    @Override
    protected cz.cesnet.shongo.controller.api.Reservation createApi()
    {
        return new cz.cesnet.shongo.controller.api.VirtualRoomReservation();
    }

    @Override
    protected void toApi(cz.cesnet.shongo.controller.api.Reservation api, Domain domain)
    {
        cz.cesnet.shongo.controller.api.VirtualRoomReservation virtualRoomReservationApi =
                (cz.cesnet.shongo.controller.api.VirtualRoomReservation) api;
        virtualRoomReservationApi.setPortCount(getPortCount());
        super.toApi(api, domain);
    }
}
