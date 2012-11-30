package cz.cesnet.shongo.controller.executor;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.connector.api.ontology.actions.multipoint.rooms.CreateRoom;
import cz.cesnet.shongo.connector.api.ontology.actions.multipoint.rooms.DeleteRoom;
import cz.cesnet.shongo.controller.ControllerAgent;
import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.common.Room;
import cz.cesnet.shongo.controller.reservation.RoomReservation;
import cz.cesnet.shongo.controller.resource.*;
import cz.cesnet.shongo.controller.scheduler.report.AbstractResourceReport;
import cz.cesnet.shongo.jade.command.AgentActionCommand;
import cz.cesnet.shongo.jade.command.Command;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Represents a {@link DeviceResource} which acts as {@link RoomEndpoint} in a {@link Compartment}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class ResourceRoomEndpoint extends RoomEndpoint implements ManagedEndpoint
{
    /**
     * {@link DeviceResource}.
     */
    private DeviceResource deviceResource;

    /**
     * @see Room
     */
    private Room room = new Room();

    /**
     * Constructor.
     */
    public ResourceRoomEndpoint()
    {
    }

    /**
     * Constructor.
     *
     * @param roomReservation to initialize from
     */
    public ResourceRoomEndpoint(RoomReservation roomReservation)
    {
        this.setDeviceResource(roomReservation.getDeviceResource());
        this.setRoom(roomReservation.getRoom());
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
     * @return {@link #room}
     */
    @Override
    @Embedded
    @Access(AccessType.FIELD)
    public Room getRoom()
    {
        return room;
    }

    /**
     * @param room sets the {@link #room}
     */
    public void setRoom(Room room)
    {
        this.room = room;
    }

    @Override
    protected cz.cesnet.shongo.controller.api.Executable createApi()
    {
        return new cz.cesnet.shongo.controller.api.VirtualRoom();
    }

    @Override
    public void toApi(cz.cesnet.shongo.controller.api.Executable executableApi, Domain domain)
    {
        super.toApi(executableApi, domain);

        cz.cesnet.shongo.controller.api.VirtualRoom virtualRoomApi =
                (cz.cesnet.shongo.controller.api.VirtualRoom) executableApi;
        virtualRoomApi.setIdentifier(domain.formatIdentifier(getId()));
        virtualRoomApi.setSlot(getSlot());
        virtualRoomApi.setState(getState().toApi());
        virtualRoomApi.setLicenseCount(room.getLicenseCount());
        virtualRoomApi.setResourceIdentifier(domain.formatIdentifier(getDeviceResource().getId()));
        for (Alias alias : getAssignedAliases()) {
            virtualRoomApi.addAlias(alias.toApi());
        }
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
        }
        else {
            throw new IllegalStateException("Resource " + getReportDescription() + " is not managed!");
        }
    }

    @Override
    protected State onStart(ExecutorThread executorThread, EntityManager entityManager)
    {
        DeviceResource deviceResource = getDeviceResource();
        StringBuilder message = new StringBuilder();
        message.append(String.format("Starting %s for %d licenses.", getReportDescription(), room.getLicenseCount()));
        if (deviceResource.hasIpAddress()) {
            message.append(String.format(" Device has address '%s'.", deviceResource.getAddress().getValue()));
        }
        executorThread.getLogger().debug(message.toString());
        List<Alias> aliases = getAliases();
        for (Alias alias : aliases) {
            StringBuilder aliasMessage = new StringBuilder();
            aliasMessage
                    .append(String.format("%s has allocated alias '%s'.", getReportDescription(), alias.getValue()));
            executorThread.getLogger().debug(aliasMessage.toString());
        }

        if (getDeviceResource().isManaged()) {
            ManagedMode managedMode = (ManagedMode) getDeviceResource().getMode();
            String agentName = managedMode.getConnectorAgentName();
            ControllerAgent controllerAgent = executorThread.getControllerAgent();

            String roomName = String.format("Shongo%d [exec:%d]", getId(), executorThread.getExecutableId());
            roomName = roomName.substring(0, Math.min(roomName.length(), 28));

            cz.cesnet.shongo.api.Room room = new cz.cesnet.shongo.api.Room();
            room.setLicenseCount(this.room.getLicenseCount());
            room.setName(roomName);
            for (Alias alias : getAliases()) {
                room.addAlias(alias.toApi());
            }
            Command command = controllerAgent.performCommandAndWait(new AgentActionCommand(agentName,
                    new CreateRoom(room)));
            if (command.getState() != Command.State.SUCCESSFUL) {
                return State.STARTING_FAILED;
            }
            setRoomId((String) command.getResult());
        }
        return super.onStart(executorThread, entityManager);
    }

    @Override
    protected State onStop(ExecutorThread executorThread, EntityManager entityManager)
    {
        StringBuilder message = new StringBuilder();
        message.append(String.format("Stopping %s for %d ports.", getReportDescription(), room.getLicenseCount()));
        executorThread.getLogger().debug(message.toString());

        if (getDeviceResource().isManaged()) {
            ManagedMode managedMode = (ManagedMode) getDeviceResource().getMode();
            String agentName = managedMode.getConnectorAgentName();
            ControllerAgent controllerAgent = executorThread.getControllerAgent();
            String virtualRoomId = getRoomId();
            if (virtualRoomId == null) {
                throw new IllegalStateException("Cannot delete virtual room because it's identifier is null.");
            }
            Command command = controllerAgent
                    .performCommandAndWait(new AgentActionCommand(agentName, new DeleteRoom(virtualRoomId)));
            if (command.getState() != Command.State.SUCCESSFUL) {
                return State.STARTED;
            }
        }
        return super.onStop(executorThread, entityManager);
    }
}
