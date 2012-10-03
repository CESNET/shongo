package cz.cesnet.shongo.controller.compartment;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.Room;
import cz.cesnet.shongo.controller.ControllerAgent;
import cz.cesnet.shongo.controller.reservation.VirtualRoomReservation;
import cz.cesnet.shongo.controller.resource.*;
import cz.cesnet.shongo.controller.scheduler.report.AbstractResourceReport;
import cz.cesnet.shongo.jade.command.Command;
import cz.cesnet.shongo.jade.command.SendCommand;
import cz.cesnet.shongo.jade.ontology.CreateRoom;
import cz.cesnet.shongo.jade.ontology.DeleteRoom;

import javax.persistence.Entity;
import javax.persistence.OneToOne;
import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Represents a {@link DeviceResource} which acts as {@link VirtualRoom} in a {@link Compartment}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class ResourceVirtualRoom extends VirtualRoom
{
    /**
     * {@link VirtualRoomReservation} for the {@link DeviceResource}.
     */
    private VirtualRoomReservation virtualRoomReservation;

    /**
     * Constructor.
     */
    public ResourceVirtualRoom()
    {
    }

    /**
     * Constructor.
     *
     * @param virtualRoomReservation sets the {@link #virtualRoomReservation}
     */
    public ResourceVirtualRoom(VirtualRoomReservation virtualRoomReservation)
    {
        this.virtualRoomReservation = virtualRoomReservation;
    }

    /**
     * @return {@link #virtualRoomReservation}
     */
    @OneToOne
    public VirtualRoomReservation getVirtualRoomReservation()
    {
        return virtualRoomReservation;
    }

    /**
     * @param virtualRoomReservation sets the {@link #virtualRoomReservation}
     */
    public void setVirtualRoomReservation(VirtualRoomReservation virtualRoomReservation)
    {
        this.virtualRoomReservation = virtualRoomReservation;
    }

    /**
     * @return {@link DeviceResource}
     */
    @Transient
    public DeviceResource getDeviceResource()
    {
        return virtualRoomReservation.getDeviceResource();
    }

    @Override
    @Transient
    public Set<Technology> getTechnologies()
    {
        return getDeviceResource().getTechnologies();
    }

    @Override
    @Transient
    public boolean isStandalone()
    {
        return getDeviceResource().isStandaloneTerminal();
    }

    @Override
    @Transient
    public List<Alias> getAliases()
    {
        List<Alias> aliases = new ArrayList<Alias>();
        TerminalCapability terminalCapability = getDeviceResource().getCapability(TerminalCapability.class);
        if (terminalCapability != null) {
            aliases.addAll(terminalCapability.getAliases());
        }
        aliases.addAll(super.getAliases());
        return aliases;
    }

    @Override
    @Transient
    public Address getAddress()
    {
        return getDeviceResource().getAddress();
    }

    @Override
    @Transient
    public String getReportDescription()
    {
        return String.format("virtual room in %s",
                AbstractResourceReport.formatResource(getDeviceResource()));
    }

    @Override
    @Transient
    public Integer getPortCount()
    {
        return virtualRoomReservation.getPortCount();
    }

    @Override
    @Transient
    public boolean onCreate(CompartmentExecutor compartmentExecutor)
    {
        DeviceResource deviceResource = getDeviceResource();
        StringBuilder message = new StringBuilder();
        message.append(String.format("Starting %s for %d ports.", getReportDescription(), getPortCount()));
        if (deviceResource.hasIpAddress()) {
            message.append(String.format(" Device has address '%s'.", deviceResource.getAddress().getValue()));
        }
        compartmentExecutor.getLogger().debug(message.toString());
        List<Alias> aliases = getAliases();
        for (Alias alias : aliases) {
            StringBuilder aliasMessage = new StringBuilder();
            aliasMessage
                    .append(String.format("%s has allocated alias '%s'.", getReportDescription(), alias.getValue()));
            compartmentExecutor.getLogger().debug(aliasMessage.toString());
        }

        if (getDeviceResource().isManaged()) {
            ManagedMode managedMode = (ManagedMode) getDeviceResource().getMode();
            String agentName = managedMode.getConnectorAgentName();
            ControllerAgent controllerAgent = compartmentExecutor.getControllerAgent();
            Room room = new Room();
            room.setPortCount(getPortCount());
            room.setName(UUID.randomUUID().toString().substring(0, 20));
            Command command = controllerAgent.performCommandAndWait(SendCommand.createSendCommand(agentName,
                    new CreateRoom(room)));
            if (command.getState() != Command.State.SUCCESSFUL) {
                return false;
            }
            setVirtualRoomId((String) command.getResult());
        }
        return true;
    }

    @Override
    public boolean onDelete(CompartmentExecutor compartmentExecutor)
    {
        StringBuilder message = new StringBuilder();
        message.append(String.format("Stopping %s for %d ports.", getReportDescription(), getPortCount()));
        compartmentExecutor.getLogger().debug(message.toString());

        if (getDeviceResource().isManaged()) {
            ManagedMode managedMode = (ManagedMode) getDeviceResource().getMode();
            String agentName = managedMode.getConnectorAgentName();
            ControllerAgent controllerAgent = compartmentExecutor.getControllerAgent();
            String virtualRoomId = getVirtualRoomId();
            if (virtualRoomId == null) {
                throw new IllegalStateException("Cannot delete virtual room because it's identifier is null.");
            }
            Command command = controllerAgent.performCommandAndWait(SendCommand.createSendCommand(agentName, new DeleteRoom(virtualRoomId)));
            if (command.getState() != Command.State.SUCCESSFUL) {
                return false;
            }
        }
        return true;
    }
}
