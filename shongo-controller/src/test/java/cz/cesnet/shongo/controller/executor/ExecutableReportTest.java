package cz.cesnet.shongo.controller.executor;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.api.jade.Command;
import cz.cesnet.shongo.connector.api.jade.ConnectorOntology;
import cz.cesnet.shongo.controller.*;
import cz.cesnet.shongo.controller.Controller;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.jade.Agent;
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
     * @see Executor
     */
    private Executor executor;

    /**
     * Constructor.
     */
    public ExecutableReportTest()
    {
        // Executor configuration
        System.setProperty(ControllerConfiguration.EXECUTOR_EXECUTABLE_START, "PT0S");
        System.setProperty(ControllerConfiguration.EXECUTOR_EXECUTABLE_END, "PT0S");
        System.setProperty(ControllerConfiguration.EXECUTOR_STARTING_DURATION_ROOM, "PT0S");
    }

    @Override
    protected void onInit()
    {
        super.onInit();

        Controller controller = getController();
        executor = new Executor(controller.getNotificationManager());
        executor.setEntityManagerFactory(getEntityManagerFactory());
        executor.setControllerAgent(controller.getAgent());
        executor.setAuthorization(controller.getAuthorization());
        executor.init(controller.getConfiguration());
    }

    @Override
    protected void onStart()
    {
        super.onStart();

        getController().startJade();
    }

    /**
     * Allocate {@link cz.cesnet.shongo.controller.booking.room.RoomEndpoint} and execute it.
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
        String mcuId = createResource(mcu);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot(dateTime, duration);
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new RoomSpecification(5, Technology.H323));

        // Allocate reservation request
        Reservation reservation = allocateAndCheck(reservationRequest);

        // Start virtual room
        ExecutionResult result = executor.execute(dateTime);

        reservation = getReservationService().getReservation(SECURITY_TOKEN_ROOT, reservation.getId());
        Assert.assertTrue(reservation.getExecutable().getStateReport().toString().contains("test test"));
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
        public Object handleCommand(Command command, AID sender)
                throws CommandException, CommandUnsupportedException
        {
            throw new CommandException("test test");
        }
    }
}
