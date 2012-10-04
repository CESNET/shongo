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
import org.springframework.scheduling.support.SimpleTriggerContext;

import javax.persistence.Column;
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
public class ResourceVirtualRoom extends VirtualRoom implements ManagedEndpoint
{
    /**
     * {@link DeviceResource}.
     */
    private DeviceResource deviceResource;

    /**
     * Port count.
     */
    private Integer portCount;

    /**
     * Constructor.
     */
    public ResourceVirtualRoom()
    {
    }

    /**
     * Constructor.
     *
     * @param virtualRoomReservation to initialize from
     */
    public ResourceVirtualRoom(VirtualRoomReservation virtualRoomReservation)
    {
        this.setDeviceResource(virtualRoomReservation.getDeviceResource());
        this.setPortCount(virtualRoomReservation.getPortCount());
    }

    /**
     * @return {@link #deviceResource}
     */
    @OneToOne
    public DeviceResource getDeviceResource()
    {
        return deviceResource;
    }

    /**
     * @param deviceResource sets the {@link #deviceResource}
     */
    public void setDeviceResource(DeviceResource deviceResource)
    {
        this.deviceResource = deviceResource;
    }

    /**
     * @return {@link #portCount}
     */
    @Override
    @Column
    public Integer getPortCount()
    {
        return portCount;
    }

    /**
     * @param portCount sets the {@link #portCount}
     */
    public void setPortCount(Integer portCount)
    {
        this.portCount = portCount;
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
        aliases.addAll(super.getAssignedAliases());
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
    public String getConnectorAgentName()
    {
        Mode mode = getDeviceResource().getMode();
        if (mode instanceof ManagedMode) {
            ManagedMode managedMode = (ManagedMode) mode;
            return managedMode.getConnectorAgentName();
        } else {
            throw new IllegalStateException("Resource " + getReportDescription() + " is not managed!");
        }
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

            String roomName = String.format("Shongo%d Comp:%d", getId(), compartmentExecutor.getCompartmentId());
            roomName = roomName.substring(0, Math.min(roomName.length(), 28));

            Room room = new Room();
            room.setPortCount(getPortCount());
            room.setName(roomName);
            for ( Alias alias : getAliases()) {
                room.addAlias(alias.toApi());
            }
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
