package cz.cesnet.shongo.controller.executor;

import cz.cesnet.shongo.controller.api.Compartment;

import javax.persistence.*;

/**
 * Represents an {@link Endpoint} which is able to interconnect multiple other {@link Endpoint}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public abstract class VirtualRoom extends Endpoint
{
    /**
     * {@link cz.cesnet.shongo.Technology} specific identifier of the {@link VirtualRoom}.
     */
    private String virtualRoomId;

    @Override
    @Transient
    public String getName()
    {
        return String.format("virtual room '%d'", getId());
    }

    @Override
    @Transient
    public int getCount()
    {
        return 0;
    }

    /**
     * @return port count of the {@link VirtualRoom}
     */
    @Transient
    public abstract Integer getPortCount();

    /**
     * @return {@link #virtualRoomId}
     */
    @Column
    public String getVirtualRoomId()
    {
        return virtualRoomId;
    }

    /**
     * @param virtualRoomId sets the {@link #virtualRoomId}
     */
    public void setVirtualRoomId(String virtualRoomId)
    {
        this.virtualRoomId = virtualRoomId;
    }
}
