package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.api.Room;
import cz.cesnet.shongo.api.jade.Command;
import cz.cesnet.shongo.connector.api.jade.ConnectorOntology;
import cz.cesnet.shongo.connector.api.jade.multipoint.CreateRoom;
import cz.cesnet.shongo.connector.api.jade.multipoint.ModifyRoom;
import cz.cesnet.shongo.controller.AbstractControllerTest;
import cz.cesnet.shongo.controller.api.rpc.RecordingsCache;
import cz.cesnet.shongo.controller.api.rpc.ResourceControlService;
import cz.cesnet.shongo.controller.api.rpc.ResourceControlServiceImpl;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.jade.Agent;
import jade.core.AID;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests for serializing API classes for JADE.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class JadeSerializationTest extends AbstractControllerTest
{
    private static Logger logger = LoggerFactory.getLogger(JadeSerializationTest.class);

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
            public Room getRoom(SecurityToken token, String deviceResourceId, String roomId)
            {
                Assert.assertEquals("1", roomId);
                Room room = new Room();
                room.setId("1");
                room.setLicenseCount(5);
                room.addAlias(AliasType.ROOM_NAME, "room");
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
    public void testCreateRoom() throws Exception
    {
        ConnectorAgent mcuAgent = getController().addJadeAgent("mcu", new ConnectorAgent());
        getController().waitForJadeAgentsToStart();

        DeviceResource mcu = new DeviceResource();
        mcu.setName("mcu");
        mcu.addTechnology(Technology.H323);
        mcu.addCapability(new RoomProviderCapability(10));
        mcu.setAllocatable(true);
        mcu.setMode(new ManagedMode(mcuAgent.getName()));
        String mcuId = createResource(SECURITY_TOKEN, mcu);

        Room room = new Room();
        room.addAlias(new Alias(AliasType.ROOM_NAME, "test"));
        getResourceControlService().createRoom(SECURITY_TOKEN, mcuId, room);
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
        String mcuId = createResource(SECURITY_TOKEN, mcu);

        Room room = getResourceControlService().getRoom(SECURITY_TOKEN, mcuId, "1");
        room.addAlias(new Alias(AliasType.H323_URI, "test"));
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
            super.setup();
        }

        @Override
        public Object handleCommand(Command command, AID sender) throws CommandException, CommandUnsupportedException
        {
            if (command instanceof CreateRoom) {
                CreateRoom createRoom = (CreateRoom) command;
                Room room = createRoom.getRoom();
                Assert.assertTrue(room.getName() != null);
                Assert.assertFalse(room.getDescription() != null);
                Assert.assertEquals(1, room.getAliases().size());
                Assert.assertEquals("test", room.getAliases().get(0).getValue());
            }
            else if (command instanceof ModifyRoom) {
                ModifyRoom modifyRoom = (ModifyRoom) command;
                Room room = modifyRoom.getRoom();
                Assert.assertEquals("1", room.getId());
                Assert.assertEquals("room", room.getName());
                Assert.assertTrue(room.getName() != null);
                Assert.assertFalse(room.getDescription() != null);
                Assert.assertEquals(3, room.getAliases().size());
                Assert.assertEquals("test", room.getAliases().get(2).getValue());
            }
            else {
                throw new TodoImplementException(command.getClass());
            }
            return null;
        }
    }
}
