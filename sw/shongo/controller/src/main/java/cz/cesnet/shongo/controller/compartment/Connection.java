package cz.cesnet.shongo.controller.compartment;

import cz.cesnet.shongo.PersistentObject;

import javax.persistence.*;

/**
 * Represents a connection (e.g., audio channel, video channel, etc.) between two {@link Endpoint}s
 * in a {@link Compartment}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public abstract class Connection extends PersistentObject
{
    /**
     * The {@link Endpoint} which initiates the {@link Connection}.
     */
    private Endpoint endpointFrom;

    /**
     * The {@link Endpoint} which accepts incoming connection.
     */
    private Endpoint endpointTo;

    /**
     * Current state of the connection.
     */
    private State state;

    /**
     * {@link cz.cesnet.shongo.Technology} specific identifier of the {@link Connection}.
     */
    private String connectionId;

    /**
     * @return {@link #endpointFrom}
     */
    @ManyToOne
    @JoinColumn(name = "endpoint_from_id")
    public Endpoint getEndpointFrom()
    {
        return endpointFrom;
    }

    /**
     * @param endpointFrom sets the {@link #endpointFrom}
     */
    public void setEndpointFrom(Endpoint endpointFrom)
    {
        this.endpointFrom = endpointFrom;
    }

    /**
     * @return {@link #endpointTo}
     */
    @ManyToOne
    @JoinColumn(name = "endpoint_to_id")
    public Endpoint getEndpointTo()
    {
        return endpointTo;
    }

    /**
     * @param endpointTo sets the {@link #endpointTo}
     */
    public void setEndpointTo(Endpoint endpointTo)
    {
        this.endpointTo = endpointTo;
    }

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
     * @return {@link #connectionId}
     */
    @Column
    public String getConnectionId()
    {
        return connectionId;
    }

    /**
     * @param connectionId sets the {@link #connectionId}
     */
    public void setConnectionId(String connectionId)
    {
        this.connectionId = connectionId;
    }

    @PrePersist
    protected void onCreate()
    {
        if (state == null) {
            state = State.NOT_ESTABLISHED;
        }
    }

    /**
     * Establish connection between {@link #endpointFrom} and {@link #endpointTo}.
     *
     * @param compartmentExecutor
     */
    protected abstract void onEstablish(CompartmentExecutor compartmentExecutor);

    /**
     * Close connection between {@link #endpointFrom} and {@link #endpointTo}.
     *
     * @param compartmentExecutor
     */
    protected abstract void onClose(CompartmentExecutor compartmentExecutor);

    /**
     * Establish connection between {@link #endpointFrom} and {@link #endpointTo}.
     *
     * @param compartmentExecutor
     */
    public final void establish(CompartmentExecutor compartmentExecutor)
    {
        if (getState() != State.NOT_ESTABLISHED) {
            throw new IllegalStateException(
                    "Connection can be established only if the connection is not established yet.");
        }

        onEstablish(compartmentExecutor);

        setState(State.ESTABLISHED);
    }

    /**
     * Close connection between {@link #endpointFrom} and {@link #endpointTo}.
     *
     * @param compartmentExecutor
     */
    public final void close(CompartmentExecutor compartmentExecutor)
    {

        if (getState() != State.ESTABLISHED) {
            throw new IllegalStateException(
                    "Connection can be closed only if the connection is already established.");
        }

        onClose(compartmentExecutor);

        setState(State.CLOSED);
    }

    /**
     * State of the {@link Connection}.
     */
    public static enum State
    {
        /**
         * {@link Connection} has not been established yet.
         */
        NOT_ESTABLISHED,

        /**
         * {@link Connection} is already established.
         */
        ESTABLISHED,

        /**
         * {@link Connection} has been already closed.
         */
        CLOSED
    }
}
