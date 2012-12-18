package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;
import cz.cesnet.shongo.api.Room;
import cz.cesnet.shongo.connector.api.ontology.ConnectorOntology;
import cz.cesnet.shongo.connector.api.ontology.actions.multipoint.rooms.ModifyRoom;
import cz.cesnet.shongo.controller.AbstractControllerTest;
import cz.cesnet.shongo.fault.FaultException;
import cz.cesnet.shongo.jade.Agent;
import cz.cesnet.shongo.jade.UnknownAgentActionException;
import cz.cesnet.shongo.jade.command.AgentActionResponderBehaviour;
import jade.content.AgentAction;
import jade.core.AID;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;

import static junit.framework.Assert.assertEquals;

/**
 * Tests for serializing API classes for JADE.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class JadeTest extends AbstractControllerTest
{
    private static Logger logger = LoggerFactory.getLogger(JadeTest.class);

    /**
     * @return {@link ResourceControlService} from the {@link #controllerClient}
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
            public Room getRoom(SecurityToken token, String deviceResourceId, String roomId)
                    throws FaultException
            {
                assertEquals("1", roomId);
                Room room = new Room();
                room.setId("1");
                room.setCode("Fixed Testing Room (TODO: Remove it)");
                room.setName("room description");
                room.setLicenseCount(5);
                room.addAlias(new Alias(AliasType.H323_E164, "9501"));
                return room;
            }
        });
    }

    @Override
    protected void onStart()
    {
        super.onStart();

        getController().startJade();
    }

    /**
     * Test serialization of {@link Room} in {@link ResourceControlService#modifyRoom(SecurityToken, String, Room)}.
     *
     * @throws Exception
     */
    @Test
    public void testModifyRoom() throws Exception
    {
        ConnectorAgent mcuAgent = getController().addJadeAgent("mcu", new ConnectorAgent());
        getController().waitForJadeAgentsToStart();

        DeviceResource mcu = new DeviceResource();
        mcu.setName("mcu");
        mcu.addTechnology(Technology.H323);
        mcu.addCapability(new RoomProviderCapability(10));
        mcu.setAllocatable(true);
        mcu.setMode(new ManagedMode(mcuAgent.getName()));
        String mcuId = getResourceService().createResource(SECURITY_TOKEN, mcu);

        Room room = getResourceControlService().getRoom(SECURITY_TOKEN, mcuId, "1");
        room.setCode("room");
        room.setName(null);
        room.setOption(Room.Option.PIN, "1234");
        getResourceControlService().modifyRoom(SECURITY_TOKEN, mcuId, room);
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
            addBehaviour(new AgentActionResponderBehaviour(this));

            super.setup();
        }

        @Override
        public Object handleAgentAction(AgentAction action, AID sender)
                throws UnknownAgentActionException, CommandException, CommandUnsupportedException
        {
            ModifyRoom modifyRoom = (ModifyRoom) action;
            Room room = modifyRoom.getRoom();
            assertEquals("1", room.getId());
            assertEquals("room", room.getCode());
            assertEquals(new HashSet<Room.Option>()
            {{
                    add(Room.Option.PIN);
                }}, room.getPropertyItemsMarkedAsNew(Room.OPTIONS));
            assertEquals("1234", room.getOption(Room.Option.PIN));
//            assertEquals(new HashSet<Room.Option>()
//            {{
//                    add(Room.Option.DESCRIPTION);
//                }}, room.getPropertyItemsMarkedAsDeleted(Room.OPTIONS));
            return null;
        }
    }
}
