package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;
import cz.cesnet.shongo.api.Room;
import cz.cesnet.shongo.connector.api.ontology.ConnectorOntology;
import cz.cesnet.shongo.connector.api.ontology.actions.multipoint.rooms.CreateRoom;
import cz.cesnet.shongo.connector.api.ontology.actions.multipoint.rooms.ListRooms;
import cz.cesnet.shongo.connector.api.ontology.actions.multipoint.rooms.ModifyRoom;
import cz.cesnet.shongo.controller.AbstractControllerTest;
import cz.cesnet.shongo.fault.CommonFault;
import cz.cesnet.shongo.fault.FaultException;
import cz.cesnet.shongo.fault.jade.CommandFailure;
import cz.cesnet.shongo.jade.Agent;
import cz.cesnet.shongo.jade.command.AgentActionResponderBehaviour;
import jade.content.AgentAction;
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
     * @return {@link cz.cesnet.shongo.controller.api.ResourceControlService} from the {@link #controllerClient}
     */
    public ResourceControlService getResourceControlService()
    {
        return getControllerClient().getService(ResourceControlService.class);
    }

    @Override
    protected void onInit()
    {
        super.onInit();

        getController().addService(new ResourceControlServiceImpl()
        {
            @Override
            protected String getAgentName(String deviceResourceId) throws FaultException
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
            Assert.assertEquals(CommonFault.JADE_COMMAND_NOT_SUPPORTED, exception.getCode());
        }

        try {
            getResourceControlService().createRoom(SECURITY_TOKEN, mcuId, new Room());
            Assert.fail("Exception should be thrown.");
        }
        catch (FaultException exception) {
            Assert.assertEquals(CommonFault.JADE_COMMAND_ERROR, exception.getCode());
        }

        try {
            getResourceControlService().modifyRoom(SECURITY_TOKEN, mcuId, new Room());
            Assert.fail("Exception should be thrown.");
        }
        catch (FaultException exception) {
            Assert.assertEquals(CommonFault.JADE_COMMAND_UNKNOWN, exception.getCode());
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
        public Object handleAgentAction(AgentAction action, AID sender)
                throws CommandException, CommandUnsupportedException
        {
            if (action instanceof ListRooms) {
                throw new CommandUnsupportedException("not supported");
            }
            else if (action instanceof CreateRoom) {
                throw new CommandException("device error");
            }
            else if (action instanceof ModifyRoom) {
                throw new RuntimeException("implementation error");
            }
            return super.handleAgentAction(action, sender);
        }
    }
}
