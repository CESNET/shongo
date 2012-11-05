package cz.cesnet.shongo.controller.reservation;

import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.executor.Endpoint;
import cz.cesnet.shongo.controller.executor.Executable;
import cz.cesnet.shongo.controller.executor.VirtualRoom;

import javax.persistence.Entity;
import javax.persistence.Transient;

/**
 * Represents a {@link cz.cesnet.shongo.controller.reservation.Reservation} for a {@link Endpoint}.
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

    @Override
    public void setExecutable(Executable executable)
    {
        if (!(executable instanceof VirtualRoom)) {
            throw new IllegalArgumentException("Only virtual room can be executed by the virtual room reservation.");
        }
        super.setExecutable(executable);
    }

    @Transient
    @Override
    public VirtualRoom getEndpoint()
    {
        return (VirtualRoom) getExecutable();
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
