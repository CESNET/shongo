package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;
import cz.cesnet.shongo.api.Room;
import cz.cesnet.shongo.api.RoomSetting;
import cz.cesnet.shongo.connector.api.ontology.ConnectorOntology;
import cz.cesnet.shongo.connector.api.ontology.actions.multipoint.rooms.CreateRoom;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.executor.*;
import cz.cesnet.shongo.jade.Agent;
import cz.cesnet.shongo.jade.command.AgentActionResponderBehaviour;
import jade.content.AgentAction;
import jade.core.AID;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertEquals;

/**
 * Tests for {@link Executor}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ExecutorTest extends AbstractControllerTest
{
    private static Logger logger = LoggerFactory.getLogger(ExecutorTest.class);

    /**
     * @see Executor
     */
    private Executor executor;

    /**
     * Constructor.
     */
    public ExecutorTest()
    {
        // Executor configuration
        System.setProperty(Configuration.EXECUTOR_EXECUTABLE_START, "PT0S");
        System.setProperty(Configuration.EXECUTOR_EXECUTABLE_END, "PT0S");
        System.setProperty(Configuration.EXECUTOR_STARTINT_DURATION_ROOM, "PT0S");
    }

    @Override
    protected void onInit()
    {
        super.onInit();

        Controller controller = getController();
        executor = new Executor();
        executor.setEntityManagerFactory(getEntityManagerFactory());
        executor.init(controller.getConfiguration());
        executor.setControllerAgent(controller.getAgent());
    }

    @Override
    protected void onStart()
    {
        super.onStart();

        getController().startJade();
    }

    /**
     * Allocate {@link RoomEndpoint} and execute it.
     *
     * @throws Exception
     */
    @Test
    public void testRoom() throws Exception
    {
        ConnectorAgent mcuAgent = getController().addJadeAgent("mcu", new ConnectorAgent());

        DateTime dateTime = DateTime.parse("2012-01-01T12:00");
        Period duration = Period.parse("PT2M");

        DeviceResource mcu = new DeviceResource();
        mcu.setName("mcu");
        mcu.addTechnology(Technology.H323);
        mcu.addCapability(new RoomProviderCapability(10));
        mcu.setAllocatable(true);
        mcu.setMode(new ManagedMode(mcuAgent.getName()));
        String mcuId = getResourceService().createResource(SECURITY_TOKEN, mcu);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot(dateTime, duration);
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        RoomSpecification roomSpecification = new RoomSpecification();
        roomSpecification.addTechnology(Technology.H323);
        roomSpecification.setParticipantCount(5);
        reservationRequest.setSpecification(roomSpecification);

        // Allocate reservation request
        allocateAndCheck(reservationRequest);

        // Start virtual room
        ExecutionResult result = executor.execute(dateTime);
        assertEquals("One executable should be started.", 1, result.getStartedExecutables().size());
        assertEquals("The started executable should be virtual room.",
                ResourceRoomEndpoint.class, result.getStartedExecutables().get(0).getClass());
        // Stop virtual room
        result = executor.execute(dateTime.plus(duration));
        assertEquals("One executable should be stopped.", 1, result.getStoppedExecutables().size());

        // Check performed actions on connector agents
        assertEquals(new ArrayList<Object>()
        {{
                add(cz.cesnet.shongo.connector.api.ontology.actions.multipoint.rooms.CreateRoom.class);
                add(cz.cesnet.shongo.connector.api.ontology.actions.multipoint.rooms.DeleteRoom.class);
            }}, mcuAgent.getPerformedActionClasses());
    }

    /**
     * Allocate {@link cz.cesnet.shongo.controller.api.Executable.Compartment} and execute it.
     *
     * @throws Exception
     */
    @Test
    public void testCompartment() throws Exception
    {
        ConnectorAgent terminalAgent = getController().addJadeAgent("terminal", new ConnectorAgent());
        ConnectorAgent mcuAgent = getController().addJadeAgent("mcu", new ConnectorAgent());

        DateTime dateTime = DateTime.parse("2012-01-01T12:00");
        Period duration = Period.parse("PT2M");

        Resource aliasProvider = new Resource();
        aliasProvider.setName("aliasProvider");
        aliasProvider.setAllocatable(true);
        aliasProvider.addCapability(new AliasProviderCapability("9500872{digit:2}", AliasType.H323_E164));
        String aliasProviderId = getResourceService().createResource(SECURITY_TOKEN, aliasProvider);

        DeviceResource terminal = new DeviceResource();
        terminal.setName("terminal");
        terminal.addTechnology(Technology.H323);
        terminal.addCapability(new TerminalCapability());
        terminal.setAllocatable(true);
        terminal.setMode(new ManagedMode(terminalAgent.getName()));
        String terminalId = getResourceService().createResource(SECURITY_TOKEN, terminal);

        DeviceResource mcu = new DeviceResource();
        mcu.setName("mcu");
        mcu.addTechnology(Technology.H323);
        mcu.addCapability(new RoomProviderCapability(10));
        mcu.setAllocatable(true);
        mcu.setMode(new ManagedMode(mcuAgent.getName()));
        String mcuId = getResourceService().createResource(SECURITY_TOKEN, mcu);

        // Create compartment reservation
        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot(dateTime, duration);
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        CompartmentSpecification compartmentSpecification = new CompartmentSpecification();
        compartmentSpecification.addSpecification(new ExistingEndpointSpecification(terminalId));
        compartmentSpecification.addSpecification(new ExternalEndpointSetSpecification(Technology.H323, 1));
        reservationRequest.setSpecification(compartmentSpecification);
        allocateAndCheck(reservationRequest);

        // Start compartment
        ExecutionResult result = executor.execute(dateTime);
        assertEquals("Three executables should be started.", 3, result.getStartedExecutables().size());
        assertEquals("The first started executable should be virtual room.",
                ResourceRoomEndpoint.class, result.getStartedExecutables().get(0).getClass());
        assertEquals("The second started executable should be connection.",
                Connection.class, result.getStartedExecutables().get(1).getClass());
        assertEquals("The third started executable should be compartment.",
                Compartment.class, result.getStartedExecutables().get(2).getClass());
        // Stop compartment
        result = executor.execute(dateTime.plus(duration));
        assertEquals("Three executables should be stopped.", 3, result.getStoppedExecutables().size());

        // Check performed actions on connector agents
        assertEquals(new ArrayList<Object>()
        {{
                add(cz.cesnet.shongo.connector.api.ontology.actions.endpoint.Dial.class);
                add(cz.cesnet.shongo.connector.api.ontology.actions.endpoint.HangUpAll.class);
            }}, terminalAgent.getPerformedActionClasses());
        assertEquals(new ArrayList<Object>()
        {{
                add(cz.cesnet.shongo.connector.api.ontology.actions.multipoint.rooms.CreateRoom.class);
                add(cz.cesnet.shongo.connector.api.ontology.actions.multipoint.rooms.DeleteRoom.class);
            }}, mcuAgent.getPerformedActionClasses());
    }

    /**
     * Allocate {@link RoomEndpoint} and execute it.
     *
     * @throws Exception
     */
    @Test
    public void testAlias() throws Exception
    {
        ConnectorAgent connectServerAgent = getController().addJadeAgent("connectServer", new ConnectorAgent());

        DateTime dateTime = DateTime.parse("2012-01-01T12:00");
        Period duration = Period.parse("PT2M");

        DeviceResource connectServer = new DeviceResource();
        connectServer.setName("connectServer");
        connectServer.setAddress("127.0.0.1");
        connectServer.addTechnology(Technology.ADOBE_CONNECT);
        connectServer.addCapability(new RoomProviderCapability(10));
        connectServer.addCapability(new AliasProviderCapability("test", AliasType.ADOBE_CONNECT_URI,
                "{device.address}/{value}").withPermanentRoom());
        connectServer.setAllocatable(true);
        connectServer.setMode(new ManagedMode(connectServerAgent.getName()));
        String mcuId = getResourceService().createResource(SECURITY_TOKEN, connectServer);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot(dateTime, duration);
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new AliasSpecification(Technology.ADOBE_CONNECT));

        // Allocate reservation request
        allocateAndCheck(reservationRequest);

        // Start virtual room
        ExecutionResult result = executor.execute(dateTime);
        assertEquals("One executable should be started.", 1, result.getStartedExecutables().size());
        assertEquals("The started executable should be virtual room.",
                ResourceRoomEndpoint.class, result.getStartedExecutables().get(0).getClass());
        // Stop virtual room
        result = executor.execute(dateTime.plus(duration));
        assertEquals("One executable should be stopped.", 1, result.getStoppedExecutables().size());

        // Check performed actions on connector agents
        assertEquals(new ArrayList<Object>()
        {{
                add(cz.cesnet.shongo.connector.api.ontology.actions.multipoint.rooms.CreateRoom.class);
                add(cz.cesnet.shongo.connector.api.ontology.actions.multipoint.rooms.DeleteRoom.class);
            }}, connectServerAgent.getPerformedActionClasses());
    }

    /**
     * Allocate {@link RoomEndpoint} and execute it.
     *
     * @throws Exception
     */
    @Test
    public void testRoomWithSetting() throws Exception
    {
        ConnectorAgent mcuAgent = getController().addJadeAgent("mcu", new ConnectorAgent());

        DateTime dateTime = DateTime.parse("2012-01-01T12:00");
        Period duration = Period.parse("PT2M");

        DeviceResource mcu = new DeviceResource();
        mcu.setName("mcu");
        mcu.addTechnology(Technology.H323);
        mcu.addCapability(new RoomProviderCapability(10));
        mcu.setAllocatable(true);
        mcu.setMode(new ManagedMode(mcuAgent.getName()));
        String mcuId = getResourceService().createResource(SECURITY_TOKEN, mcu);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot(dateTime, duration);
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        RoomSpecification roomSpecification = new RoomSpecification();
        roomSpecification.addTechnology(Technology.H323);
        roomSpecification.setParticipantCount(5);
        roomSpecification.addRoomSetting(new RoomSetting.H323().withPin("1234"));
        reservationRequest.setSpecification(roomSpecification);

        // Allocate reservation request
        allocateAndCheck(reservationRequest);

        // Start virtual room
        ExecutionResult result = executor.execute(dateTime);
        assertEquals("One executable should be started.", 1, result.getStartedExecutables().size());
        assertEquals("The started executable should be virtual room.",
                ResourceRoomEndpoint.class, result.getStartedExecutables().get(0).getClass());
        // Stop virtual room
        result = executor.execute(dateTime.plus(duration));
        assertEquals("One executable should be stopped.", 1, result.getStoppedExecutables().size());

        // Check performed actions on connector agents
        assertEquals(new ArrayList<Object>()
        {{
                add(cz.cesnet.shongo.connector.api.ontology.actions.multipoint.rooms.CreateRoom.class);
                add(cz.cesnet.shongo.connector.api.ontology.actions.multipoint.rooms.DeleteRoom.class);
            }}, mcuAgent.getPerformedActionClasses());
        cz.cesnet.shongo.connector.api.ontology.actions.multipoint.rooms.CreateRoom createRoomAction =
                mcuAgent.getPerformedActionByClass(
                        cz.cesnet.shongo.connector.api.ontology.actions.multipoint.rooms.CreateRoom.class);
        assertEquals("1234", createRoomAction.getRoom().getOption(Room.Option.PIN));
    }

    /**
     * Allocate {@link RoomEndpoint} through {@link AliasReservation} and use it by {@link RoomReservation}.
     * The preference of provided room is also tested by presence of fake connect server (which should not be used).
     *
     * @throws Exception
     */
    @Test
    public void testProvidedPermanentAlias() throws Exception
    {
        ConnectorAgent connectServerAgent = getController().addJadeAgent("connectServer", new ConnectorAgent());

        DateTime dateTime = DateTime.parse("2012-01-01T12:00");
        Period duration = Period.parse("PT2M");

        DeviceResource connectServerFake = new DeviceResource();
        connectServerFake.setName("connectServerFake");
        connectServerFake.addTechnology(Technology.ADOBE_CONNECT);
        connectServerFake.addCapability(new RoomProviderCapability(10));
        connectServerFake.addCapability(new AliasProviderCapability("fake", AliasType.ADOBE_CONNECT_URI,
                "{device.address}/{value}").withPermanentRoom());
        connectServerFake.setAllocatable(true);
        connectServerFake.setMode(new ManagedMode(connectServerAgent.getName()));
        getResourceService().createResource(SECURITY_TOKEN, connectServerFake);

        DeviceResource connectServer = new DeviceResource();
        connectServer.setName("connectServer");
        connectServer.setAddress("127.0.0.1");
        connectServer.addTechnology(Technology.ADOBE_CONNECT);
        connectServer.addCapability(new RoomProviderCapability(10));
        connectServer.addCapability(new AliasProviderCapability("test", AliasType.ADOBE_CONNECT_URI,
                "{device.address}/{value}").withPermanentRoom());
        connectServer.setAllocatable(true);
        connectServer.setMode(new ManagedMode(connectServerAgent.getName()));
        String connectServerId = getResourceService().createResource(SECURITY_TOKEN, connectServer);

        ReservationRequest aliasReservationRequest = new ReservationRequest();
        aliasReservationRequest.setSlot(dateTime, duration);
        aliasReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        aliasReservationRequest.setSpecification(
                new AliasSpecification(Technology.ADOBE_CONNECT).withResourceId(connectServerId));
        AliasReservation aliasReservation = (AliasReservation) allocateAndCheck(aliasReservationRequest);
        assertEquals("Alias should not be allocated from the fake connect server.",
                "test", aliasReservation.getValue());

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot(dateTime, duration);
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new RoomSpecification(10, Technology.ADOBE_CONNECT));
        reservationRequest.addProvidedReservationId(aliasReservation.getId());
        allocateAndCheck(reservationRequest);

        // Start virtual rooms
        ExecutionResult result = executor.execute(dateTime);
        assertEquals("Two executables should be started.", 2, result.getStartedExecutables().size());
        assertEquals("The first started executable should be virtual room.",
                ResourceRoomEndpoint.class, result.getStartedExecutables().get(0).getClass());
        assertEquals("The second started executable should be used virtual room.",
                UsedRoomEndpoint.class, result.getStartedExecutables().get(1).getClass());
        // Stop virtual rooms
        result = executor.execute(dateTime.plus(duration));
        assertEquals("Two executables should be stopped.", 2, result.getStoppedExecutables().size());

        // Check performed actions on connector agents
        List<Class<? extends AgentAction>> performedActionClasses = connectServerAgent.getPerformedActionClasses();
        assertEquals(new ArrayList<Object>()
        {{
                add(cz.cesnet.shongo.connector.api.ontology.actions.multipoint.rooms.CreateRoom.class);
                add(cz.cesnet.shongo.connector.api.ontology.actions.multipoint.rooms.ModifyRoom.class);
                add(cz.cesnet.shongo.connector.api.ontology.actions.multipoint.rooms.ModifyRoom.class);
                add(cz.cesnet.shongo.connector.api.ontology.actions.multipoint.rooms.DeleteRoom.class);
            }}, performedActionClasses);
    }

    /**
     * Allocate {@link RoomEndpoint}, provide it to {@link cz.cesnet.shongo.controller.executor.Compartment} and execute
     * both of them separately (first {@link RoomEndpoint} and then
     * {@link cz.cesnet.shongo.controller.executor.Compartment}).
     *
     * @throws Exception
     */
    @Test
    public void testProvidedRoomStartedSeparately() throws Exception
    {
        ConnectorAgent mcuAgent = getController().addJadeAgent("mcu", new ConnectorAgent());

        DateTime dateTime = DateTime.parse("2012-01-01T12:00");
        Period duration = Period.parse("PT2M");

        DeviceResource mcu = new DeviceResource();
        mcu.setName("mcu");
        mcu.addTechnology(Technology.H323);
        mcu.addCapability(new RoomProviderCapability(10));
        mcu.addCapability(new AliasProviderCapability("950000001", AliasType.H323_E164).withRestrictedToResource());
        mcu.setAllocatable(true);
        mcu.setMode(new ManagedMode(mcuAgent.getName()));
        String mcuId = getResourceService().createResource(SECURITY_TOKEN, mcu);

        // Create virtual room reservation
        ReservationRequest roomReservationRequest = new ReservationRequest();
        roomReservationRequest.setSlot(dateTime, duration);
        roomReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        RoomSpecification roomSpecification = new RoomSpecification();
        roomSpecification.addTechnology(Technology.H323);
        roomSpecification.setParticipantCount(10);
        roomReservationRequest.setSpecification(roomSpecification);
        String roomReservationId = allocateAndCheck(roomReservationRequest).getId();

        // Start virtual room
        ExecutionResult result = executor.execute(dateTime);
        assertEquals("One executable should be started.", 1, result.getStartedExecutables().size());
        assertEquals("The started executable should be virtual room.",
                ResourceRoomEndpoint.class, result.getStartedExecutables().get(0).getClass());

        // Create compartment reservation
        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot(dateTime, duration);
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        CompartmentSpecification compartmentSpecification = new CompartmentSpecification();
        compartmentSpecification.addSpecification(new ExternalEndpointSetSpecification(Technology.H323, 10));
        reservationRequest.setSpecification(compartmentSpecification);
        reservationRequest.addProvidedReservationId(roomReservationId);
        allocateAndCheck(reservationRequest);

        // Start compartment
        result = executor.execute(dateTime);
        assertEquals("One executable should be started.", 1, result.getStartedExecutables().size());
        assertEquals("The started executable should be virtual room.",
                Compartment.class, result.getStartedExecutables().get(0).getClass());

        // Stop virtual room and compartment
        result = executor.execute(dateTime.plus(duration));
        assertEquals("Two executables should be stopped.", 2, result.getStoppedExecutables().size());

        // Check performed actions on connector agents
        assertEquals(new ArrayList<Object>()
        {{
                add(cz.cesnet.shongo.connector.api.ontology.actions.multipoint.rooms.CreateRoom.class);
                add(cz.cesnet.shongo.connector.api.ontology.actions.multipoint.rooms.DeleteRoom.class);
            }}, mcuAgent.getPerformedActionClasses());
    }

    /**
     * Allocate {@link RoomEndpoint}, provide it to {@link cz.cesnet.shongo.controller.executor.Compartment} and execute
     * both at once ({@link RoomEndpoint} through the {@link cz.cesnet.shongo.controller.executor.Compartment}).
     *
     * @throws Exception
     */
    @Test
    public void testProvidedRoomStartedAtOnce() throws Exception
    {
        ConnectorAgent mcuAgent = getController().addJadeAgent("mcu", new ConnectorAgent());

        DateTime dateTime = DateTime.parse("2012-01-01T12:00");
        Period duration = Period.parse("PT2M");

        DeviceResource mcu = new DeviceResource();
        mcu.setName("mcu");
        mcu.addTechnology(Technology.H323);
        mcu.addCapability(new RoomProviderCapability(10));
        mcu.addCapability(new AliasProviderCapability("950000001", AliasType.H323_E164).withRestrictedToResource());
        mcu.setAllocatable(true);
        mcu.setMode(new ManagedMode(mcuAgent.getName()));
        String mcuId = getResourceService().createResource(SECURITY_TOKEN, mcu);

        // Create virtual room reservation
        ReservationRequest roomReservationRequest = new ReservationRequest();
        roomReservationRequest.setSlot(dateTime, duration);
        roomReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        RoomSpecification roomSpecification = new RoomSpecification();
        roomSpecification.addTechnology(Technology.H323);
        roomSpecification.setParticipantCount(10);
        roomReservationRequest.setSpecification(roomSpecification);
        String roomReservationId = allocateAndCheck(roomReservationRequest).getId();

        // Create compartment reservation
        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot(dateTime, duration);
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        CompartmentSpecification compartmentSpecification = new CompartmentSpecification();
        compartmentSpecification.addSpecification(new ExternalEndpointSetSpecification(Technology.H323, 10));
        reservationRequest.setSpecification(compartmentSpecification);
        reservationRequest.addProvidedReservationId(roomReservationId);
        allocateAndCheck(reservationRequest);

        // Execute compartment
        ExecutionResult result = executor.execute(dateTime);
        assertEquals("Two executables should be started.", 2, result.getStartedExecutables().size());
        assertEquals("The first started executable should be virtual room.",
                ResourceRoomEndpoint.class, result.getStartedExecutables().get(0).getClass());
        assertEquals("The second started executable should be copartment.",
                Compartment.class, result.getStartedExecutables().get(1).getClass());

        // Stop compartment
        result = executor.execute(dateTime.plus(duration));
        assertEquals("Two executables should be stopped.", 2, result.getStoppedExecutables().size());

        // Check performed actions on connector agents
        assertEquals(new ArrayList<Object>()
        {{
                add(cz.cesnet.shongo.connector.api.ontology.actions.multipoint.rooms.CreateRoom.class);
                add(cz.cesnet.shongo.connector.api.ontology.actions.multipoint.rooms.DeleteRoom.class);
            }}, mcuAgent.getPerformedActionClasses());
    }

    /**
     * Test delete {@link ReservationRequest} with started {@link ResourceRoomEndpoint}.
     *
     * @throws Exception
     */
    @Test
    public void testDeleteStarted() throws Exception
    {
        ConnectorAgent mcuAgent = getController().addJadeAgent("mcu", new ConnectorAgent());

        DateTime dateTime = DateTime.now();
        Period duration = Period.parse("PT2H");

        DeviceResource mcu = new DeviceResource();
        mcu.setName("mcu");
        mcu.addTechnology(Technology.H323);
        mcu.addCapability(new RoomProviderCapability(10));
        mcu.addCapability(new AliasProviderCapability("950000001", AliasType.H323_E164));
        mcu.setAllocatable(true);
        mcu.setMode(new ManagedMode(mcuAgent.getName()));
        String mcuId = getResourceService().createResource(SECURITY_TOKEN, mcu);

        // Create virtual room reservation
        ReservationRequest roomReservationRequest = new ReservationRequest();
        roomReservationRequest.setSlot(dateTime, duration);
        roomReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        RoomSpecification roomSpecification = new RoomSpecification();
        roomSpecification.addTechnology(Technology.H323);
        roomSpecification.setParticipantCount(10);
        roomSpecification.addRoomSetting(new RoomSetting.H323().withPin("1234"));
        roomReservationRequest.setSpecification(roomSpecification);
        String roomReservationRequestId = allocate(roomReservationRequest);
        checkAllocated(roomReservationRequestId);

        // Execute compartment
        ExecutionResult result = executor.execute(dateTime);
        assertEquals("One executable should be started.", 1, result.getStartedExecutables().size());
        assertEquals("The started executable should be virtual room.",
                ResourceRoomEndpoint.class, result.getStartedExecutables().get(0).getClass());

        Thread.sleep(1000);

        // Delete reservation request and the reservation
        getReservationService().deleteReservationRequest(SECURITY_TOKEN, roomReservationRequestId);
        // Run scheduler to modify room ending date/time
        runScheduler();

        // Stop compartment
        result = executor.execute(dateTime.plus(duration));
        assertEquals("One executable should be stopped.", 1, result.getStoppedExecutables().size());

        // Check performed actions on connector agents
        assertEquals(new ArrayList<Object>()
        {{
                add(cz.cesnet.shongo.connector.api.ontology.actions.multipoint.rooms.CreateRoom.class);
                add(cz.cesnet.shongo.connector.api.ontology.actions.multipoint.rooms.DeleteRoom.class);
            }}, mcuAgent.getPerformedActionClasses());
    }

    /**
     * Testing connector agent.
     */
    public class ConnectorAgent extends Agent
    {
        /**
         * List of performed actions on connector.
         */
        private List<AgentAction> performedActions = new ArrayList<AgentAction>();

        /**
         * @return {@link Class}es for {@link #performedActions}
         */
        public List<Class<? extends AgentAction>> getPerformedActionClasses()
        {
            List<Class<? extends AgentAction>> performedActionClasses = new ArrayList<Class<? extends AgentAction>>();
            for (AgentAction agentAction : performedActions) {
                performedActionClasses.add(agentAction.getClass());
            }
            return performedActionClasses;
        }

        /**
         * @param type
         * @return {@link AgentAction} of given {@code type}
         */
        public <T> T getPerformedActionByClass(Class<T> type)
        {
            for (AgentAction agentAction : performedActions) {
                if (type.isAssignableFrom(agentAction.getClass())) {
                    return type.cast(agentAction);
                }
            }
            throw new IllegalStateException("Agent action of type '" + type.getSimpleName() + "' was not found.");
        }

        @Override
        protected void setup()
        {
            addOntology(ConnectorOntology.getInstance());
            super.setup();
        }

        @Override
        public Object handleAgentAction(AgentAction action, AID sender)
                throws CommandException, CommandUnsupportedException
        {
            performedActions.add(action);
            logger.debug("ConnectorAgent '{}' receives action '{}'.", getName(), action.getClass().getSimpleName());
            if (action instanceof CreateRoom) {
                return "roomId";
            }
            return null;
        }
    }
}
