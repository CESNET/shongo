package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.jade.ActionRequestResponderBehaviour;
import cz.cesnet.shongo.jade.Agent;
import cz.cesnet.shongo.jade.UnknownActionException;
import cz.cesnet.shongo.jade.ontology.actions.multipoint.rooms.CreateRoom;
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
        System.setProperty(Configuration.EXECUTOR_COMPARTMENT_WAITING_VIRTUAL_ROOM, "PT0S");
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
     * Allocate {@link Compartment} for one existing and one external endpoint and execute it.
     *
     * @throws Exception
     */
    @Test
    public void test() throws Exception
    {
        ConnectorAgent terminalAgent = getController().addJadeAgent("terminal", new ConnectorAgent());
        ConnectorAgent mcuAgent = getController().addJadeAgent("mcu", new ConnectorAgent());

        DateTime dateTime = DateTime.parse("2012-01-01T12:00");
        Period duration = Period.parse("PT2M");

        DeviceResource terminal = new DeviceResource();
        terminal.setName("terminal");
        terminal.addTechnology(Technology.H323);
        terminal.addCapability(new TerminalCapability());
        terminal.setAllocatable(true);
        terminal.setMode(new ManagedMode(terminalAgent.getName()));
        String terminalIdentifier = getResourceService().createResource(SECURITY_TOKEN, terminal);

        DeviceResource mcu = new DeviceResource();
        mcu.setName("mcu");
        mcu.setAddress("127.0.0.1");
        mcu.addTechnology(Technology.H323);
        mcu.addCapability(new VirtualRoomsCapability(10));
        mcu.setAllocatable(true);
        mcu.setMode(new ManagedMode(mcuAgent.getName()));
        String mcuIdentifier = getResourceService().createResource(SECURITY_TOKEN, mcu);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setType(ReservationRequestType.NORMAL);
        reservationRequest.setSlot(dateTime, duration);
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        CompartmentSpecification compartmentSpecification = new CompartmentSpecification();
        compartmentSpecification.addSpecification(new ExistingEndpointSpecification(terminalIdentifier));
        compartmentSpecification.addSpecification(new ExternalEndpointSetSpecification(Technology.H323, 1));
        reservationRequest.setSpecification(compartmentSpecification);

        // Allocate reservation request
        allocateAndCheck(reservationRequest);

        // Execute compartment
        int count = executor.execute(dateTime);
        assertEquals("One compartment should be executed.", 1, count);

        // Wait for executor threads to end
        executor.waitForThreads();

        // Check performed actions on connector agents
        assertEquals(new ArrayList()
        {{
                add(cz.cesnet.shongo.jade.ontology.actions.endpoint.Dial.class);
                add(cz.cesnet.shongo.jade.ontology.actions.endpoint.HangUpAll.class);
            }}, terminalAgent.getPerformedActions());
        assertEquals(new ArrayList()
        {{
                add(cz.cesnet.shongo.jade.ontology.actions.multipoint.rooms.CreateRoom.class);
                add(cz.cesnet.shongo.jade.ontology.actions.multipoint.rooms.DeleteRoom.class);
            }}, mcuAgent.getPerformedActions());
    }

    /**
     * Testing connector agent.
     */
    public class ConnectorAgent extends Agent
    {
        /**
         * List of performed actions on connector.
         */
        private List<Class<? extends AgentAction>> performedActions = new ArrayList<Class<? extends AgentAction>>();

        /**
         * @return {@link #performedActions}
         */
        public List<Class<? extends AgentAction>> getPerformedActions()
        {
            return performedActions;
        }

        @Override
        protected void setup()
        {
            addBehaviour(new ActionRequestResponderBehaviour(this));

            super.setup();
        }

        @Override
        public Object handleAgentAction(AgentAction action, AID sender)
                throws UnknownActionException, CommandException, CommandUnsupportedException
        {
            performedActions.add(action.getClass());
            logger.debug("ConnectorAgent '{}' receives action '{}'.", getName(), action.getClass().getSimpleName());
            if (action instanceof CreateRoom) {
                return "roomId";
            }
            return null;
        }
    }

}
