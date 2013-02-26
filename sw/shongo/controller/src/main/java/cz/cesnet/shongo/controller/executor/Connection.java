package cz.cesnet.shongo.controller.executor;

import cz.cesnet.shongo.connector.api.jade.endpoint.Dial;
import cz.cesnet.shongo.connector.api.jade.endpoint.HangUpAll;
import cz.cesnet.shongo.connector.api.jade.multipoint.users.DialParticipant;
import cz.cesnet.shongo.connector.api.jade.multipoint.users.DisconnectParticipant;
import cz.cesnet.shongo.controller.ControllerAgent;
import cz.cesnet.shongo.controller.Executor;
import cz.cesnet.shongo.controller.executor.report.CommandFailureReport;
import cz.cesnet.shongo.controller.resource.Alias;
import cz.cesnet.shongo.jade.AgentActionCommand;
import cz.cesnet.shongo.jade.Command;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Represents a connection (e.g., audio channel, video channel, etc.) between two {@link Endpoint}s
 * in a {@link Compartment}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class Connection extends Executable
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
     * {@link cz.cesnet.shongo.controller.resource.Alias} of the {@link Connection#endpointTo}.
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
    @Transient
    public Collection<Executable> getExecutionDependencies()
    {
        List<Executable> dependencies = new ArrayList<Executable>();
        dependencies.add(endpointFrom);
        dependencies.add(endpointTo);
        return dependencies;
    }

    @Override
    protected State onStart(Executor executor)
    {
        if (getEndpointFrom() instanceof ManagedEndpoint) {
            StringBuilder message = new StringBuilder();
            message.append(String.format("Dialing from %s to alias '%s' in technology '%s'.",
                    getEndpointFrom().getReportDescription(), getAlias().getValue(),
                    getAlias().getTechnology().getName()));
            executor.getLogger().debug(message.toString());

            ManagedEndpoint managedEndpointFrom = (ManagedEndpoint) getEndpointFrom();
            String agentName = managedEndpointFrom.getConnectorAgentName();
            ControllerAgent controllerAgent = executor.getControllerAgent();
            Command command = null;
            if (getEndpointFrom() instanceof RoomEndpoint) {
                RoomEndpoint roomEndpoint = (RoomEndpoint) getEndpointFrom();
                command = controllerAgent.performCommand(new AgentActionCommand(
                        agentName, new DialParticipant(roomEndpoint.getRoomId(), getAlias().toApi())));
            }
            else {
                command = controllerAgent.performCommand(new AgentActionCommand(
                        agentName, new Dial(getAlias().toApi())));
            }
            if (command.getState() == Command.State.SUCCESSFUL) {
                setConnectionId((String) command.getResult());
                return State.STARTED;
            }
            else {
                addReport(new CommandFailureReport(command.getFailure()));
                return State.STARTING_FAILED;
            }
        }
        return super.onStart(executor);
    }

    @Override
    protected State onStop(Executor executor)
    {
        StringBuilder message = new StringBuilder();
        message.append(String.format("Hanging up the %s.", getEndpointFrom().getReportDescription()));
        executor.getLogger().debug(message.toString());

        if (getEndpointFrom() instanceof ManagedEndpoint) {
            ManagedEndpoint managedEndpointFrom = (ManagedEndpoint) getEndpointFrom();
            String agentName = managedEndpointFrom.getConnectorAgentName();
            ControllerAgent controllerAgent = executor.getControllerAgent();
            Command command = null;
            if (getEndpointFrom() instanceof RoomEndpoint) {
                RoomEndpoint roomEndpoint = (RoomEndpoint) getEndpointFrom();
                command = controllerAgent.performCommand(new AgentActionCommand(agentName,
                        new DisconnectParticipant(roomEndpoint.getRoomId(), getConnectionId())));
            }
            else {
                // TODO: use connection id to hangup

                command = controllerAgent.performCommand(new AgentActionCommand(agentName, new HangUpAll()));
            }
            if (command.getState() == Command.State.SUCCESSFUL) {
                return State.STOPPED;
            }
            else {
                addReport(new CommandFailureReport(command.getFailure()));
                return State.STOPPING_FAILED;
            }
        }
        return super.onStop(executor);
    }
}
