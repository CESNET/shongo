package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.PrintableObject;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.Room;
import cz.cesnet.shongo.api.RoomSummary;
import cz.cesnet.shongo.api.RoomUser;
import cz.cesnet.shongo.controller.*;
import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.resource.ManagedMode;
import cz.cesnet.shongo.controller.resource.Mode;
import cz.cesnet.shongo.controller.resource.ResourceManager;
import cz.cesnet.shongo.fault.FaultException;
import cz.cesnet.shongo.fault.TodoImplementException;
import cz.cesnet.shongo.jade.command.ActionRequestCommand;
import cz.cesnet.shongo.jade.command.Command;
import cz.cesnet.shongo.jade.ontology.actions.common.GetSupportedMethods;
import cz.cesnet.shongo.jade.ontology.actions.endpoint.*;
import cz.cesnet.shongo.jade.ontology.actions.multipoint.io.*;
import cz.cesnet.shongo.jade.ontology.actions.multipoint.rooms.*;
import cz.cesnet.shongo.jade.ontology.actions.multipoint.users.*;
import jade.content.AgentAction;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Resource service implementation.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ResourceControlServiceImpl extends Component
        implements ResourceControlService, Component.DomainAware, Component.ControllerAgentAware,
                   Component.EntityManagerFactoryAware, Component.AuthorizationAware
{
    /**
     * @see Domain
     */
    private Domain domain;

    /**
     * @see ControllerAgent
     */
    private ControllerAgent controllerAgent;

    /**
     * @see javax.persistence.EntityManagerFactory
     */
    private EntityManagerFactory entityManagerFactory;

    /**
     * @see cz.cesnet.shongo.controller.Authorization
     */
    private Authorization authorization;

    @Override
    public void setDomain(Domain domain)
    {
        this.domain = domain;
    }

    @Override
    public void setControllerAgent(ControllerAgent controllerAgent)
    {
        this.controllerAgent = controllerAgent;
    }

    @Override
    public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory)
    {
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public void setAuthorization(Authorization authorization)
    {
        this.authorization = authorization;
    }

    @Override
    public void init(Configuration configuration)
    {
        checkDependency(domain, Domain.class);
        checkDependency(controllerAgent, ControllerAgent.class);
        checkDependency(authorization, Authorization.class);
        super.init(configuration);
    }

    @Override
    public String getServiceName()
    {
        return "ResourceControl";
    }

    @Override
    public Collection<String> getSupportedMethods(SecurityToken token, String deviceResourceIdentifier)
            throws FaultException
    {
        authorization.validate(token);
        return (List<String>) commandDevice(deviceResourceIdentifier, new GetSupportedMethods());
    }

    @Override
    public String dial(SecurityToken token, String deviceResourceIdentifier, String address) throws FaultException
    {
        authorization.validate(token);
        return (String) commandDevice(deviceResourceIdentifier, new Dial(address));
    }

    @Override
    public String dial(SecurityToken token, String deviceResourceIdentifier, Alias alias) throws FaultException
    {
        authorization.validate(token);
        return (String) commandDevice(deviceResourceIdentifier, new Dial(alias));
    }

    @Override
    public void standBy(SecurityToken token, String deviceResourceIdentifier) throws FaultException
    {
        authorization.validate(token);
        commandDevice(deviceResourceIdentifier, new StandBy());
    }

    @Override
    public void hangUp(SecurityToken token, String deviceResourceIdentifier, String callId) throws FaultException
    {
        authorization.validate(token);
        commandDevice(deviceResourceIdentifier, new HangUp(callId));
    }

    @Override
    public void hangUpAll(SecurityToken token, String deviceResourceIdentifier) throws FaultException
    {
        authorization.validate(token);
        commandDevice(deviceResourceIdentifier, new HangUpAll());
    }

    @Override
    public void resetDevice(SecurityToken token, String deviceResourceIdentifier) throws FaultException
    {
        authorization.validate(token);
        commandDevice(deviceResourceIdentifier, new ResetDevice());
    }

    @Override
    public void mute(SecurityToken token, String deviceResourceIdentifier) throws FaultException
    {
        authorization.validate(token);
        commandDevice(deviceResourceIdentifier, new Mute());
    }

    @Override
    public void unmute(SecurityToken token, String deviceResourceIdentifier) throws FaultException
    {
        authorization.validate(token);
        commandDevice(deviceResourceIdentifier, new Unmute());
    }

    @Override
    public void setMicrophoneLevel(SecurityToken token, String deviceResourceIdentifier, int level)
            throws FaultException
    {
        authorization.validate(token);
        commandDevice(deviceResourceIdentifier, new SetMicrophoneLevel(level));
    }

    @Override
    public void setPlaybackLevel(SecurityToken token, String deviceResourceIdentifier, int level) throws FaultException
    {
        authorization.validate(token);
        commandDevice(deviceResourceIdentifier, new SetPlaybackLevel(level));
    }

    @Override
    public void enableVideo(SecurityToken token, String deviceResourceIdentifier) throws FaultException
    {
        authorization.validate(token);
        commandDevice(deviceResourceIdentifier, new EnableVideo());
    }

    @Override
    public void disableVideo(SecurityToken token, String deviceResourceIdentifier) throws FaultException
    {
        authorization.validate(token);
        commandDevice(deviceResourceIdentifier, new DisableVideo());
    }

    @Override
    public void startPresentation(SecurityToken token, String deviceResourceIdentifier) throws FaultException
    {
        authorization.validate(token);
        commandDevice(deviceResourceIdentifier, new StartPresentation());
    }

    @Override
    public void stopPresentation(SecurityToken token, String deviceResourceIdentifier) throws FaultException
    {
        authorization.validate(token);
        commandDevice(deviceResourceIdentifier, new StopPresentation());
    }

    @Override
    public String dialParticipant(SecurityToken token, String deviceResourceIdentifier, String roomId, String address)
            throws FaultException
    {
        authorization.validate(token);
        return (String) commandDevice(deviceResourceIdentifier, new DialParticipant(roomId, address));
    }

    @Override
    public String dialParticipant(SecurityToken token, String deviceResourceIdentifier, String roomId, Alias alias)
            throws FaultException
    {
        authorization.validate(token);
        return (String) commandDevice(deviceResourceIdentifier, new DialParticipant(roomId, alias));
    }

    @Override
    public void disconnectParticipant(SecurityToken token, String deviceResourceIdentifier, String roomId,
            String roomUserId) throws FaultException
    {
        authorization.validate(token);
        commandDevice(deviceResourceIdentifier, new DisconnectParticipant(roomId, roomUserId));
    }

    @Override
    public Collection<RoomSummary> listRooms(SecurityToken token, String deviceResourceIdentifier) throws FaultException
    {
        authorization.validate(token);
        Collection<RoomSummary> rooms = (Collection<RoomSummary>) commandDevice(deviceResourceIdentifier, new ListRooms());
        return rooms;
    }

    @Override
    public RoomSummary getRoomSummary(SecurityToken token, String deviceResourceIdentifier, String roomId)
            throws FaultException
    {
        authorization.validate(token);
        return (RoomSummary) commandDevice(deviceResourceIdentifier, new GetRoomSummary(roomId));
    }

    @Override
    public Room getRoom(SecurityToken token, String deviceResourceIdentifier, String roomId) throws FaultException
    {
        Room room = new Room();
        room.setIdentifier("1");
        room.setName("Fixed Testing Room (TODO: Remove it)");
        room.setPortCount(5);
        room.addAlias(new Alias(Technology.H323, AliasType.E164, "9501"));
        room.setOption(Room.Option.DESCRIPTION, "room description");
        return room;
    }

    @Override
    public String createRoom(SecurityToken token, String deviceResourceIdentifier, Room room) throws FaultException
    {
        authorization.validate(token);
        return (String) commandDevice(deviceResourceIdentifier, new CreateRoom(room));
    }

    @Override
    public String modifyRoom(SecurityToken token, String deviceResourceIdentifier, Room room) throws FaultException
    {
        authorization.validate(token);
        //return (String) commandDevice(deviceResourceIdentifier, new ModifyRoom(roomId, attributes, options));
        throw new TodoImplementException();
    }

    @Override
    public void deleteRoom(SecurityToken token, String deviceResourceIdentifier, String roomId) throws FaultException
    {
        authorization.validate(token);
        commandDevice(deviceResourceIdentifier, new DeleteRoom(roomId));
    }

    @Override
    public Collection<RoomUser> listParticipants(SecurityToken token, String deviceResourceIdentifier, String roomId)
            throws FaultException
    {
        authorization.validate(token);
        return (List<RoomUser>) commandDevice(deviceResourceIdentifier, new ListParticipants(roomId));
    }

    @Override
    public RoomUser getParticipant(SecurityToken token, String deviceResourceIdentifier, String roomId,
            String roomUserId) throws FaultException
    {
        authorization.validate(token);
        return (RoomUser) commandDevice(deviceResourceIdentifier, new GetParticipant(roomId, roomUserId));
    }

    @Override
    public void modifyParticipant(SecurityToken token, String deviceResourceIdentifier, String roomId,
            String roomUserId, Map<String, Object> attributes) throws FaultException
    {
        authorization.validate(token);
        commandDevice(deviceResourceIdentifier, new ModifyParticipant(roomId, roomUserId, attributes));
    }

    @Override
    public void muteParticipant(SecurityToken token, String deviceResourceIdentifier, String roomId, String roomUserId)
            throws FaultException
    {
        authorization.validate(token);
        commandDevice(deviceResourceIdentifier, new MuteParticipant(roomId, roomUserId));
    }

    @Override
    public void unmuteParticipant(SecurityToken token, String deviceResourceIdentifier, String roomId,
            String roomUserId) throws FaultException
    {
        authorization.validate(token);
        commandDevice(deviceResourceIdentifier, new UnmuteParticipant(roomId, roomUserId));
    }

    @Override
    public void enableParticipantVideo(SecurityToken token, String deviceResourceIdentifier, String roomId,
            String roomUserId) throws FaultException
    {
        authorization.validate(token);
        commandDevice(deviceResourceIdentifier, new EnableParticipantVideo(roomId, roomUserId));
    }

    @Override
    public void disableParticipantVideo(SecurityToken token, String deviceResourceIdentifier, String roomId,
            String roomUserId) throws FaultException
    {
        authorization.validate(token);
        commandDevice(deviceResourceIdentifier, new DisableParticipantVideo(roomId, roomUserId));
    }

    @Override
    public void setParticipantMicrophoneLevel(SecurityToken token, String deviceResourceIdentifier, String roomId,
            String roomUserId, int level) throws FaultException
    {
        authorization.validate(token);
        commandDevice(deviceResourceIdentifier, new SetParticipantMicrophoneLevel(roomId, roomUserId, level));
    }

    @Override
    public void setParticipantPlaybackLevel(SecurityToken token, String deviceResourceIdentifier, String roomId,
            String roomUserId, int level) throws FaultException
    {
        authorization.validate(token);
        commandDevice(deviceResourceIdentifier, new SetParticipantPlaybackLevel(roomId, roomUserId, level));
    }

    /**
     * Asks the local controller agent to send a command to be performed by a device.
     *
     * @param deviceResourceIdentifier identifier of device to perform a command
     * @param action                   command to be performed by the device
     * @throws FaultException
     */
    private Object commandDevice(String deviceResourceIdentifier, AgentAction action) throws FaultException
    {
        String agentName = getAgentName(deviceResourceIdentifier);
        Command command = controllerAgent.performCommandAndWait(new ActionRequestCommand(agentName, action));
        if (command.getState() == Command.State.SUCCESSFUL) {
            return command.getResult();
        }
        throw new FaultException(command.getStateDescription());
    }

    /**
     * Gets name of agent managing a given device.
     *
     * @param deviceResourceIdentifier identifier of device agent of which to get
     * @return agent name of managed resource with given {@code deviceResourceIdentifier}
     * @throws FaultException when resource doesn't exist or when is not managed
     */
    private String getAgentName(String deviceResourceIdentifier) throws FaultException
    {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        Long deviceResourceId = domain.parseIdentifier(deviceResourceIdentifier);
        ResourceManager resourceManager = new ResourceManager(entityManager);
        DeviceResource deviceResource = resourceManager.getDevice(deviceResourceId);
        entityManager.close();
        Mode mode = deviceResource.getMode();
        if (mode instanceof ManagedMode) {
            ManagedMode managedMode = (ManagedMode) mode;
            return managedMode.getConnectorAgentName();
        }
        throw new FaultException("Resource '%s' is not managed!", deviceResourceIdentifier);
    }
}
