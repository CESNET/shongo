package cz.cesnet.shongo.controller.executor;

import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.controller.ControllerAgent;
import cz.cesnet.shongo.jade.command.ActionRequestCommand;
import cz.cesnet.shongo.jade.command.Command;
import cz.cesnet.shongo.jade.ontology.actions.endpoint.HangUpAll;
import cz.cesnet.shongo.jade.ontology.actions.multipoint.users.DisconnectParticipant;

import javax.persistence.*;

/**
 * Represents a connection (e.g., audio channel, video channel, etc.) between two {@link Endpoint}s
 * in a {@link Compartment}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public abstract class Connection extends Executable
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

    @Override
    protected State onStop(ExecutorThread executorThread, EntityManager entityManager)
    {
        StringBuilder message = new StringBuilder();
        message.append(String.format("Hanging up the %s.", getEndpointFrom().getReportDescription()));
        executorThread.getLogger().debug(message.toString());

        if (getEndpointFrom() instanceof ManagedEndpoint) {
            ManagedEndpoint managedEndpointFrom = (ManagedEndpoint) getEndpointFrom();
            String agentName = managedEndpointFrom.getConnectorAgentName();
            ControllerAgent controllerAgent = executorThread.getControllerAgent();
            Command command = null;
            if (getEndpointFrom() instanceof VirtualRoom) {
                VirtualRoom virtualRoom = (VirtualRoom) getEndpointFrom();
                command = controllerAgent.performCommand(new ActionRequestCommand(agentName,
                        new DisconnectParticipant(virtualRoom.getVirtualRoomId(), getConnectionId())));
            }
            else {
                // TODO: use connection id to hangup

                command = controllerAgent.performCommand(new ActionRequestCommand(agentName, new HangUpAll()));
            }
            if (command.getState() != Command.State.SUCCESSFUL) {
                return State.STARTED;
            }
        }
        return super.onStop(executorThread, entityManager);
    }
}
