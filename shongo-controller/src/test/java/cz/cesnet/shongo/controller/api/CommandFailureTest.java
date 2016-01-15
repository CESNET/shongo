package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.JadeReportSet;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.api.Room;
import cz.cesnet.shongo.api.jade.Command;
import cz.cesnet.shongo.connector.api.jade.ConnectorOntology;
import cz.cesnet.shongo.connector.api.jade.multipoint.CreateRoom;
import cz.cesnet.shongo.connector.api.jade.multipoint.ListRooms;
import cz.cesnet.shongo.connector.api.jade.multipoint.ModifyRoom;
import cz.cesnet.shongo.controller.AbstractControllerTest;
import cz.cesnet.shongo.controller.ControllerReportSet;
import cz.cesnet.shongo.controller.api.rpc.RecordingsCache;
import cz.cesnet.shongo.controller.api.rpc.ResourceControlService;
import cz.cesnet.shongo.controller.api.rpc.ResourceControlServiceImpl;
import cz.cesnet.shongo.jade.Agent;
import jade.core.AID;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link cz.cesnet.shongo.JadeReport}s,
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class CommandFailureTest extends AbstractControllerTest
{
    /**
     * @return {@link cz.cesnet.shongo.controller.api.rpc.ResourceControlService} from the {@link #controllerClient}
     */
    public ResourceControlService getResourceControlService()
    {
        return getControllerClient().getService(ResourceControlService.class);
    }

    @Override
    protected void onInit()
    {
        super.onInit();

        getController().addRpcService(new ResourceControlServiceImpl(new RecordingsCache())
        {
            @Override
            protected String getAgentName(cz.cesnet.shongo.controller.booking.resource.DeviceResource deviceResource)
            {
                return "mcu";
            }
        });
    }

    @Override
    protected void onStart()
    {
        super.onStart();

        getController().startJade();
    }

    @Test
    public void test() throws Exception
    {
        DeviceResource deviceResource = new DeviceResource();
        deviceResource.setName("mcu");
        deviceResource.addTechnology(Technology.H323);
        String mcuId = createResource(deviceResource);

        getController().addJadeAgent("mcu", new ConnectorAgent());
        getController().waitForJadeAgentsToStart();

        try {
            getResourceControlService().listRooms(SECURITY_TOKEN_ROOT, mcuId);
            Assert.fail("Exception should be thrown.");
        }
        catch (ControllerReportSet.DeviceCommandFailedException exception) {
            Assert.assertEquals(JadeReportSet.CommandNotSupportedReport.class, exception.getJadeReport().getClass());
        }

        try {
            getResourceControlService().createRoom(SECURITY_TOKEN_ROOT, mcuId, new Room());
            Assert.fail("Exception should be thrown.");
        }
        catch (ControllerReportSet.DeviceCommandFailedException exception) {
            Assert.assertEquals(JadeReportSet.CommandFailedReport.class, exception.getJadeReport().getClass());
        }

        try {
            getResourceControlService().modifyRoom(SECURITY_TOKEN_ROOT, mcuId, new Room());
            Assert.fail("Exception should be thrown.");
        }
        catch (CommonReportSet.UnknownErrorException exception) {
            Assert.assertEquals(CommonReportSet.UnknownErrorException.class, exception.getClass());
        }
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
        public Object handleCommand(Command command, AID sender) throws CommandException, CommandUnsupportedException
        {
            if (command instanceof ListRooms) {
                throw new CommandUnsupportedException("not supported");
            }
            else if (command instanceof CreateRoom) {
                throw new CommandException("device error");
            }
            else if (command instanceof ModifyRoom) {
                throw new RuntimeException("implementation error");
            }
            return super.handleCommand(command, sender);
        }
    }
}
