package cz.cesnet.shongo.controller.executor;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.ontology.ConnectorOntology;
import cz.cesnet.shongo.controller.*;
import cz.cesnet.shongo.controller.Controller;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.jade.Agent;
import cz.cesnet.shongo.jade.command.AgentActionResponderBehaviour;
import jade.content.AgentAction;
import jade.core.AID;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link cz.cesnet.shongo.controller.executor.ExecutionPlan}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ExecutableReportTest extends AbstractControllerTest
{
    /**
     * @see cz.cesnet.shongo.controller.Executor
     */
    private Executor executor;

    /**
     * Constructor.
     */
    public ExecutableReportTest()
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
        String agentName = "mcu";
        ConnectorAgent mcuAgent = getController().addJadeAgent(agentName, new ConnectorAgent());

        DateTime dateTime = DateTime.parse("2012-01-01T12:00");
        Period duration = Period.parse("PT2M");

        DeviceResource mcu = new DeviceResource();
        mcu.setName("mcu");
        mcu.addTechnology(Technology.H323);
        mcu.addCapability(new RoomProviderCapability(10));
        mcu.setAllocatable(true);
        mcu.setMode(new ManagedMode(agentName));
        String mcuId = getResourceService().createResource(SECURITY_TOKEN, mcu);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot(dateTime, duration);
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        RoomSpecification roomSpecification = new RoomSpecification();
        roomSpecification.addTechnology(Technology.H323);
        roomSpecification.setParticipantCount(5);
        reservationRequest.setSpecification(roomSpecification);

        // Allocate reservation request
        Reservation reservation = allocateAndCheck(reservationRequest);

        // Start virtual room
        ExecutionResult result = executor.execute(dateTime);

        reservation = getReservationService().getReservation(SECURITY_TOKEN, reservation.getId());
        Assert.assertTrue(reservation.getExecutable().getStateReport().contains("test test"));
    }

    /**
     * Testing connector agent.
     */
    public class ConnectorAgent extends Agent
    {
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
            throw new CommandException("test test");
        }
    }
}
