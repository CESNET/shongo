package cz.cesnet.shongo.controller.executor;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.connector.api.ontology.actions.endpoint.Dial;
import cz.cesnet.shongo.connector.api.ontology.actions.multipoint.users.DialParticipant;
import cz.cesnet.shongo.controller.ControllerAgent;
import cz.cesnet.shongo.controller.resource.Address;
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
public class ConnectionByAddress extends Connection
{
    /**
     * {@link Technology} for the {@link Connection}.
     */
    private Technology technology;

    /**
     * IP address or URL of the {@link Connection#endpointTo}.
     */
    private Address address;

    /**
     * @return {@link #technology}
     */
    @Column
    @Enumerated(EnumType.STRING)
    public Technology getTechnology()
    {
        return technology;
    }

    /**
     * @param technology sets the {@link #technology}
     */
    public void setTechnology(Technology technology)
    {
        this.technology = technology;
    }

    /**
     * @return {@link #address}
     */
    public Address getAddress()
    {
        return address;
    }

    /**
     * @param address sets the {@link #address}
     */
    public void setAddress(Address address)
    {
        this.address = address;
    }

    @Override
    protected State onStart(ExecutorThread executorThread, EntityManager entityManager)
    {
        if (getEndpointFrom() instanceof ManagedEndpoint) {
            StringBuilder message = new StringBuilder();
            message.append(String.format("Dialing from %s to address '%s' in technology '%s'.",
                    getEndpointFrom().getReportDescription(), getAddress().getValue(),
                    getTechnology().getName()));
            executorThread.getLogger().debug(message.toString());

            ManagedEndpoint managedEndpointFrom = (ManagedEndpoint) getEndpointFrom();
            String agentName = managedEndpointFrom.getConnectorAgentName();
            ControllerAgent controllerAgent = executorThread.getControllerAgent();
            Command command = null;
            if (getEndpointFrom() instanceof VirtualRoom) {
                VirtualRoom virtualRoom = (VirtualRoom) getEndpointFrom();
                command = controllerAgent.performCommandAndWait(new AgentActionCommand(
                        agentName, new DialParticipant(virtualRoom.getVirtualRoomId(), getAddress().getValue())));
            }
            else {
                command = controllerAgent.performCommandAndWait(new AgentActionCommand(
                        agentName, new Dial(getAddress().getValue())));
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
