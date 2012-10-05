package cz.cesnet.shongo.controller.compartment;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.ControllerAgent;
import cz.cesnet.shongo.controller.resource.Address;
import cz.cesnet.shongo.controller.resource.Alias;
import cz.cesnet.shongo.jade.command.ActionRequestCommand;
import cz.cesnet.shongo.jade.command.Command;
import cz.cesnet.shongo.jade.ontology.Dial;
import cz.cesnet.shongo.jade.ontology.DialParticipant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

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
    protected boolean onEstablish(CompartmentExecutor compartmentExecutor)
    {
        StringBuilder message = new StringBuilder();
        message.append(String.format("Dialing from %s to address '%s' in technology '%s'.",
                getEndpointFrom().getReportDescription(), getAddress(),
                getTechnology().getName()));
        compartmentExecutor.getLogger().debug(message.toString());

        if (getEndpointFrom() instanceof ManagedEndpoint) {
            ManagedEndpoint managedEndpointFrom = (ManagedEndpoint) getEndpointFrom();
            String agentName = managedEndpointFrom.getConnectorAgentName();
            ControllerAgent controllerAgent = compartmentExecutor.getControllerAgent();
            Command command = null;
            if (getEndpointFrom() instanceof VirtualRoom) {
                VirtualRoom virtualRoom = (VirtualRoom) getEndpointFrom();
                command = controllerAgent.performCommandAndWait(new ActionRequestCommand(
                        agentName, new DialParticipant(virtualRoom.getVirtualRoomId(), getAddress().getValue())));
            }
            else {
                command = controllerAgent.performCommandAndWait(new ActionRequestCommand(
                        agentName, new Dial(getAddress().getValue())));
            }
            if (command.getState() != Command.State.SUCCESSFUL) {
                return false;
            }
            setConnectionId((String) command.getResult());
        }
        return true;
    }
}
