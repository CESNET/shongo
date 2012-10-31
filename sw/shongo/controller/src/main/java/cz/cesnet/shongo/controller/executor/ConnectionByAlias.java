package cz.cesnet.shongo.controller.executor;

import cz.cesnet.shongo.controller.ControllerAgent;
import cz.cesnet.shongo.controller.resource.Alias;
import cz.cesnet.shongo.jade.command.ActionRequestCommand;
import cz.cesnet.shongo.jade.command.Command;
import cz.cesnet.shongo.jade.ontology.actions.endpoint.Dial;
import cz.cesnet.shongo.jade.ontology.actions.multipoint.users.DialParticipant;

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
    protected State onEstablish(ExecutorThread executorThread)
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
            if (getEndpointFrom() instanceof VirtualRoom) {
                VirtualRoom virtualRoom = (VirtualRoom) getEndpointFrom();
                command = controllerAgent.performCommandAndWait(new ActionRequestCommand(
                        agentName, new DialParticipant(virtualRoom.getVirtualRoomId(), getAlias().toApi())));
            }
            else {
                command = controllerAgent.performCommandAndWait(new ActionRequestCommand(
                        agentName, new Dial(getAlias().toApi())));
            }
            if (command.getState() != Command.State.SUCCESSFUL) {
                return State.FAILED;
            }
            setConnectionId((String) command.getResult());
            return State.ESTABLISHED;
        }
        return State.NOT_ESTABLISHED;
    }
}
