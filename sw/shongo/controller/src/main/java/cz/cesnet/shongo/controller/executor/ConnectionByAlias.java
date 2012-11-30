package cz.cesnet.shongo.controller.executor;

import cz.cesnet.shongo.connector.api.ontology.actions.endpoint.Dial;
import cz.cesnet.shongo.connector.api.ontology.actions.multipoint.users.DialParticipant;
import cz.cesnet.shongo.controller.ControllerAgent;
import cz.cesnet.shongo.controller.resource.Alias;
import cz.cesnet.shongo.jade.command.AgentActionCommand;
import cz.cesnet.shongo.jade.command.Command;

import javax.persistence.*;

/**
 * Represents a {@link Connection} by which is establish by a {@link Alias}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class ConnectionByAlias extends Connection
{
    /**
     * {@link Alias} of the {@link Connection#endpointTo}.
     */
    private Alias alias;

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

    @Override
    protected State onStart(ExecutorThread executorThread, EntityManager entityManager)
    {
        if (getEndpointFrom() instanceof ManagedEndpoint) {
            StringBuilder message = new StringBuilder();
            message.append(String.format("Dialing from %s to alias '%s' in technology '%s'.",
                    getEndpointFrom().getReportDescription(), getAlias().getValue(),
                    getAlias().getTechnology().getName()));
            executorThread.getLogger().debug(message.toString());

            ManagedEndpoint managedEndpointFrom = (ManagedEndpoint) getEndpointFrom();
            String agentName = managedEndpointFrom.getConnectorAgentName();
            ControllerAgent controllerAgent = executorThread.getControllerAgent();
            Command command = null;
            if (getEndpointFrom() instanceof RoomEndpoint) {
                RoomEndpoint roomEndpoint = (RoomEndpoint) getEndpointFrom();
                command = controllerAgent.performCommandAndWait(new AgentActionCommand(
                        agentName, new DialParticipant(roomEndpoint.getRoomId(), getAlias().toApi())));
            }
            else {
                command = controllerAgent.performCommandAndWait(new AgentActionCommand(
                        agentName, new Dial(getAlias().toApi())));
            }
            if (command.getState() != Command.State.SUCCESSFUL) {
                return State.STARTING_FAILED;
            }
            setConnectionId((String) command.getResult());
            return super.onStart(executorThread, entityManager);
        }
        else {
            return State.NOT_STARTED;
        }
    }
}
