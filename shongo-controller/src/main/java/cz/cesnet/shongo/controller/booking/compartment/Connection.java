package cz.cesnet.shongo.controller.booking.compartment;

import cz.cesnet.shongo.api.AbstractComplexType;
import cz.cesnet.shongo.connector.api.jade.endpoint.Dial;
import cz.cesnet.shongo.connector.api.jade.endpoint.HangUpAll;
import cz.cesnet.shongo.connector.api.jade.multipoint.DialRoomParticipant;
import cz.cesnet.shongo.connector.api.jade.multipoint.DisconnectRoomParticipant;
import cz.cesnet.shongo.controller.ControllerAgent;
import cz.cesnet.shongo.controller.executor.Executor;
import cz.cesnet.shongo.controller.booking.alias.Alias;
import cz.cesnet.shongo.controller.booking.room.RoomEndpoint;
import cz.cesnet.shongo.controller.booking.executable.Endpoint;
import cz.cesnet.shongo.controller.booking.executable.Executable;
import cz.cesnet.shongo.controller.booking.executable.ExecutableManager;
import cz.cesnet.shongo.controller.booking.executable.ManagedEndpoint;
import cz.cesnet.shongo.jade.SendLocalCommand;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Represents a connection (e.g., audio channel, video channel, etc.) between two {@link cz.cesnet.shongo.controller.booking.executable.Endpoint}s
 * in a {@link Compartment}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class Connection extends Executable
{
    /**
     * The {@link cz.cesnet.shongo.controller.booking.executable.Endpoint} which initiates the {@link Connection}.
     */
    private Endpoint endpointFrom;

    /**
     * The {@link Endpoint} which accepts incoming connection.
     */
    private Endpoint endpointTo;

    /**
     * {@link cz.cesnet.shongo.controller.booking.alias.Alias} of the {@link Connection#endpointTo}.
     */
    private Alias alias;

    /**
     * {@link cz.cesnet.shongo.Technology} specific id of the {@link Connection}.
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
     * @return {@link #alias}
     */
    @OneToOne(cascade = CascadeType.ALL)
    @Access(AccessType.FIELD)
    public Alias getAlias()
    {
        return alias;
    }

    /**
     * @param alias sets the {@link #alias}
     */
    public void setAlias(Alias alias)
    {
        this.alias = alias;
    }

    /**
     * @return {@link #connectionId}
     */
    @Column(length = AbstractComplexType.DEFAULT_COLUMN_LENGTH)
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
    @Transient
    public Collection<Executable> getExecutionDependencies()
    {
        List<Executable> dependencies = new ArrayList<Executable>();
        dependencies.add(endpointFrom);
        dependencies.add(endpointTo);
        return dependencies;
    }

    @Override
    protected State onStart(Executor executor, ExecutableManager executableManager)
    {
        if (getEndpointFrom() instanceof ManagedEndpoint) {
            StringBuilder message = new StringBuilder();
            message.append(String.format("Dialing from %s to alias '%s' in technology '%s'.",
                    getEndpointFrom().getDescription(), getAlias().getValue(),
                    getAlias().getTechnology().getName()));
            executor.getLogger().debug(message.toString());

            ManagedEndpoint managedEndpointFrom = (ManagedEndpoint) getEndpointFrom();
            String agentName = managedEndpointFrom.getConnectorAgentName();
            ControllerAgent controllerAgent = executor.getControllerAgent();
            SendLocalCommand sendLocalCommand;
            if (getEndpointFrom() instanceof RoomEndpoint) {
                RoomEndpoint roomEndpoint = (RoomEndpoint) getEndpointFrom();
                sendLocalCommand = controllerAgent.sendCommand(agentName,
                        new DialRoomParticipant(roomEndpoint.getRoomId(), getAlias().toApi()));
            }
            else {
                sendLocalCommand = controllerAgent.sendCommand(agentName, new Dial(getAlias().toApi()));
            }
            if (sendLocalCommand.isSuccessful()) {
                setConnectionId((String) sendLocalCommand.getResult());
                return State.STARTED;
            }
            else {
                executableManager.createExecutionReport(this, sendLocalCommand);
                return State.STARTING_FAILED;
            }
        }
        return super.onStart(executor, executableManager);
    }

    @Override
    protected State onStop(Executor executor, ExecutableManager executableManager)
    {
        StringBuilder message = new StringBuilder();
        message.append(String.format("Hanging up the %s.", getEndpointFrom().getDescription()));
        executor.getLogger().debug(message.toString());

        if (getEndpointFrom() instanceof ManagedEndpoint) {
            ManagedEndpoint managedEndpointFrom = (ManagedEndpoint) getEndpointFrom();
            String agentName = managedEndpointFrom.getConnectorAgentName();
            ControllerAgent controllerAgent = executor.getControllerAgent();
            SendLocalCommand sendLocalCommand;
            if (getEndpointFrom() instanceof RoomEndpoint) {
                RoomEndpoint roomEndpoint = (RoomEndpoint) getEndpointFrom();
                sendLocalCommand = controllerAgent.sendCommand(agentName,
                        new DisconnectRoomParticipant(roomEndpoint.getRoomId(), getConnectionId()));
            }
            else {
                // TODO: use connection id to hangup
                sendLocalCommand = controllerAgent.sendCommand(agentName, new HangUpAll());
            }
            if (sendLocalCommand.isSuccessful()) {
                return State.STOPPED;
            }
            else {
                executableManager.createExecutionReport(this, sendLocalCommand);
                return State.STOPPING_FAILED;
            }
        }
        return super.onStop(executor, executableManager);
    }
}
