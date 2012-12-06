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
import cz.cesnet.shongo.controller.executor.ResourceRoomEndpoint;
import cz.cesnet.shongo.controller.executor.ExecutorThread;
import cz.cesnet.shongo.jade.Agent;
import cz.cesnet.shongo.jade.UnknownAgentActionException;
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
        System.setProperty(Configuration.EXECUTOR_COMPARTMENT_WAITING_ROOM, "PT0S");
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
        aliasProvider.addCapability(new AliasProviderCapability(AliasType.H323_E164, "9500872[dd]"));
        String aliasProviderIdentifier = getResourceService().createResource(SECURITY_TOKEN, aliasProvider);

        DeviceResource terminal = new DeviceResource();
        terminal.setName("terminal");
        terminal.addTechnology(Technology.H323);
        terminal.addCapability(new TerminalCapability());
        terminal.setAllocatable(true);
        terminal.setMode(new ManagedMode(terminalAgent.getName()));
        String terminalIdentifier = getResourceService().createResource(SECURITY_TOKEN, terminal);

        DeviceResource mcu = new DeviceResource();
        mcu.setName("mcu");
        mcu.addTechnology(Technology.H323);
        mcu.addCapability(new RoomProviderCapability(10));
        mcu.setAllocatable(true);
        mcu.setMode(new ManagedMode(mcuAgent.getName()));
        String mcuIdentifier = getResourceService().createResource(SECURITY_TOKEN, mcu);

        // Create compartment reservation
        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot(dateTime, duration);
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        CompartmentSpecification compartmentSpecification = new CompartmentSpecification();
        compartmentSpecification.addSpecification(new ExistingEndpointSpecification(terminalIdentifier));
        compartmentSpecification.addSpecification(new ExternalEndpointSetSpecification(Technology.H323, 1));
        reservationRequest.setSpecification(compartmentSpecification);
        allocateAndCheck(reservationRequest);

        // Execute compartment
        List<ExecutorThread> executorThreads = executor.execute(dateTime);
        assertEquals("One thread should be executed.", 1, executorThreads.size());
        assertEquals("Thread should execute compartment.", cz.cesnet.shongo.controller.executor.Compartment.class,
                executorThreads.get(0).getExecutable(getEntityManager()).getClass());

        // Wait for executor threads to end
        executor.waitForThreads();

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
     * Allocate {@link cz.cesnet.shongo.controller.executor.RoomEndpoint} and execute it.
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
        mcu.setAddress("127.0.0.1");
        mcu.addTechnology(Technology.H323);
        mcu.addCapability(new RoomProviderCapability(10));
        mcu.setAllocatable(true);
        mcu.setMode(new ManagedMode(mcuAgent.getName()));
        String mcuIdentifier = getResourceService().createResource(SECURITY_TOKEN, mcu);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot(dateTime, duration);
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        RoomSpecification roomSpecification = new RoomSpecification();
        roomSpecification.addTechnology(Technology.H323);
        roomSpecification.setParticipantCount(5);
        reservationRequest.setSpecification(roomSpecification);

        // Allocate reservation request
        allocateAndCheck(reservationRequest);

        // Execute virtual room
        List<ExecutorThread> executorThreads = executor.execute(dateTime);
        assertEquals("One thread should be executed.", 1, executorThreads.size());
        assertEquals("Thread should execute virtual room.",
                ResourceRoomEndpoint.class,
                executorThreads.get(0).getExecutable(getEntityManager()).getClass());

        // Wait for executor threads to end
        executor.waitForThreads();

        // Check performed actions on connector agents
        assertEquals(new ArrayList<Object>()
        {{
                add(cz.cesnet.shongo.connector.api.ontology.actions.multipoint.rooms.CreateRoom.class);
                add(cz.cesnet.shongo.connector.api.ontology.actions.multipoint.rooms.DeleteRoom.class);
            }}, mcuAgent.getPerformedActionClasses());
    }

    /**
     * Allocate {@link cz.cesnet.shongo.controller.executor.RoomEndpoint} and execute it.
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
        mcu.setAddress("127.0.0.1");
        mcu.addTechnology(Technology.H323);
        mcu.addCapability(new RoomProviderCapability(10));
        mcu.setAllocatable(true);
        mcu.setMode(new ManagedMode(mcuAgent.getName()));
        String mcuIdentifier = getResourceService().createResource(SECURITY_TOKEN, mcu);

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

        // Execute virtual room
        List<ExecutorThread> executorThreads = executor.execute(dateTime);
        assertEquals("One thread should be executed.", 1, executorThreads.size());
        assertEquals("Thread should execute virtual room.",
                ResourceRoomEndpoint.class,
                executorThreads.get(0).getExecutable(getEntityManager()).getClass());

        // Wait for executor threads to end
        executor.waitForThreads();

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
     * Allocate {@link cz.cesnet.shongo.controller.executor.RoomEndpoint}, provide it to {@link cz.cesnet.shongo.controller.executor.Compartment} and execute
     * both of them separately (first {@link cz.cesnet.shongo.controller.executor.RoomEndpoint} and then
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
        mcu.setAddress("127.0.0.1");
        mcu.addTechnology(Technology.H323);
        mcu.addCapability(new RoomProviderCapability(10));
        mcu.setAllocatable(true);
        mcu.setMode(new ManagedMode(mcuAgent.getName()));
        String mcuIdentifier = getResourceService().createResource(SECURITY_TOKEN, mcu);

        // Create virtual room reservation
        ReservationRequest roomReservationRequest = new ReservationRequest();
        roomReservationRequest.setSlot(dateTime, duration);
        roomReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        RoomSpecification roomSpecification = new RoomSpecification();
        roomSpecification.addTechnology(Technology.H323);
        roomSpecification.setParticipantCount(10);
        roomReservationRequest.setSpecification(roomSpecification);
        String roomReservationIdentifier = allocateAndCheck(roomReservationRequest).getIdentifier();

        // Execute virtual room
        List<ExecutorThread> executorThreads = executor.execute(dateTime);
        assertEquals("One thread should be executed.", 1, executorThreads.size());
        assertEquals("Thread should execute virtual room.",
                ResourceRoomEndpoint.class,
                executorThreads.get(0).getExecutable(getEntityManager()).getClass());

        // Wait for executor threads to end
        executor.waitForThreads();

        // Create compartment reservation
        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot(dateTime, duration);
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        CompartmentSpecification compartmentSpecification = new CompartmentSpecification();
        compartmentSpecification.addSpecification(new ExternalEndpointSetSpecification(Technology.H323, 10));
        reservationRequest.setSpecification(compartmentSpecification);
        reservationRequest.addProvidedReservationIdentifier(roomReservationIdentifier);
        allocateAndCheck(reservationRequest);

        // Execute compartment
        executorThreads = executor.execute(dateTime);
        assertEquals("One thread should be executed.", 1, executorThreads.size());
        assertEquals("Thread should execute compartment.", cz.cesnet.shongo.controller.executor.Compartment.class,
                executorThreads.get(0).getExecutable(getEntityManager()).getClass());

        // Wait for executor threads to end
        executor.waitForThreads();

        // Check performed actions on connector agents
        assertEquals(new ArrayList<Object>()
        {{
                add(cz.cesnet.shongo.connector.api.ontology.actions.multipoint.rooms.CreateRoom.class);
                add(cz.cesnet.shongo.connector.api.ontology.actions.multipoint.rooms.DeleteRoom.class);
            }}, mcuAgent.getPerformedActionClasses());
    }

    /**
     * Allocate {@link cz.cesnet.shongo.controller.executor.RoomEndpoint}, provide it to {@link cz.cesnet.shongo.controller.executor.Compartment} and execute
     * both at once ({@link cz.cesnet.shongo.controller.executor.RoomEndpoint} through the {@link cz.cesnet.shongo.controller.executor.Compartment}).
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
        mcu.setAddress("127.0.0.1");
        mcu.addTechnology(Technology.H323);
        mcu.addCapability(new RoomProviderCapability(10));
        mcu.setAllocatable(true);
        mcu.setMode(new ManagedMode(mcuAgent.getName()));
        String mcuIdentifier = getResourceService().createResource(SECURITY_TOKEN, mcu);

        // Create virtual room reservation
        ReservationRequest roomReservationRequest = new ReservationRequest();
        roomReservationRequest.setSlot(dateTime, duration);
        roomReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        RoomSpecification roomSpecification = new RoomSpecification();
        roomSpecification.addTechnology(Technology.H323);
        roomSpecification.setParticipantCount(10);
        roomReservationRequest.setSpecification(roomSpecification);
        String roomReservationIdentifier = allocateAndCheck(roomReservationRequest).getIdentifier();

        // Create compartment reservation
        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot(dateTime, duration);
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        CompartmentSpecification compartmentSpecification = new CompartmentSpecification();
        compartmentSpecification.addSpecification(new ExternalEndpointSetSpecification(Technology.H323, 10));
        reservationRequest.setSpecification(compartmentSpecification);
        reservationRequest.addProvidedReservationIdentifier(roomReservationIdentifier);
        allocateAndCheck(reservationRequest);

        // Execute compartment
        List<ExecutorThread> executorThreads = executor.execute(dateTime);
        assertEquals("One thread should be executed.", 1, executorThreads.size());
        assertEquals("Thread should execute compartment.", cz.cesnet.shongo.controller.executor.Compartment.class,
                executorThreads.get(0).getExecutable(getEntityManager()).getClass());

        // Wait for executor threads to end
        executor.waitForThreads();

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
            addBehaviour(new AgentActionResponderBehaviour(this));

            super.setup();
        }

        @Override
        public Object handleAgentAction(AgentAction action, AID sender)
                throws UnknownAgentActionException, CommandException, CommandUnsupportedException
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
