package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.controller.ControllerFaultSet;
import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;
import cz.cesnet.shongo.api.Room;
import cz.cesnet.shongo.api.jade.Command;
import cz.cesnet.shongo.connector.api.jade.ConnectorOntology;
import cz.cesnet.shongo.connector.api.jade.multipoint.rooms.CreateRoom;
import cz.cesnet.shongo.connector.api.jade.multipoint.rooms.ListRooms;
import cz.cesnet.shongo.connector.api.jade.multipoint.rooms.ModifyRoom;
import cz.cesnet.shongo.controller.AbstractControllerTest;
import cz.cesnet.shongo.controller.api.rpc.ResourceControlService;
import cz.cesnet.shongo.controller.api.rpc.ResourceControlServiceImpl;
import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.fault.FaultException;
import cz.cesnet.shongo.fault.jade.CommandError;
import cz.cesnet.shongo.fault.jade.CommandNotSupported;
import cz.cesnet.shongo.fault.jade.CommandUnknownFailure;
import cz.cesnet.shongo.fault.old.CommonFault;
import cz.cesnet.shongo.fault.jade.CommandFailure;
import cz.cesnet.shongo.jade.Agent;
import jade.core.AID;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link CommandFailure}s,
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

        getController().addRpcService(new ResourceControlServiceImpl()
        {
            @Override
            protected String getAgentName(EntityIdentifier entityId) throws FaultException
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
        String mcuId = "1";
        getController().addJadeAgent("mcu", new ConnectorAgent());
        getController().waitForJadeAgentsToStart();

        try {
            getResourceControlService().listRooms(SECURITY_TOKEN, mcuId);
            Assert.fail("Exception should be thrown.");
        }
        catch (FaultException exception) {
            ControllerFaultSet.DeviceCommandFailedFault deviceCommandFailedFault =
                    exception.getFault(ControllerFaultSet.DeviceCommandFailedFault.class);
            Assert.assertEquals(CommandNotSupported.class, deviceCommandFailedFault.getError().getClass());
        }

        try {
            getResourceControlService().createRoom(SECURITY_TOKEN, mcuId, new Room());
            Assert.fail("Exception should be thrown.");
        }
        catch (FaultException exception) {
            ControllerFaultSet.DeviceCommandFailedFault deviceCommandFailedFault =
                    exception.getFault(ControllerFaultSet.DeviceCommandFailedFault.class);
            Assert.assertEquals(CommandError.class, deviceCommandFailedFault.getError().getClass());
        }

        try {
            getResourceControlService().modifyRoom(SECURITY_TOKEN, mcuId, new Room());
            Assert.fail("Exception should be thrown.");
        }
        catch (FaultException exception) {
            ControllerFaultSet.DeviceCommandFailedFault deviceCommandFailedFault =
                    exception.getFault(ControllerFaultSet.DeviceCommandFailedFault.class);
            Assert.assertEquals(CommandUnknownFailure.class, deviceCommandFailedFault.getError().getClass());
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
