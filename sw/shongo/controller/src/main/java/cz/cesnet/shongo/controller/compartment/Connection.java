package cz.cesnet.shongo.controller.compartment;

import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.controller.ControllerAgent;
import cz.cesnet.shongo.jade.command.Command;
import cz.cesnet.shongo.jade.command.SendCommand;
import cz.cesnet.shongo.jade.ontology.DisconnectParticipant;
import cz.cesnet.shongo.jade.ontology.HangUpAll;

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
    protected abstract boolean onEstablish(CompartmentExecutor compartmentExecutor);

    /**
     * Close connection between {@link #endpointFrom} and {@link #endpointTo}.
     *
     * @param compartmentExecutor
     */
    protected boolean onClose(CompartmentExecutor compartmentExecutor)
    {
        StringBuilder message = new StringBuilder();
        message.append(String.format("Hanging up the %s.", getEndpointFrom().getReportDescription()));
        compartmentExecutor.getLogger().debug(message.toString());

        if (getEndpointFrom() instanceof ManagedEndpoint) {
            ManagedEndpoint managedEndpointFrom = (ManagedEndpoint) getEndpointFrom();
            String agentName = managedEndpointFrom.getConnectorAgentName();
            ControllerAgent controllerAgent = compartmentExecutor.getControllerAgent();
            Command command = null;
            if (getEndpointFrom() instanceof VirtualRoom) {
                VirtualRoom virtualRoom = (VirtualRoom) getEndpointFrom();
                command = controllerAgent.performCommand(SendCommand.createSendCommand(agentName,
                        new DisconnectParticipant(virtualRoom.getVirtualRoomId(), getConnectionId())));
            } else {
                // TODO: use connection id to hangup

                command = controllerAgent.performCommand(SendCommand.createSendCommand(agentName, new HangUpAll()));
            }
            if (command.getState() != Command.State.SUCCESSFUL) {
                return false;
            }
        }
        return true;
    }

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

        if (onEstablish(compartmentExecutor)) {
            setState(State.ESTABLISHED);
        }
        else {
            setState(State.FAILED);
        }
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
         * {@link Connection} failed to establish.
         */
        FAILED,

        /**
         * {@link Connection} has been already closed.
         */
        CLOSED;

        /**
         * @return converted to {@link cz.cesnet.shongo.controller.api.CompartmentReservation.Connection.State}
         */
        public cz.cesnet.shongo.controller.api.CompartmentReservation.Connection.State toApi()
        {
            switch (this) {
                case NOT_ESTABLISHED:
                    return cz.cesnet.shongo.controller.api.CompartmentReservation.Connection.State.NOT_ESTABLISHED;
                case ESTABLISHED:
                    return cz.cesnet.shongo.controller.api.CompartmentReservation.Connection.State.ESTABLISHED;
                case FAILED:
                    return cz.cesnet.shongo.controller.api.CompartmentReservation.Connection.State.FAILED;
                case CLOSED:
                    return cz.cesnet.shongo.controller.api.CompartmentReservation.Connection.State.CLOSED;
                default:
                    throw new IllegalStateException("Cannot convert " + this.toString() + " to API.");
            }
        }
    }
}
