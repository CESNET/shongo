package cz.cesnet.shongo.controller.compartment;

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
     * Current state of the {@link VirtualRoom}.
     */
    private State state;

    /**
     * {@link cz.cesnet.shongo.Technology} specific identifier of the {@link VirtualRoom}.
     */
    private String virtualRoomId;

    /**
     * @return port count of the {@link VirtualRoom}
     */
    @Transient
    public abstract Integer getPortCount();

    /**
     * @return {@link #state}
     */
    @Column
    @Enumerated(EnumType.STRING)
    public State getState()
    {
        return state;
    }

    /**
     * @param state sets the {@link #state}
     */
    public void setState(State state)
    {
        this.state = state;
    }

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

    @PrePersist
    protected void onCreate()
    {
        if (state == null) {
            state = State.NOT_CREATED;
        }
    }

    /**
     * Start virtual room.
     *
     * @param compartmentExecutor
     */
    protected abstract void onCreate(CompartmentExecutor compartmentExecutor);

    /**
     * Stop virtual room.
     *
     * @param compartmentExecutor
     */
    protected abstract void onDelete(CompartmentExecutor compartmentExecutor);

    /**
     * Start virtual room.
     *
     * @param compartmentExecutor
     */
    public final void create(CompartmentExecutor compartmentExecutor)
    {
        if (getState() != State.NOT_CREATED) {
            throw new IllegalStateException(
                    "Virtual room can be created only if the virtual room is not created yet.");
        }

        onCreate(compartmentExecutor);

        setState(State.CREATED);
    }

    /**
     * Stop virtual room.
     *
     * @param compartmentExecutor
     */
    public final void delete(CompartmentExecutor compartmentExecutor)
    {
        if (getState() != State.CREATED) {
            throw new IllegalStateException(
                    "Virtual room can be deleted only if the virtual room is already created.");
        }

        onDelete(compartmentExecutor);

        setState(State.DELETED);
    }

    /**
     * State of the {@link VirtualRoom}.
     */
    public static enum State
    {
        /**
         * {@link VirtualRoom} has not been created yet.
         */
        NOT_CREATED,

        /**
         * {@link VirtualRoom} is already created.
         */
        CREATED,

        /**
         * {@link VirtualRoom} has been already deleted.
         */
        DELETED
    }
}
