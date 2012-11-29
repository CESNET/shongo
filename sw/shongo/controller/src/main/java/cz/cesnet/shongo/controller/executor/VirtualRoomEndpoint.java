package cz.cesnet.shongo.controller.executor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Transient;

/**
 * Represents an {@link Endpoint} which is able to interconnect multiple other {@link Endpoint}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public abstract class VirtualRoomEndpoint extends Endpoint
{
    /**
     * {@link cz.cesnet.shongo.Technology} specific identifier of the {@link VirtualRoomEndpoint}.
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
     * @return port count of the {@link VirtualRoomEndpoint}
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
